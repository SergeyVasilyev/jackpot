package com.sportygroup.jackpot.domain.strategy;

import com.sportygroup.jackpot.domain.RewardConfig;
import com.sportygroup.jackpot.domain.RewardType;

import java.math.BigDecimal;

/**
 * Decides whether a bet wins. Takes the config value object and the {@code poolAfter}
 * snapshot (A13) plus an injected RNG, so it is pure given the RNG and unit-testable with a seed.
 * The reward amount on a win is {@code poolAfter}, decided by {@code RewardService}.
 */
public interface RewardStrategy {

    RewardType type();

    boolean isWin(RewardConfig config, BigDecimal poolAfter);
}
