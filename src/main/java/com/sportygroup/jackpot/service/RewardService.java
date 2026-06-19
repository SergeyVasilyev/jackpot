package com.sportygroup.jackpot.service;

import com.sportygroup.jackpot.domain.Jackpot;
import com.sportygroup.jackpot.domain.JackpotContribution;
import com.sportygroup.jackpot.domain.JackpotReward;
import com.sportygroup.jackpot.domain.strategy.RewardStrategy;
import com.sportygroup.jackpot.domain.strategy.RewardStrategyFactory;
import com.sportygroup.jackpot.dto.Bet;
import com.sportygroup.jackpot.exception.BetNotFoundException;
import com.sportygroup.jackpot.repository.JackpotContributionRepository;
import com.sportygroup.jackpot.repository.JackpotRewardRepository;
import com.sportygroup.jackpot.web.dto.RewardResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Two paths (A2):
 * <ul>
 *   <li><b>consumer path</b> — {@link #evaluate}: decide the win on {@code poolAfter}; on a win
 *       persist a {@link JackpotReward} and reset the pool (FR5, FR6).</li>
 *   <li><b>API path</b> — {@link #getOutcome}: read the already-decided outcome for a bet, derived
 *       from whether a reward row exists.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class RewardService {

    private final RewardStrategyFactory rewardStrategyFactory;
    private final JackpotRewardRepository rewardRepository;
    private final JackpotContributionRepository contributionRepository;

    /** Consumer path: evaluate the win on {@code poolAfter}; on a win persist the reward + reset the pool. */
    public void evaluate(Bet bet, Jackpot jackpot, BigDecimal poolAfter) {
        RewardStrategy strategy = rewardStrategyFactory.get(jackpot.getRewardType());
        if (strategy.isWin(jackpot.getRewardConfig(), poolAfter)) {
            JackpotReward reward = new JackpotReward(
                    bet.betId(), bet.userId(), jackpot.getId(), poolAfter, Instant.now());
            rewardRepository.saveAndFlush(reward);
            jackpot.resetPool();
        }
    }

    /** API path: read the stored win/loss outcome for a processed bet (read-only, A2). */
    @Transactional(readOnly = true)
    public RewardResponse getOutcome(String betId) {
        JackpotContribution contribution = contributionRepository.findByBetId(betId)
                .orElseThrow(() -> new BetNotFoundException("No processed bet " + betId));

        return rewardRepository.findByBetId(betId)
                .map(reward -> new RewardResponse(
                        contribution.getBetId(), contribution.getUserId(), contribution.getJackpotId(),
                        true, reward.getRewardAmount(), reward.getCreatedAt()))
                .orElseGet(() -> new RewardResponse(
                        contribution.getBetId(), contribution.getUserId(), contribution.getJackpotId(),
                        false, null, null));
    }
}
