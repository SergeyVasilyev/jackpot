package com.sportygroup.jackpot.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Immutable record of a bet's contribution to a pool (FR4). The unique {@code bet_id}
 * is the idempotency / dedup gate: a redelivered bet violates it and the whole attempt rolls
 * back (A5).
 */
@Entity
@Table(name = "jackpot_contribution",
        uniqueConstraints = @UniqueConstraint(name = "uk_contribution_bet_id", columnNames = "bet_id"))
@Getter
@Setter
@NoArgsConstructor
public class JackpotContribution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "bet_id", nullable = false, unique = true)
    private String betId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "jackpot_id", nullable = false)
    private String jackpotId;

    @Column(name = "stake_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal stakeAmount;

    @Column(name = "contribution_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal contributionAmount;

    /** Pool after this contribution was applied ({@code poolAfter}, A13). */
    @Column(name = "current_jackpot_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal currentJackpotAmount;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public JackpotContribution(String betId, String userId, String jackpotId, BigDecimal stakeAmount,
                               BigDecimal contributionAmount, BigDecimal currentJackpotAmount, Instant createdAt) {
        this.betId = betId;
        this.userId = userId;
        this.jackpotId = jackpotId;
        this.stakeAmount = stakeAmount;
        this.contributionAmount = contributionAmount;
        this.currentJackpotAmount = currentJackpotAmount;
        this.createdAt = createdAt;
    }
}
