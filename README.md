# Jackpot Service

Backend service (Sporty Group home assignment) that receives bets, contributes each bet to a
matching jackpot pool, evaluates each bet for a jackpot reward, and exposes the outcome over REST.
Built with **Java 21 / Spring Boot 4.1**, Spring Data JPA + H2 (in-memory), and Spring Kafka.

- Task spec: [assigment.pdf](assigment.pdf)
- Design decisions & trade-offs: the [Assumptions & decisions (A1–A19)](#assumptions--decisions-a1a19)
  section below — code comments cite these tags (e.g. `A5`).

---

## Quick start

The default run boots an **in-JVM Kafka broker** (the assignment's "mock" in spirit — but a real
broker, so the whole publish → consume → contribute → evaluate flow actually runs) plus **in-memory
H2**. No external infrastructure, one command:

```bash
# Option A — Maven
./mvnw spring-boot:run

# Option B — runnable jar
./mvnw clean package
java -jar target/jackpot-0.0.1-SNAPSHOT.jar

# Option C — Docker (layered image)
docker build -t jackpot-service .
docker run -p 8080:8080 jackpot-service
```

The service listens on `http://localhost:8080`. On startup it loads two jackpots from
[`src/main/resources/jackpots.json`](src/main/resources/jackpots.json):

| Jackpot | Initial pool | Contribution | Reward |
|---------|--------------|--------------|--------|
| `jackpot-1` | 1000 | FIXED 10% | FIXED 1% |
| `jackpot-2` | 5000 | VARIABLE 20%→5% over pool 100 000 | VARIABLE 0.1%→100% over pool 1 000 000 |

---

## Using the API

All payloads are JSON under `/api/v1`.

### 1. Publish a bet (FR1)

```bash
curl -i -X POST localhost:8080/api/v1/bets \
  -H 'Content-Type: application/json' \
  -d '{"betId":"b-1001","userId":"u-42","jackpotId":"jackpot-1","betAmount":50.00}'
```

```
202 Accepted   { "betId": "b-1001", "status": "PUBLISHED" }
422            { "error": "JACKPOT_NOT_FOUND", ... }   # no such jackpot — nothing published
400            { "error": "VALIDATION_ERROR", "errorDetails": [...] }
```

The bet is published to the `jackpot-bets` topic, consumed asynchronously, contributed to the pool,
and evaluated for a reward — all in the consumer (see *Assumptions & decisions* below).

### 2. Read a bet's reward outcome (FR5)

```bash
curl -i localhost:8080/api/v1/bets/b-1001/reward
```

```
200 OK   { "betId":"b-1001", "userId":"u-42", "jackpotId":"jackpot-1", "won":true,  "rewardAmount":1250.00, "createdAt":"..." }
200 OK   { "betId":"b-1001", "userId":"u-42", "jackpotId":"jackpot-1", "won":false }
404      { "error":"BET_NOT_FOUND", ... }   # unknown bet, OR not consumed yet (poll & retry)
```

This endpoint is **read-only**: the win was already decided in the consumer, so it reports the
stored outcome. A `404` means either the bet is unknown or it has not been processed yet (the async
window between publish and consume) — a client that just published can poll.

### H2 console

Enabled at `http://localhost:8080/h2-console` (JDBC URL `jdbc:h2:mem:jackpot`, user `sa`, no
password) to inspect `jackpot`, `jackpot_contribution`, `jackpot_reward`.

---

## Configuration

Override via env vars or `-D` flags (Spring relaxed binding).

| Property | Default | Meaning |
|----------|---------|---------|
| `jackpot.kafka.mode` | `embedded` | `embedded` (in-JVM broker) / `external` (real broker) / `log-only` (mock, does not drive the consumer) |
| `spring.kafka.bootstrap-servers` | `localhost:9092` | Broker address for `external` mode |
| `jackpot.contention.max-retries` | `3` | Optimistic-lock retry budget before a bet is routed to `jackpot-bets-cancelled` |
| `jackpot.contention.backoff-ms` | `50` | Backoff between contention retries |
| `jackpot.reward.rng-seed` | _(none)_ | Fix the reward RNG seed for deterministic outcomes (test aid) |
| `jackpot.initializer.file` | `classpath:jackpots.json` | Jackpot definitions loaded at startup |

### Run modes

- **`embedded`** (default) — in-JVM `EmbeddedKafkaKraftBroker`; the full flow runs with no external
  infra. This is the assignment-sanctioned "mock", implemented as a real broker so it exercises the
  consumer.
- **`external`** — real broker. Example with Docker Compose (brings up Kafka + the service):
  ```bash
  docker compose up --build
  ```
- **`log-only`** — the most literal reading of the assignment's "mock the producer, just log the
  payload". Note it does **not** drive the consumer, so contribution/reward do not run:
  ```bash
  java -jar target/jackpot-0.0.1-SNAPSHOT.jar --jackpot.kafka.mode=log-only
  ```

> Note: `spring-kafka-test` (which provides the embedded broker) is on the runtime classpath so the
> default `embedded` mode works out of the box. A true production build would move it back to
> `test` scope and run in `external` mode (see [A17](#assumptions--decisions-a1a19)).

---

## Testing

```bash
./mvnw test
```

Covers: strategy formula unit tests; `JackpotService` validation/idempotency; `BetService`,
`ContributionService` and `RewardService` (win → reward + pool reset, loss, read-outcome);
`BetConsumer` branches (duplicate-skip, unknown jackpot, contention → cancelled, unexpected error →
cancelled, poison/invalid messages); and an end-to-end integration test (`@SpringBootTest` over the
embedded broker) that publishes a bet and reads back its evaluated outcome.

---

## Assumptions & decisions (A1–A19)

The assignment fixes the *what*, not every *how*. These are the load-bearing decisions; the code
comments cite them by tag (e.g. `A5`). This list is the canonical reference for those tags.

- **A1 — Jackpot creation & initialization.** Creation is a service-level API
  (`JackpotService.create(command)`): the `id` is an optional string (a GUID is generated when
  omitted); a duplicate id logs a WARN and keeps the existing jackpot (idempotent), returning a DTO.
  At startup `JackpotInitializer` reads a JSON file and calls it per entry. A `POST /api/v1/jackpots`
  would be a thin wrapper over the same API.
- **A2 — Evaluation in the consumer; read-only API.** Each bet is evaluated exactly once, in the
  consumer, right after its contribution. `GET …/reward` is read-only — it reports whether the
  already-processed bet won (derived from whether a `JackpotReward` row exists). A unique `bet_id` on
  `jackpot_reward` enforces at most one reward per bet. This prevents double payouts and chance
  "farming" via repeated calls, and reads the assignment's "endpoint to evaluate" as a query.
- **A3 — Unknown jackpot handling.** Pre-publish (`BetService`): the bet is rejected synchronously
  (`422 JACKPOT_NOT_FOUND`), nothing is published. At consume time (the async path with no caller to
  answer): the bet is routed to the `jackpot-bets-cancelled` topic with an error code.
- **A4 — Unknown or not-yet-processed bet on reward query → 404.** A `404` does not distinguish "bet
  never existed" from "accepted but not consumed yet" (the async window). A client that just
  published can poll and retry.
- **A5 — Idempotency by `bet_id`.** Kafka is at-least-once, so a repeated `bet_id` must take effect
  once. Dedup rests on the unique `bet_id` constraint (DB-agnostic, no vendor upsert). Processing is
  one transaction; a redelivered bet violates the constraint on the contribution insert, rolls the
  whole attempt back, and is caught at the transaction boundary as a skip + ack.
- **A6 — Monetary precision & rounding.** Money is `BigDecimal` `DECIMAL(19,4)`; percentages/chances
  are JSON numbers read as `BigDecimal` (no `double` hop). Every computed money amount is rounded to
  4 decimals with `RoundingMode.HALF_EVEN` (banker's rounding), once, on the final amount;
  intermediate percentage math keeps full precision.
- **A7 — Stateless & multi-instance-ready; single-instance H2 runtime.** The service keeps no
  in-memory state and is designed to run as multiple instances against one shared DB; for ease of
  running the assignment it is deployed as a single instance over in-memory H2. The safety for many
  instances is A8.
- **A8 — Concurrency control on the shared pool.** Keying `jackpot-bets` by `jackpotId` +
  single-owner-per-partition serializes a jackpot's bets, so steady-state contention is structurally
  zero. The backstop for rare transient conflicts is optimistic locking (`@Version` on the pool) +
  the unique `bet_id`.
- **A9 — Bounded contention retries → cancelled.** On an optimistic-lock conflict the unit of work
  is retried up to `jackpot.contention.max-retries` (with backoff); if still uncommitted, the bet is
  routed to `jackpot-bets-cancelled` (`CONTENTION_RETRY_EXHAUSTED`) rather than looping forever.
  Unexpected errors are parked the same way (`PROCESSING_ERROR`) so the partition never blocks.
- **A10 — Schema via Hibernate auto-DDL.** The in-memory DB is ephemeral and rebuilt each startup, so
  the schema is created by Hibernate `create-drop`; no migration tooling. Versioned migrations
  (e.g. Liquibase) would be the production swap against a durable RDBMS.
- **A11 — Layered Docker image.** Packaged as a layered Spring Boot image (dependencies vs.
  application as separate layers) for cache-friendly rebuilds.
- **A12 — Both run methods documented here.** The README documents Docker and plain `java -jar`, each
  with its config knobs; the default build runs with no external infra (embedded Kafka + H2).
- **A13 — Win evaluated strictly after the bet is applied to the pool.** Two pool values are named
  throughout: `poolBefore` (on arrival) and `poolAfter = poolBefore + contribution`. The variable
  *contribution rate* uses `poolBefore`; the *win-check* and the *reward amount* use `poolAfter`.
- **A14 — Contribution model + parameters.** `contribution = pct × stake`. *Fixed* — constant
  `pct = initialPct`. *Variable* — `pct` falls linearly with the pool from `initialPct` down to a
  floor `minPct`, reaching the floor at `poolThreshold`. Percentages are fractions in `(0, 1]`;
  invariants validated in `JackpotService.create`. The linear shape/floor are a design choice.
- **A15 — Reward model + parameters.** Win chance. *Fixed* — constant `chance = initialChance`.
  *Variable* — `chance` grows linearly with the pool from `initialChance` up to 100%, hitting 100% at
  `poolLimit`. A pure function of `poolAfter` (symmetric to A14); invariants validated in
  `JackpotService.create`.
- **A16 — Built-in PRNG is sufficient for this scope.** Reward evaluation draws from
  `java.util.Random` (`random() < chance`); the `Random` is injected and seedable
  (`jackpot.reward.rng-seed`) so tests are deterministic. Production money jackpots would want a
  CSPRNG (e.g. `SecureRandom`).
- **A17 — Embedded in-memory Kafka is the assignment's "mock"; real Kafka code throughout.** The
  default `embedded` mode boots an in-JVM `EmbeddedKafkaKraftBroker` and uses the real
  `KafkaTemplate` + `@KafkaListener`, so the whole flow runs with one command and no external infra.
  `spring-kafka-test` (which provides that broker) is on the runtime classpath for this reason; a
  production build would move it back to `test` scope and run in `external` mode. A literal
  `log-only` producer is kept but is not the default because it does not drive the consumer.
- **A18 — Reward amount = the whole jackpot pool, returned in the response.** The assignment does not
  define the payout size; since the pool resets to its initial value on a win, the winner takes the
  entire pool at win time (`rewardAmount = poolAfter`). It is persisted at win time in the consumer
  (`RewardService.evaluate`, A2) and later read back by the read-only `GET …/reward`
  (present only when `won = true`).
- **A19 — Inbound bet amount truncated to the money scale, surplus digits discarded.** A `betAmount`
  with more than 4 decimals is truncated — dropped, not rounded — with `RoundingMode.DOWN` before
  publishing (e.g. `50.00509 → 50.0050`). Validation rejects zero/negative amounts first; truncation
  applies only to excess precision.