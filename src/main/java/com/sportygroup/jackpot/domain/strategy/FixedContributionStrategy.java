package com.sportygroup.jackpot.domain.strategy;

import com.sportygroup.jackpot.domain.ContributionConfig;
import com.sportygroup.jackpot.domain.ContributionType;
import com.sportygroup.jackpot.domain.Money;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** Fixed contribution: a constant percentage of the stake — {@code contribution = stake × initialPct} (A14). */
@Component
public class FixedContributionStrategy implements ContributionStrategy {

    @Override
    public ContributionType type() {
        return ContributionType.FIXED;
    }

    @Override
    public BigDecimal contribution(ContributionConfig config, BigDecimal stake, BigDecimal poolBefore) {
        return Money.round(stake.multiply(config.getInitialPct()));
    }
}
