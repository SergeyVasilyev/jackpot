package com.sportygroup.jackpot.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Reward strategy parameters, stored inline as the {@code reward_params} JSON column on the
 * jackpot row. All numbers are {@link BigDecimal} so JSON precision survives (A6).
 *
 * <ul>
 *   <li>{@code initialChance} — starting (Variable) / constant (Fixed) win chance, in (0, 1].</li>
 *   <li>{@code poolLimit} — pool size at which the Variable chance becomes 100% (Variable only).</li>
 * </ul>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RewardConfig {
    private BigDecimal initialChance;
    private BigDecimal poolLimit;
}
