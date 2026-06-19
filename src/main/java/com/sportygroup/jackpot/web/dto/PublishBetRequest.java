package com.sportygroup.jackpot.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** Request body for {@code POST /api/v1/bets}. */
public record PublishBetRequest(
        @NotBlank String betId,
        @NotBlank String userId,
        @NotBlank String jackpotId,
        @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal betAmount) {
}
