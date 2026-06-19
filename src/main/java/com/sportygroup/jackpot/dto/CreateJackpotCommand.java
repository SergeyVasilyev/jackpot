package com.sportygroup.jackpot.dto;

import com.sportygroup.jackpot.domain.ContributionType;
import com.sportygroup.jackpot.domain.RewardType;

import java.math.BigDecimal;

/**
 * Command to create a jackpot (A1). The {@code id} is optional — a GUID is generated when absent.
 * Maps directly to one entry of the initializer JSON file.
 */
public record CreateJackpotCommand(
        String id,
        BigDecimal initialPool,
        ContributionSpec contribution,
        RewardSpec reward) {

    public record ContributionSpec(
            ContributionType type,
            BigDecimal initialPct,
            BigDecimal minPct,
            BigDecimal poolThreshold) {
    }

    public record RewardSpec(
            RewardType type,
            BigDecimal initialChance,
            BigDecimal poolLimit) {
    }
}
