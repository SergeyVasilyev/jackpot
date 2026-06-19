package com.sportygroup.jackpot.domain.strategy;

import com.sportygroup.jackpot.domain.ContributionConfig;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class ContributionStrategyTest {

    private final FixedContributionStrategy fixed = new FixedContributionStrategy();
    private final VariableContributionStrategy variable = new VariableContributionStrategy();

    @Test
    void fixedContributionIsConstantPercentageOfStake() {
        ContributionConfig cfg = new ContributionConfig(new BigDecimal("0.10"), null, null);

        BigDecimal result = fixed.contribution(cfg, new BigDecimal("50.00"), BigDecimal.ZERO);

        assertThat(result).isEqualByComparingTo("5.0000");
    }

    @Test
    void variableContributionStartsAtInitialPctWhenPoolEmpty() {
        ContributionConfig cfg = new ContributionConfig(
                new BigDecimal("0.20"), new BigDecimal("0.05"), new BigDecimal("100000"));

        BigDecimal result = variable.contribution(cfg, new BigDecimal("50.00"), BigDecimal.ZERO);

        assertThat(result).isEqualByComparingTo("10.0000"); // 50 * 0.20
    }

    @Test
    void variableContributionReachesFloorAtThreshold() {
        ContributionConfig cfg = new ContributionConfig(
                new BigDecimal("0.20"), new BigDecimal("0.05"), new BigDecimal("100000"));

        BigDecimal result = variable.contribution(cfg, new BigDecimal("100.00"), new BigDecimal("100000"));

        assertThat(result).isEqualByComparingTo("5.0000"); // 100 * 0.05 (floor)
    }

    @Test
    void variableContributionClampsBeyondThreshold() {
        ContributionConfig cfg = new ContributionConfig(
                new BigDecimal("0.20"), new BigDecimal("0.05"), new BigDecimal("100000"));

        BigDecimal result = variable.contribution(cfg, new BigDecimal("100.00"), new BigDecimal("500000"));

        assertThat(result).isEqualByComparingTo("5.0000"); // progress clamped to 1 → floor
    }
}
