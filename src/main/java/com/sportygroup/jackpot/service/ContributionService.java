package com.sportygroup.jackpot.service;

import com.sportygroup.jackpot.domain.Jackpot;
import com.sportygroup.jackpot.domain.JackpotContribution;
import com.sportygroup.jackpot.domain.strategy.ContributionStrategy;
import com.sportygroup.jackpot.domain.strategy.ContributionStrategyFactory;
import com.sportygroup.jackpot.dto.Bet;
import com.sportygroup.jackpot.exception.DuplicateBetException;
import com.sportygroup.jackpot.repository.JackpotContributionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Computes and persists a bet's contribution (FR3, FR4). Persists the contribution <b>first</b>
 * (the unique {@code bet_id} dedup gate, A5), then updates the pool ({@code poolBefore → poolAfter},
 * A13). The eager {@code saveAndFlush} surfaces a duplicate {@code bet_id} as a
 * {@link DuplicateBetException} inside the attempt's transaction so it is caught within the
 * consumer's retry loop.
 */
@Service
@RequiredArgsConstructor
public class ContributionService {

    private final ContributionStrategyFactory contributionStrategyFactory;
    private final JackpotContributionRepository contributionRepository;

    /** @return the pool after this contribution ({@code poolAfter}, A13). */
    public BigDecimal contribute(Bet bet, Jackpot jackpot) {
        BigDecimal poolBefore = jackpot.getCurrentPool();
        ContributionStrategy strategy = contributionStrategyFactory.get(jackpot.getContributionType());
        BigDecimal contribution = strategy.contribution(jackpot.getContributionConfig(), bet.betAmount(), poolBefore);
        BigDecimal poolAfter = poolBefore.add(contribution);

        JackpotContribution record = new JackpotContribution(
                bet.betId(), bet.userId(), jackpot.getId(),
                bet.betAmount(), contribution, poolAfter, Instant.now());

        try {
            contributionRepository.saveAndFlush(record);
        } catch (DataIntegrityViolationException e) {
            // Redelivered / duplicate bet — the whole attempt must roll back as a no-op (A5).
            throw new DuplicateBetException(bet.betId(), e);
        }

        jackpot.applyContribution(contribution);
        return poolAfter;
    }
}
