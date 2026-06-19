package com.sportygroup.jackpot.service;

import com.sportygroup.jackpot.domain.Jackpot;
import com.sportygroup.jackpot.dto.Bet;
import com.sportygroup.jackpot.exception.JackpotNotFoundException;
import com.sportygroup.jackpot.repository.JackpotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Processes one bet as a single transaction: contribution (insert with the unique
 * {@code bet_id} dedup gate, then pool update) and, on a win, reward insert + pool reset all commit
 * together. Each call is its own transaction, so a retried attempt (A9) fully rolls back on
 * conflict and the next attempt re-inserts cleanly (A5). Optimistic-lock conflicts and duplicate
 * violations are surfaced by the eager flushes inside this transaction and caught by the caller at
 * the transaction boundary.
 */
@Service
@RequiredArgsConstructor
public class BetProcessor {

    private final JackpotRepository jackpotRepository;
    private final ContributionService contributionService;
    private final RewardService rewardService;

    @Transactional
    public void process(Bet bet) {
        Jackpot jackpot = jackpotRepository.findById(bet.jackpotId())
                .orElseThrow(() -> new JackpotNotFoundException(
                        "No jackpot " + bet.jackpotId() + " for bet " + bet.betId()));

        BigDecimal poolAfter = contributionService.contribute(bet, jackpot);
        rewardService.evaluate(bet, jackpot, poolAfter);
    }
}
