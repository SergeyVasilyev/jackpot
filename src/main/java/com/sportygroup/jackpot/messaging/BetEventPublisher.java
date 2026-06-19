package com.sportygroup.jackpot.messaging;

import com.sportygroup.jackpot.dto.Bet;
import com.sportygroup.jackpot.dto.CancelledBet;

/**
 * Abstraction over publishing bets and cancellations. Real Kafka vs. log-only
 * is chosen by {@code jackpot.kafka.mode}. Both methods are synchronous at the seam: they block on
 * the broker ack and throw on failure (so a 202 / a consumer ack only follows durable publication).
 */
public interface BetEventPublisher {

    /** Publish a bet to {@code jackpot-bets}. */
    void publish(Bet bet);

    /** Publish a cancellation to {@code jackpot-bets-cancelled} (consumer-only path). */
    void publishCancelled(CancelledBet cancelledBet);
}
