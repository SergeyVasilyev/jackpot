package com.sportygroup.jackpot.dto;

import com.sportygroup.jackpot.domain.ContributionType;
import com.sportygroup.jackpot.domain.Jackpot;
import com.sportygroup.jackpot.domain.RewardType;

import java.math.BigDecimal;

/**
 * DTO returned by {@code JackpotService.create} (A1) — includes the id (supplied, generated, or the
 * existing one on a duplicate).
 */
public record JackpotDto(
        String id,
        BigDecimal initialPool,
        BigDecimal currentPool,
        ContributionType contributionType,
        RewardType rewardType) {

    public static JackpotDto from(Jackpot jackpot) {
        return new JackpotDto(
                jackpot.getId(),
                jackpot.getInitialPool(),
                jackpot.getCurrentPool(),
                jackpot.getContributionType(),
                jackpot.getRewardType());
    }
}
