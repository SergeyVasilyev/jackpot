package com.sportygroup.jackpot.messaging;

import com.sportygroup.jackpot.domain.CancellationReason;
import com.sportygroup.jackpot.dto.Bet;
import com.sportygroup.jackpot.dto.CancelledBet;
import com.sportygroup.jackpot.exception.DuplicateBetException;
import com.sportygroup.jackpot.exception.JackpotNotFoundException;
import com.sportygroup.jackpot.repository.JackpotRepository;
import com.sportygroup.jackpot.service.BetProcessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.kafka.support.Acknowledgment;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Consumer branch coverage: happy path, duplicate skip (A5), unknown jackpot (A3),
 * bounded contention → cancelled (A9), jackpot-gone race, unexpected error → cancelled, and poison
 * / structurally-invalid messages. Every terminal branch must ack exactly once so the partition is
 * never blocked.
 */
@ExtendWith(MockitoExtension.class)
class BetConsumerTest {

    private static final String PAYLOAD = "{\"betId\":\"b-1\",...}";
    private static final int MAX_RETRIES = 3;

    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private JackpotRepository jackpotRepository;
    @Mock
    private BetProcessor betProcessor;
    @Mock
    private BetEventPublisher publisher;
    @Mock
    private Acknowledgment ack;
    @Captor
    private ArgumentCaptor<CancelledBet> cancelledCaptor;

    private BetConsumer consumer;
    private final Bet bet = new Bet("b-1", "u-1", "jackpot-1", new BigDecimal("50.00"));

    @BeforeEach
    void setUp() {
        consumer = new BetConsumer(objectMapper, jackpotRepository, betProcessor, publisher, MAX_RETRIES, 0L);
    }

    private void parsesTo(Bet parsed) {
        when(objectMapper.readValue(PAYLOAD, Bet.class)).thenReturn(parsed);
    }

    @Test
    void happyPath_processesAndAcks() {
        parsesTo(bet);
        when(jackpotRepository.existsById("jackpot-1")).thenReturn(true);
        doNothing().when(betProcessor).process(bet);

        consumer.onMessage(PAYLOAD, ack);

        verify(betProcessor).process(bet);
        verify(ack).acknowledge();
        verify(publisher, never()).publishCancelled(any());
    }

    @Test
    void duplicateBet_skippedAndAckedWithoutRetry() {
        parsesTo(bet);
        when(jackpotRepository.existsById("jackpot-1")).thenReturn(true);
        doThrow(new DuplicateBetException("b-1", new RuntimeException())).when(betProcessor).process(bet);

        consumer.onMessage(PAYLOAD, ack);

        verify(betProcessor, times(1)).process(bet);
        verify(publisher, never()).publishCancelled(any());
        verify(ack).acknowledge();
    }

    @Test
    void contentionExhausted_routedToCancelledAndAcked() {
        parsesTo(bet);
        when(jackpotRepository.existsById("jackpot-1")).thenReturn(true);
        doThrow(new OptimisticLockingFailureException("conflict")).when(betProcessor).process(bet);

        consumer.onMessage(PAYLOAD, ack);

        verify(betProcessor, times(MAX_RETRIES)).process(bet);
        verify(publisher).publishCancelled(cancelledCaptor.capture());
        assertThat(cancelledCaptor.getValue().errorCode()).isEqualTo(CancellationReason.CONTENTION_RETRY_EXHAUSTED);
        verify(ack).acknowledge();
    }

    @Test
    void unknownJackpot_routedToCancelledWithoutProcessing() {
        parsesTo(bet);
        when(jackpotRepository.existsById("jackpot-1")).thenReturn(false);

        consumer.onMessage(PAYLOAD, ack);

        verify(betProcessor, never()).process(any());
        verify(publisher).publishCancelled(cancelledCaptor.capture());
        assertThat(cancelledCaptor.getValue().errorCode()).isEqualTo(CancellationReason.JACKPOT_NOT_FOUND);
        verify(ack).acknowledge();
    }

    @Test
    void jackpotGoneDuringProcessing_routedToCancelled() {
        parsesTo(bet);
        when(jackpotRepository.existsById("jackpot-1")).thenReturn(true);
        doThrow(new JackpotNotFoundException("gone")).when(betProcessor).process(bet);

        consumer.onMessage(PAYLOAD, ack);

        verify(betProcessor, times(1)).process(bet);
        verify(publisher).publishCancelled(cancelledCaptor.capture());
        assertThat(cancelledCaptor.getValue().errorCode()).isEqualTo(CancellationReason.JACKPOT_NOT_FOUND);
        verify(ack).acknowledge();
    }

    @Test
    void unexpectedError_routedToCancelledAndAcked() {
        parsesTo(bet);
        when(jackpotRepository.existsById("jackpot-1")).thenReturn(true);
        doThrow(new IllegalStateException("boom")).when(betProcessor).process(bet);

        consumer.onMessage(PAYLOAD, ack);

        // Not retried (not a contention conflict) and not redelivered (acked).
        verify(betProcessor, times(1)).process(bet);
        verify(publisher).publishCancelled(cancelledCaptor.capture());
        assertThat(cancelledCaptor.getValue().errorCode()).isEqualTo(CancellationReason.PROCESSING_ERROR);
        verify(ack).acknowledge();
    }

    @Test
    void poisonMessage_loggedAndAckedWithoutProcessing() {
        when(objectMapper.readValue(PAYLOAD, Bet.class)).thenThrow(new RuntimeException("malformed"));

        consumer.onMessage(PAYLOAD, ack);

        verifyNoInteractions(betProcessor, publisher);
        verify(jackpotRepository, never()).existsById(any());
        verify(ack).acknowledge();
    }

    @Test
    void structurallyInvalidBet_ackedWithoutProcessing() {
        parsesTo(new Bet("", "u-1", "jackpot-1", new BigDecimal("50.00"))); // blank betId

        consumer.onMessage(PAYLOAD, ack);

        verifyNoInteractions(betProcessor, publisher);
        verify(jackpotRepository, never()).existsById(any());
        verify(ack).acknowledge();
    }

    @Test
    void negativeAmountBet_treatedAsInvalidAndAcked() {
        parsesTo(new Bet("b-1", "u-1", "jackpot-1", new BigDecimal("-5.00")));

        consumer.onMessage(PAYLOAD, ack);

        verifyNoInteractions(betProcessor, publisher);
        verify(ack).acknowledge();
    }
}
