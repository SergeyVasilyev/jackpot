package com.sportygroup.jackpot.domain.strategy;

import com.sportygroup.jackpot.domain.ContributionType;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Resolves the {@link ContributionStrategy} bean for a jackpot's {@link ContributionType}.
 * Kept separate from the reward factory because the two families share the {@code FIXED}/{@code VARIABLE}
 * key space — one shared map would collide.
 */
@Component
public class ContributionStrategyFactory {

    private final Map<ContributionType, ContributionStrategy> byType = new EnumMap<>(ContributionType.class);

    public ContributionStrategyFactory(List<ContributionStrategy> strategies) {
        for (ContributionStrategy strategy : strategies) {
            byType.put(strategy.type(), strategy);
        }
    }

    public ContributionStrategy get(ContributionType type) {
        ContributionStrategy strategy = byType.get(type);
        if (strategy == null) {
            throw new IllegalStateException("No ContributionStrategy for type " + type);
        }
        return strategy;
    }
}
