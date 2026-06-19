package com.sportygroup.jackpot.domain.strategy;

import com.sportygroup.jackpot.domain.RewardConfig;
import com.sportygroup.jackpot.domain.RewardType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Random;

/** Fixed reward: a constant win probability — {@code win = random() < initialChance} (A15). */
@Component
public class FixedRewardStrategy implements RewardStrategy {

    private final Random random;

    public FixedRewardStrategy(Random rewardRandom) {
        this.random = rewardRandom;
    }

    @Override
    public RewardType type() {
        return RewardType.FIXED;
    }

    @Override
    public boolean isWin(RewardConfig config, BigDecimal poolAfter) {
        return random.nextDouble() < config.getInitialChance().doubleValue();
    }
}
