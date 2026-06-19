package com.sportygroup.jackpot.service;

import com.sportygroup.jackpot.domain.ContributionType;
import com.sportygroup.jackpot.domain.Jackpot;
import com.sportygroup.jackpot.domain.JackpotContribution;
import com.sportygroup.jackpot.domain.JackpotReward;
import com.sportygroup.jackpot.domain.RewardConfig;
import com.sportygroup.jackpot.domain.RewardType;
import com.sportygroup.jackpot.domain.strategy.RewardStrategy;
import com.sportygroup.jackpot.domain.strategy.RewardStrategyFactory;
import com.sportygroup.jackpot.dto.Bet;
import com.sportygroup.jackpot.exception.BetNotFoundException;
import com.sportygroup.jackpot.repository.JackpotContributionRepository;
import com.sportygroup.jackpot.repository.JackpotRewardRepository;
import com.sportygroup.jackpot.web.dto.RewardResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RewardServiceTest {

    @Mock
    private RewardStrategyFactory rewardStrategyFactory;
    @Mock
    private JackpotRewardRepository rewardRepository;
    @Mock
    private JackpotContributionRepository contributionRepository;
    @Mock
    private RewardStrategy rewardStrategy;
    @Captor
    private ArgumentCaptor<JackpotReward> rewardCaptor;
    @InjectMocks
    private RewardService rewardService;

    private final Bet bet = new Bet("b-1", "u-1", "jackpot-1", new BigDecimal("50.00"));

    private Jackpot jackpotAt(BigDecimal currentPool) {
        Jackpot jackpot = new Jackpot("jackpot-1", new BigDecimal("1000.0000"),
                ContributionType.FIXED, null, RewardType.FIXED, new RewardConfig(new BigDecimal("0.01"), null));
        jackpot.setCurrentPool(currentPool); // simulate the post-contribution state
        return jackpot;
    }

    @Test
    void evaluate_win_persistsRewardAndResetsPool() {
        Jackpot jackpot = jackpotAt(new BigDecimal("1100.0000"));
        BigDecimal poolAfter = new BigDecimal("1100.0000");
        when(rewardStrategyFactory.get(RewardType.FIXED)).thenReturn(rewardStrategy);
        when(rewardStrategy.isWin(eq(jackpot.getRewardConfig()), eq(poolAfter))).thenReturn(true);

        rewardService.evaluate(bet, jackpot, poolAfter);

        verify(rewardRepository).saveAndFlush(rewardCaptor.capture());
        JackpotReward saved = rewardCaptor.getValue();
        assertThat(saved.getBetId()).isEqualTo("b-1");
        assertThat(saved.getUserId()).isEqualTo("u-1");
        assertThat(saved.getJackpotId()).isEqualTo("jackpot-1");
        assertThat(saved.getRewardAmount()).isEqualByComparingTo("1100.0000"); // whole pool (A18)
        // Pool reset to the initial value after the win (FR6, A2).
        assertThat(jackpot.getCurrentPool()).isEqualByComparingTo("1000.0000");
    }

    @Test
    void evaluate_loss_leavesPoolAndPersistsNothing() {
        Jackpot jackpot = jackpotAt(new BigDecimal("1100.0000"));
        BigDecimal poolAfter = new BigDecimal("1100.0000");
        when(rewardStrategyFactory.get(RewardType.FIXED)).thenReturn(rewardStrategy);
        when(rewardStrategy.isWin(any(), eq(poolAfter))).thenReturn(false);

        rewardService.evaluate(bet, jackpot, poolAfter);

        verify(rewardRepository, never()).saveAndFlush(any());
        assertThat(jackpot.getCurrentPool()).isEqualByComparingTo("1100.0000");
    }

    @Test
    void getOutcome_win_returnsWonWithStoredAmount() {
        JackpotContribution contribution = new JackpotContribution("b-1", "u-1", "jackpot-1",
                new BigDecimal("50.0000"), new BigDecimal("5.0000"), new BigDecimal("1100.0000"), Instant.now());
        JackpotReward reward = new JackpotReward("b-1", "u-1", "jackpot-1", new BigDecimal("1100.0000"), Instant.now());
        when(contributionRepository.findByBetId("b-1")).thenReturn(Optional.of(contribution));
        when(rewardRepository.findByBetId("b-1")).thenReturn(Optional.of(reward));

        RewardResponse response = rewardService.getOutcome("b-1");

        assertThat(response.won()).isTrue();
        assertThat(response.betId()).isEqualTo("b-1");
        assertThat(response.jackpotId()).isEqualTo("jackpot-1");
        assertThat(response.rewardAmount()).isEqualByComparingTo("1100.0000");
    }

    @Test
    void getOutcome_loss_returnsNotWonWithNullAmount() {
        JackpotContribution contribution = new JackpotContribution("b-1", "u-1", "jackpot-1",
                new BigDecimal("50.0000"), new BigDecimal("5.0000"), new BigDecimal("1005.0000"), Instant.now());
        when(contributionRepository.findByBetId("b-1")).thenReturn(Optional.of(contribution));
        when(rewardRepository.findByBetId("b-1")).thenReturn(Optional.empty());

        RewardResponse response = rewardService.getOutcome("b-1");

        assertThat(response.won()).isFalse();
        assertThat(response.rewardAmount()).isNull();
        assertThat(response.createdAt()).isNull();
    }

    @Test
    void getOutcome_unknownBet_throwsBetNotFound() {
        when(contributionRepository.findByBetId("nope")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> rewardService.getOutcome("nope"))
                .isInstanceOf(BetNotFoundException.class);
    }
}
