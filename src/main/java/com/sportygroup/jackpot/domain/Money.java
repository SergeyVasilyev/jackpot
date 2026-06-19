package com.sportygroup.jackpot.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Monetary scale helpers (A6). Money is a {@code DECIMAL(19,4)}: every computed amount is
 * rounded to 4 decimals with banker's rounding ({@link RoundingMode#HALF_EVEN}); surplus input
 * digits are truncated ({@link RoundingMode#DOWN}).
 */
public final class Money {

    public static final int SCALE = 4;

    private Money() {
    }

    /** Round a computed monetary amount to the money scale with banker's rounding (A6). */
    public static BigDecimal round(BigDecimal value) {
        return value.setScale(SCALE, RoundingMode.HALF_EVEN);
    }

    /** Truncate an inbound amount to the money scale, discarding surplus digits (A6). */
    public static BigDecimal truncate(BigDecimal value) {
        return value.setScale(SCALE, RoundingMode.DOWN);
    }
}
