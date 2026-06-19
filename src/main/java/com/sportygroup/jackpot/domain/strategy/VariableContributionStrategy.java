package com.sportygroup.jackpot.domain.strategy;

import com.sportygroup.jackpot.domain.ContributionConfig;
import com.sportygroup.jackpot.domain.ContributionType;
import com.sportygroup.jackpot.domain.Money;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Variable contribution (A14): {@code pct} falls linearly with the pool from {@code initialPct}
 * down to the floor {@code minPct}, reaching the floor when the pool hits {@code poolThreshold}.
 *
 * <pre>
 * progress     = min(1, poolBefore / poolThreshold)
 * pct          = initialPct − (initialPct − minPct) × progress
 * contribution = stake × pct
 * </pre>
 *
 * A pure function of {@code poolBefore}; percentage math runs at full precision, the final amount
 * is rounded (A6).
 */
@Component
public class VariableContributionStrategy implements ContributionStrategy {

    @Override
    public ContributionType type() {
        return ContributionType.VARIABLE;
    }

    @Override
    public BigDecimal contribution(ContributionConfig config, BigDecimal stake, BigDecimal poolBefore) {
        BigDecimal initialPct = config.getInitialPct();
        BigDecimal minPct = config.getMinPct();
        BigDecimal poolThreshold = config.getPoolThreshold();

        BigDecimal progress = poolBefore.divide(poolThreshold, 20, java.math.RoundingMode.HALF_EVEN);
        if (progress.compareTo(BigDecimal.ONE) > 0) {
            progress = BigDecimal.ONE;
        }
        BigDecimal pct = initialPct.subtract(initialPct.subtract(minPct).multiply(progress));
        return Money.round(stake.multiply(pct));
    }
}
