package com.sportygroup.jackpot.service;

import com.sportygroup.jackpot.domain.ContributionConfig;
import com.sportygroup.jackpot.domain.ContributionType;
import com.sportygroup.jackpot.domain.Jackpot;
import com.sportygroup.jackpot.domain.RewardConfig;
import com.sportygroup.jackpot.domain.RewardType;
import com.sportygroup.jackpot.dto.CreateJackpotCommand;
import com.sportygroup.jackpot.dto.JackpotDto;
import com.sportygroup.jackpot.exception.InvalidJackpotConfigException;
import com.sportygroup.jackpot.repository.JackpotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Single creation entry point for jackpots (A1). Validates strategy parameters (the JSON column
 * cannot), persists, and returns a DTO. Idempotent on a supplied id: a duplicate logs a WARN and
 * keeps the existing jackpot. A future {@code POST /api/v1/jackpots} would be a thin wrapper.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class JackpotService {

    private final JackpotRepository jackpotRepository;

    @Transactional
    public JackpotDto create(CreateJackpotCommand command) {
        String id = (command.id() == null || command.id().isBlank())
                ? UUID.randomUUID().toString()
                : command.id();

        return jackpotRepository.findById(id)
                .map(existing -> {
                    log.warn("Jackpot {} already exists; keeping the existing one (idempotent create)", id);
                    return JackpotDto.from(existing);
                })
                .orElseGet(() -> {
                    Jackpot jackpot = buildAndValidate(id, command);
                    Jackpot saved = jackpotRepository.save(jackpot);
                    log.info("Created jackpot {} (contribution={}, reward={}, initialPool={})",
                            saved.getId(), saved.getContributionType(), saved.getRewardType(), saved.getInitialPool());
                    return JackpotDto.from(saved);
                });
    }

    private Jackpot buildAndValidate(String id, CreateJackpotCommand command) {
        if (command.initialPool() == null || command.initialPool().signum() < 0) {
            throw new InvalidJackpotConfigException("initialPool must be >= 0 for jackpot " + id);
        }
        if (command.contribution() == null || command.contribution().type() == null) {
            throw new InvalidJackpotConfigException("contribution.type is required for jackpot " + id);
        }
        if (command.reward() == null || command.reward().type() == null) {
            throw new InvalidJackpotConfigException("reward.type is required for jackpot " + id);
        }

        ContributionConfig contributionConfig = validateContribution(id, command.contribution());
        RewardConfig rewardConfig = validateReward(id, command.reward());

        return new Jackpot(id, command.initialPool(),
                command.contribution().type(), contributionConfig,
                command.reward().type(), rewardConfig);
    }

    private ContributionConfig validateContribution(String id, CreateJackpotCommand.ContributionSpec spec) {
        requireFraction(id, "contribution.initialPct", spec.initialPct());
        if (spec.type() == ContributionType.VARIABLE) {
            if (spec.minPct() == null || spec.minPct().signum() < 0 || spec.minPct().compareTo(BigDecimal.ONE) > 0) {
                throw new InvalidJackpotConfigException("contribution.minPct must be in [0, 1] for jackpot " + id);
            }
            if (spec.minPct().compareTo(spec.initialPct()) > 0) {
                throw new InvalidJackpotConfigException("contribution.minPct must be <= initialPct for jackpot " + id);
            }
            if (spec.poolThreshold() == null || spec.poolThreshold().signum() <= 0) {
                throw new InvalidJackpotConfigException("contribution.poolThreshold must be > 0 for jackpot " + id);
            }
        }
        return new ContributionConfig(spec.initialPct(), spec.minPct(), spec.poolThreshold());
    }

    private RewardConfig validateReward(String id, CreateJackpotCommand.RewardSpec spec) {
        requireFraction(id, "reward.initialChance", spec.initialChance());
        if (spec.type() == RewardType.VARIABLE
                && (spec.poolLimit() == null || spec.poolLimit().signum() <= 0)) {
            throw new InvalidJackpotConfigException("reward.poolLimit must be > 0 for jackpot " + id);
        }
        return new RewardConfig(spec.initialChance(), spec.poolLimit());
    }

    private void requireFraction(String id, String field, BigDecimal value) {
        if (value == null || value.signum() <= 0 || value.compareTo(BigDecimal.ONE) > 0) {
            throw new InvalidJackpotConfigException(field + " must be in (0, 1] for jackpot " + id);
        }
    }
}
