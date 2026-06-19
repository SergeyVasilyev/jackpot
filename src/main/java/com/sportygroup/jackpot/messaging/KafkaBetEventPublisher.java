package com.sportygroup.jackpot.messaging;

import com.sportygroup.jackpot.config.KafkaTopics;
import com.sportygroup.jackpot.dto.Bet;
import com.sportygroup.jackpot.dto.CancelledBet;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Real Kafka publisher (A17), used in {@code embedded} and {@code external} modes. Synchronous at
 * the seam: each send blocks on the broker ack and throws on failure — keyed by
 * {@code jackpotId} so a jackpot's bets keep order within a partition (A8).
 */
@Slf4j
public class KafkaBetEventPublisher implements BetEventPublisher {

    private static final long SEND_TIMEOUT_SECONDS = 30;

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public KafkaBetEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void publish(Bet bet) {
        send(KafkaTopics.BETS, bet.jackpotId(), bet);
    }

    @Override
    public void publishCancelled(CancelledBet cancelledBet) {
        send(KafkaTopics.BETS_CANCELLED, cancelledBet.bet().jackpotId(), cancelledBet);
    }

    private void send(String topic, String key, Object payload) {
        try {
            kafkaTemplate.send(topic, key, payload).get(SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while publishing to " + topic, e);
        } catch (ExecutionException | TimeoutException e) {
            throw new IllegalStateException("Failed to publish to " + topic, e);
        }
    }
}
