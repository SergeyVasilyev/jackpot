package com.sportygroup.jackpot.domain.strategy;

import com.sportygroup.jackpot.domain.RewardType;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Resolves the {@link RewardStrategy} bean for a jackpot's {@link RewardType}.
 * Kept separate from the contribution factory because the two families share the
 * {@code FIXED}/{@code VARIABLE} key space — one shared map would collide.
 */
@Component
public class RewardStrategyFactory {

    private final Map<RewardType, RewardStrategy> byType = new EnumMap<>(RewardType.class);

    public RewardStrategyFactory(List<RewardStrategy> strategies) {
        for (RewardStrategy strategy : strategies) {
            byType.put(strategy.type(), strategy);
        }
    }

    public RewardStrategy get(RewardType type) {
        RewardStrategy strategy = byType.get(type);
        if (strategy == null) {
            throw new IllegalStateException("No RewardStrategy for type " + type);
        }
        return strategy;
    }
}
