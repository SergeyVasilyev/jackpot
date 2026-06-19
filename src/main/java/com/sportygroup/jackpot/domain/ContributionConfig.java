package com.sportygroup.jackpot.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Contribution strategy parameters, stored inline as the {@code contribution_params} JSON column
 * on the jackpot row. All numbers are {@link BigDecimal} so JSON precision survives (A6).
 *
 * <ul>
 *   <li>{@code initialPct} — starting (Variable) / constant (Fixed) contribution rate, in (0, 1].</li>
 *   <li>{@code minPct} — floor the Variable rate never drops below (Variable only).</li>
 *   <li>{@code poolThreshold} — pool size at which the Variable rate reaches the floor (Variable only).</li>
 * </ul>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContributionConfig {
    private BigDecimal initialPct;
    private BigDecimal minPct;
    private BigDecimal poolThreshold;
}
