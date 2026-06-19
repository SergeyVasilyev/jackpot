package com.sportygroup.jackpot;

import com.sportygroup.jackpot.web.dto.PublishBetRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * End-to-end: publish a bet over REST → embedded Kafka → consumer contributes + evaluates →
 * read the outcome over REST (FR1, FR2, FR3, FR5, FR6).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class JackpotFlowIntegrationTest {

    @LocalServerPort
    private int port;

    private RestClient client;

    @BeforeEach
    void setUp() {
        client = RestClient.create("http://localhost:" + port);
    }

    private ResponseEntity<String> post(String uri, Object body) {
        return client.post().uri(uri).contentType(MediaType.APPLICATION_JSON).body(body)
                .exchange((req, res) -> ResponseEntity.status(res.getStatusCode()).body(readBody(res)));
    }

    private ResponseEntity<String> postRaw(String uri, String json) {
        return client.post().uri(uri).contentType(MediaType.APPLICATION_JSON).body(json)
                .exchange((req, res) -> ResponseEntity.status(res.getStatusCode()).body(readBody(res)));
    }

    private ResponseEntity<String> get(String uri) {
        return client.get().uri(uri)
                .exchange((req, res) -> ResponseEntity.status(res.getStatusCode()).body(readBody(res)));
    }

    private static String readBody(RestClient.RequestHeadersSpec.ConvertibleClientHttpResponse res) throws java.io.IOException {
        return new String(res.getBody().readAllBytes(), StandardCharsets.UTF_8);
    }

    @Test
    void publishedBetIsContributedAndEvaluated() {
        String betId = "b-" + UUID.randomUUID();
        PublishBetRequest request = new PublishBetRequest(betId, "u-42", "jackpot-1", new BigDecimal("50.00"));

        ResponseEntity<String> publish = post("/api/v1/bets", request);
        assertThat(publish.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(publish.getBody()).contains("PUBLISHED");

        // The outcome appears once the consumer has processed the bet (async window, A4).
        await().atMost(Duration.ofSeconds(20)).pollInterval(Duration.ofMillis(200)).untilAsserted(() -> {
            ResponseEntity<String> reward = get("/api/v1/bets/" + betId + "/reward");
            assertThat(reward.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(reward.getBody()).contains("\"jackpotId\":\"jackpot-1\"");
            // Seed 42 → first draw ≈ 0.727 > 0.01 chance → deterministic loss.
            assertThat(reward.getBody()).contains("\"won\":false");
        });
    }

    @Test
    void unknownJackpotIsRejectedOnPublish() {
        PublishBetRequest request = new PublishBetRequest("b-x", "u-1", "no-such-jackpot", new BigDecimal("10.00"));

        ResponseEntity<String> response = post("/api/v1/bets", request);

        assertThat(response.getStatusCode().value()).isEqualTo(422);
        assertThat(response.getBody()).contains("JACKPOT_NOT_FOUND");
    }

    @Test
    void unknownBetRewardQueryReturns404() {
        ResponseEntity<String> response = get("/api/v1/bets/does-not-exist/reward");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).contains("BET_NOT_FOUND");
    }

    @Test
    void validationErrorReturns400() {
        String badBody = "{ \"betId\": \"\", \"userId\": \"u\", \"jackpotId\": \"jackpot-1\", \"betAmount\": -5 }";

        ResponseEntity<String> response = postRaw("/api/v1/bets", badBody);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("VALIDATION_ERROR");
    }
}
