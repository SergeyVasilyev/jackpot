package com.sportygroup.jackpot.domain.strategy;

import com.sportygroup.jackpot.domain.ContributionConfig;
import com.sportygroup.jackpot.domain.ContributionType;

import java.math.BigDecimal;

/**
 * Computes how much of a stake contributes to a pool. Takes the config value object and
 * the {@code poolBefore} snapshot (A13) — not the {@code Jackpot} entity — so it is pure and
 * trivially unit-testable. A new configuration is added as a new bean + enum value (Open/Closed).
 */
public interface ContributionStrategy {

    ContributionType type();

    /** Contribution amount, already rounded to the money scale (A6). */
    BigDecimal contribution(ContributionConfig config, BigDecimal stake, BigDecimal poolBefore);
}
