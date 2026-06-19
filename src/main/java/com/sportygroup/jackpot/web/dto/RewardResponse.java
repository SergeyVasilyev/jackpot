package com.sportygroup.jackpot.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Response for {@code GET /api/v1/bets/{betId}/reward}. {@code rewardAmount}/{@code createdAt}
 * are present only on a win; nulls are omitted.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RewardResponse(
        String betId,
        String userId,
        String jackpotId,
        boolean won,
        BigDecimal rewardAmount,
        Instant createdAt) {
}
