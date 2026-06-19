package com.sportygroup.jackpot.service;

import com.sportygroup.jackpot.dto.Bet;
import com.sportygroup.jackpot.exception.JackpotNotFoundException;
import com.sportygroup.jackpot.messaging.BetEventPublisher;
import com.sportygroup.jackpot.repository.JackpotRepository;
import com.sportygroup.jackpot.web.dto.PublishBetRequest;
import com.sportygroup.jackpot.web.dto.PublishBetResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BetServiceTest {

    @Mock
    private JackpotRepository jackpotRepository;
    @Mock
    private BetEventPublisher publisher;
    @Captor
    private ArgumentCaptor<Bet> betCaptor;
    @InjectMocks
    private BetService betService;

    @Test
    void publish_existingJackpot_truncatesAmountAndPublishes() {
        when(jackpotRepository.existsById("jackpot-1")).thenReturn(true);
        // Surplus decimals beyond the money scale (4) must be truncated on publish (A6).
        PublishBetRequest request = new PublishBetRequest("b-1", "u-1", "jackpot-1", new BigDecimal("50.00509"));

        PublishBetResponse response = betService.publish(request);

        assertThat(response.betId()).isEqualTo("b-1");
        assertThat(response.status()).isEqualTo("PUBLISHED");

        verify(publisher).publish(betCaptor.capture());
        Bet published = betCaptor.getValue();
        assertThat(published.jackpotId()).isEqualTo("jackpot-1");
        assertThat(published.betAmount()).isEqualByComparingTo("50.0050"); // truncated, not rounded up
    }

    @Test
    void publish_unknownJackpot_throwsAndPublishesNothing() {
        when(jackpotRepository.existsById("nope")).thenReturn(false);
        PublishBetRequest request = new PublishBetRequest("b-1", "u-1", "nope", new BigDecimal("10.00"));

        assertThatThrownBy(() -> betService.publish(request))
                .isInstanceOf(JackpotNotFoundException.class);

        verify(publisher, never()).publish(any());
    }
}
