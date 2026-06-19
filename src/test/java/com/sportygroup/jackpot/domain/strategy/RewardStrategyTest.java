package com.sportygroup.jackpot.domain.strategy;

import com.sportygroup.jackpot.domain.RewardConfig;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

class RewardStrategyTest {

    /** A Random with a fixed draw, so win/lose is deterministic regardless of seed. */
    private static Random fixedDraw(double value) {
        return new Random() {
            @Override
            public double nextDouble() {
                return value;
            }
        };
    }

    @Test
    void fixedRewardWinsWhenDrawBelowChance() {
        FixedRewardStrategy strategy = new FixedRewardStrategy(fixedDraw(0.005));
        RewardConfig cfg = new RewardConfig(new BigDecimal("0.01"), null);

        assertThat(strategy.isWin(cfg, BigDecimal.ZERO)).isTrue();
    }

    @Test
    void fixedRewardLosesWhenDrawAboveChance() {
        FixedRewardStrategy strategy = new FixedRewardStrategy(fixedDraw(0.5));
        RewardConfig cfg = new RewardConfig(new BigDecimal("0.01"), null);

        assertThat(strategy.isWin(cfg, BigDecimal.ZERO)).isFalse();
    }

    @Test
    void variableRewardIsCertainAtPoolLimit() {
        VariableRewardStrategy strategy = new VariableRewardStrategy(fixedDraw(0.999999));
        RewardConfig cfg = new RewardConfig(new BigDecimal("0.001"), new BigDecimal("1000000"));

        assertThat(strategy.isWin(cfg, new BigDecimal("1000000"))).isTrue();
    }

    @Test
    void variableRewardGrowsWithPool() {
        // At half the limit: chance = 0.001 + (1 - 0.001) * 0.5 ≈ 0.5005
        VariableRewardStrategy strategy = new VariableRewardStrategy(fixedDraw(0.4));
        RewardConfig cfg = new RewardConfig(new BigDecimal("0.001"), new BigDecimal("1000000"));

        assertThat(strategy.isWin(cfg, new BigDecimal("500000"))).isTrue();
    }
}
