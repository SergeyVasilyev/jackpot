package com.sportygroup.jackpot.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;

/**
 * Aggregate root: a prize pool with its contribution + reward configurations.
 * Owns its pool and strategy parameters; the parameters live inline as JSON columns so adding a
 * strategy type needs no schema change. The {@code @Version} column backstops concurrent pool
 * updates with optimistic locking (A8).
 */
@Entity
@Table(name = "jackpot")
@Getter
@Setter
@NoArgsConstructor
public class Jackpot {

    @Id
    @Column(name = "id", nullable = false)
    private String id;

    @Column(name = "initial_pool", nullable = false, precision = 19, scale = 4)
    private BigDecimal initialPool;

    @Column(name = "current_pool", nullable = false, precision = 19, scale = 4)
    private BigDecimal currentPool;

    @Version
    @Column(name = "version")
    private Long version;

    @Enumerated(EnumType.STRING)
    @Column(name = "contribution_type", nullable = false)
    private ContributionType contributionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "reward_type", nullable = false)
    private RewardType rewardType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "contribution_params")
    private ContributionConfig contributionConfig;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "reward_params")
    private RewardConfig rewardConfig;

    public Jackpot(String id, BigDecimal initialPool, ContributionType contributionType,
                   ContributionConfig contributionConfig, RewardType rewardType, RewardConfig rewardConfig) {
        this.id = id;
        this.initialPool = initialPool;
        this.currentPool = initialPool;
        this.contributionType = contributionType;
        this.contributionConfig = contributionConfig;
        this.rewardType = rewardType;
        this.rewardConfig = rewardConfig;
    }

    /** Apply a contribution to the pool: {@code poolAfter = poolBefore + contribution} (A13). */
    public void applyContribution(BigDecimal contribution) {
        this.currentPool = this.currentPool.add(contribution);
    }

    /** Reset the pool to its initial value after a win (FR6, A2). */
    public void resetPool() {
        this.currentPool = this.initialPool;
    }
}
