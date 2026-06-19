package com.sportygroup.jackpot.messaging;

import com.sportygroup.jackpot.config.KafkaTopics;
import com.sportygroup.jackpot.dto.Bet;
import com.sportygroup.jackpot.dto.CancelledBet;
import lombok.extern.slf4j.Slf4j;

/**
 * Log-only publisher (A17): the most literal reading of the assignment's "mock the producer, just
 * log the payload". Active only in {@code log-only} mode; note it does <b>not</b> drive the
 * consumer, so contribution/reward never run in this mode.
 */
@Slf4j
public class MockBetEventPublisher implements BetEventPublisher {

    @Override
    public void publish(Bet bet) {
        log.info("MOCK {}: {}", KafkaTopics.BETS, bet);
    }

    @Override
    public void publishCancelled(CancelledBet cancelledBet) {
        log.info("MOCK {}: {}", KafkaTopics.BETS_CANCELLED, cancelledBet);
    }
}
