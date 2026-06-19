package com.sportygroup.jackpot.service;

import com.sportygroup.jackpot.domain.ContributionType;
import com.sportygroup.jackpot.domain.Jackpot;
import com.sportygroup.jackpot.domain.JackpotContribution;
import com.sportygroup.jackpot.domain.RewardType;
import com.sportygroup.jackpot.domain.strategy.ContributionStrategy;
import com.sportygroup.jackpot.domain.strategy.ContributionStrategyFactory;
import com.sportygroup.jackpot.dto.Bet;
import com.sportygroup.jackpot.exception.DuplicateBetException;
import com.sportygroup.jackpot.repository.JackpotContributionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContributionServiceTest {

    @Mock
    private ContributionStrategyFactory contributionStrategyFactory;
    @Mock
    private JackpotContributionRepository contributionRepository;
    @Mock
    private ContributionStrategy contributionStrategy;
    @Captor
    private ArgumentCaptor<JackpotContribution> contributionCaptor;
    @InjectMocks
    private ContributionService contributionService;

    private final Bet bet = new Bet("b-1", "u-1", "jackpot-1", new BigDecimal("50.0000"));

    private Jackpot jackpot() {
        return new Jackpot("jackpot-1", new BigDecimal("1000.0000"),
                ContributionType.FIXED, null, RewardType.FIXED, null);
    }

    @Test
    void contribute_persistsRecord_updatesPool_returnsPoolAfter() {
        Jackpot jackpot = jackpot();
        when(contributionStrategyFactory.get(ContributionType.FIXED)).thenReturn(contributionStrategy);
        when(contributionStrategy.contribution(any(), eq(bet.betAmount()), eq(new BigDecimal("1000.0000"))))
                .thenReturn(new BigDecimal("5.0000"));
        when(contributionRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));

        BigDecimal poolAfter = contributionService.contribute(bet, jackpot);

        assertThat(poolAfter).isEqualByComparingTo("1005.0000");
        assertThat(jackpot.getCurrentPool()).isEqualByComparingTo("1005.0000");

        verify(contributionRepository).saveAndFlush(contributionCaptor.capture());
        JackpotContribution saved = contributionCaptor.getValue();
        assertThat(saved.getBetId()).isEqualTo("b-1");
        assertThat(saved.getStakeAmount()).isEqualByComparingTo("50.0000");
        assertThat(saved.getContributionAmount()).isEqualByComparingTo("5.0000");
        assertThat(saved.getCurrentJackpotAmount()).isEqualByComparingTo("1005.0000"); // poolAfter (A13)
    }

    @Test
    void contribute_duplicateBetId_throwsDuplicateBetException_andLeavesPool() {
        Jackpot jackpot = jackpot();
        when(contributionStrategyFactory.get(ContributionType.FIXED)).thenReturn(contributionStrategy);
        when(contributionStrategy.contribution(any(), any(), any())).thenReturn(new BigDecimal("5.0000"));
        when(contributionRepository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("dup bet_id"));

        assertThatThrownBy(() -> contributionService.contribute(bet, jackpot))
                .isInstanceOf(DuplicateBetException.class);

        // The pool update happens only after a successful insert, so a duplicate leaves it untouched (A5).
        assertThat(jackpot.getCurrentPool()).isEqualByComparingTo("1000.0000");
    }
}
