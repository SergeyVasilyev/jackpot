package com.sportygroup.jackpot.dto;

import java.math.BigDecimal;

/**
 * A wager placed by a user; the Kafka message on {@code jackpot-bets}.
 * {@code betAmount} is monetary — deserialized straight into {@link BigDecimal} so precision is
 * not lost on the message hop (A6).
 */
public record Bet(String betId, String userId, String jackpotId, BigDecimal betAmount) {
}
