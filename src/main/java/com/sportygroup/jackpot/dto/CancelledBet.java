package com.sportygroup.jackpot.dto;

import com.sportygroup.jackpot.domain.CancellationReason;

/**
 * A bet that could not be processed; the message on {@code jackpot-bets-cancelled} (A3, A9).
 * It is the original {@link Bet} plus the cancellation reason.
 */
public record CancelledBet(Bet bet, CancellationReason errorCode, String message) {
}
