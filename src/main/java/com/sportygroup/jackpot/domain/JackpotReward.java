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
 * Immutable record of a won reward (FR6). The unique {@code bet_id} enforces at most one
 * reward per bet, preventing a double payout (A2).
 */
@Entity
@Table(name = "jackpot_reward",
        uniqueConstraints = @UniqueConstraint(name = "uk_reward_bet_id", columnNames = "bet_id"))
@Getter
@Setter
@NoArgsConstructor
public class JackpotReward {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "bet_id", nullable = false, unique = true)
    private String betId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "jackpot_id", nullable = false)
    private String jackpotId;

    /** The payout: the whole pool at win time ({@code poolAfter}, A13). */
    @Column(name = "reward_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal rewardAmount;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public JackpotReward(String betId, String userId, String jackpotId, BigDecimal rewardAmount, Instant createdAt) {
        this.betId = betId;
        this.userId = userId;
        this.jackpotId = jackpotId;
        this.rewardAmount = rewardAmount;
        this.createdAt = createdAt;
    }
}
