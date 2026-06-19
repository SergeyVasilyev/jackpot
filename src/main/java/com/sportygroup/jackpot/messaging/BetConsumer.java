package com.sportygroup.jackpot.messaging;

import com.sportygroup.jackpot.config.KafkaTopics;
import com.sportygroup.jackpot.domain.CancellationReason;
import com.sportygroup.jackpot.dto.Bet;
import com.sportygroup.jackpot.dto.CancelledBet;
import com.sportygroup.jackpot.exception.DuplicateBetException;
import com.sportygroup.jackpot.exception.JackpotNotFoundException;
import com.sportygroup.jackpot.repository.JackpotRepository;
import com.sportygroup.jackpot.service.BetProcessor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;

import java.math.BigDecimal;

/**
 * Reads {@code jackpot-bets} and runs contribution + reward evaluation as one unit.
 * Single ack point for every terminal branch (success / duplicate-skip / cancelled / poison), after
 * the transaction commits or the cancellation is published, so a crash before that redelivers and
 * A5 dedups the replay.
 */
@Slf4j
public class BetConsumer {

    private final ObjectMapper objectMapper;
    private final JackpotRepository jackpotRepository;
    private final BetProcessor betProcessor;
    private final BetEventPublisher publisher;
    private final int maxRetries;
    private final long backoffMs;

    public BetConsumer(ObjectMapper objectMapper, JackpotRepository jackpotRepository, BetProcessor betProcessor,
                       BetEventPublisher publisher, int maxRetries, long backoffMs) {
        this.objectMapper = objectMapper;
        this.jackpotRepository = jackpotRepository;
        this.betProcessor = betProcessor;
        this.publisher = publisher;
        this.maxRetries = maxRetries;
        this.backoffMs = backoffMs;
    }

    // groupId comes from the consumer factory (jackpot.kafka.group-id) — not repeated here.
    @KafkaListener(topics = KafkaTopics.BETS, containerFactory = "kafkaListenerContainerFactory")
    public void onMessage(String payload, Acknowledgment ack) {
        Bet bet = parse(payload);
        if (bet == null) {
            // Poison message → DLQ; simplified here to log.error + ack so the partition is not blocked.
            log.error("Invalid message routed to DLQ (log-only): {}", payload);
            ack.acknowledge();
            return;
        }

        if (!jackpotRepository.existsById(bet.jackpotId())) {
            // Safety net for the async race / external producer (A3).
            publisher.publishCancelled(new CancelledBet(bet, CancellationReason.JACKPOT_NOT_FOUND,
                    "No jackpot " + bet.jackpotId() + " for bet " + bet.betId()));
            ack.acknowledge();
            return;
        }

        process(bet);
        ack.acknowledge();
    }

    /** Bounded retry on optimistic-lock contention; each attempt is its own transaction (A8, A9). */
    private void process(Bet bet) {
        int attempt = 0;
        while (true) {
            try {
                betProcessor.process(bet);
                return;
            } catch (DuplicateBetException e) {
                log.info("Duplicate bet {} — already processed, skipping (A5)", bet.betId());
                return;
            } catch (OptimisticLockingFailureException e) {
                attempt++;
                if (attempt >= maxRetries) {
                    log.warn("Contention retries exhausted for bet {} — routing to cancelled (A9)", bet.betId());
                    publisher.publishCancelled(new CancelledBet(bet, CancellationReason.CONTENTION_RETRY_EXHAUSTED,
                            "Optimistic-lock retries exhausted for bet " + bet.betId()));
                    return;
                }
                backoff();
            } catch (JackpotNotFoundException e) {
                // Lost the async race against the pre-check (A3): the jackpot vanished between
                // existsById and the transaction. Park it rather than redeliver forever.
                log.warn("Jackpot gone while processing bet {} — routing to cancelled (A3)", bet.betId());
                publisher.publishCancelled(new CancelledBet(bet, CancellationReason.JACKPOT_NOT_FOUND, e.getMessage()));
                return;
            } catch (RuntimeException e) {
                // Any other failure (bug, DB outage, …) must not block the partition with an
                // unbounded redelivery loop: log it and park the bet on the cancelled topic, then ack.
                log.error("Unexpected error processing bet {} — routing to cancelled", bet.betId(), e);
                publisher.publishCancelled(new CancelledBet(bet, CancellationReason.PROCESSING_ERROR,
                        "Unexpected error processing bet " + bet.betId() + ": " + e.getMessage()));
                return;
            }
        }
    }

    private void backoff() {
        try {
            Thread.sleep(backoffMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted during contention backoff", e);
        }
    }

    private Bet parse(String payload) {
        Bet bet;
        try {
            bet = objectMapper.readValue(payload, Bet.class);
        } catch (Exception e) {
            log.error("Failed to deserialize bet payload: {}", payload, e);
            return null;
        }
        if (isBlank(bet.betId()) || isBlank(bet.userId()) || isBlank(bet.jackpotId())
                || bet.betAmount() == null || bet.betAmount().compareTo(BigDecimal.ZERO) <= 0) {
            log.error("Structurally invalid bet: {}", bet);
            return null;
        }
        return bet;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
