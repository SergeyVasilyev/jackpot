package com.sportygroup.jackpot.domain.strategy;

import com.sportygroup.jackpot.domain.RewardConfig;
import com.sportygroup.jackpot.domain.RewardType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Random;

/**
 * Variable reward (A15): {@code chance} grows linearly with the pool from {@code initialChance}
 * up to 100%, reaching 100% when the pool hits {@code poolLimit}.
 *
 * <pre>
 * chance = min(1, initialChance + (1 − initialChance) × (poolAfter / poolLimit))
 * win    = poolAfter ≥ poolLimit ? true : random() < chance
 * </pre>
 *
 * A pure function of {@code poolAfter} (symmetric to the variable contribution, A13/A14).
 */
@Component
public class VariableRewardStrategy implements RewardStrategy {

    private final Random random;

    public VariableRewardStrategy(Random rewardRandom) {
        this.random = rewardRandom;
    }

    @Override
    public RewardType type() {
        return RewardType.VARIABLE;
    }

    @Override
    public boolean isWin(RewardConfig config, BigDecimal poolAfter) {
        BigDecimal poolLimit = config.getPoolLimit();
        if (poolAfter.compareTo(poolLimit) >= 0) {
            return true;
        }
        BigDecimal initialChance = config.getInitialChance();
        BigDecimal ratio = poolAfter.divide(poolLimit, 20, RoundingMode.HALF_EVEN);
        BigDecimal chance = initialChance.add(BigDecimal.ONE.subtract(initialChance).multiply(ratio));
        if (chance.compareTo(BigDecimal.ONE) > 0) {
            chance = BigDecimal.ONE;
        }
        return random.nextDouble() < chance.doubleValue();
    }
}
