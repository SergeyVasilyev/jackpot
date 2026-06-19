package com.sportygroup.jackpot.domain;

/**
 * Error codes carried on the {@code jackpot-bets-cancelled} topic (A3, A9).
 * Kept as an enum so more cancellation reasons can be added later.
 */
public enum CancellationReason {
    JACKPOT_NOT_FOUND,
    CONTENTION_RETRY_EXHAUSTED,
    PROCESSING_ERROR
}
