package com.sportygroup.jackpot.service;

import com.sportygroup.jackpot.domain.Money;
import com.sportygroup.jackpot.dto.Bet;
import com.sportygroup.jackpot.exception.JackpotNotFoundException;
import com.sportygroup.jackpot.messaging.BetEventPublisher;
import com.sportygroup.jackpot.repository.JackpotRepository;
import com.sportygroup.jackpot.web.dto.PublishBetRequest;
import com.sportygroup.jackpot.web.dto.PublishBetResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Publish path (FR1, A3): normalize the amount, pre-check that the jackpot exists, then publish to
 * {@code jackpot-bets}. A missing jackpot rejects the placement synchronously (422) — nothing is
 * published. Publishing is synchronous at the seam, so a 202 means the bet is durably on the topic
 *.
 */
@Service
@RequiredArgsConstructor
public class BetService {

    private final JackpotRepository jackpotRepository;
    private final BetEventPublisher publisher;

    public PublishBetResponse publish(PublishBetRequest request) {
        // Surplus decimals on the inbound amount are discarded to the money scale (A6).
        Bet bet = new Bet(request.betId(), request.userId(), request.jackpotId(),
                Money.truncate(request.betAmount()));

        if (!jackpotRepository.existsById(bet.jackpotId())) {
            throw new JackpotNotFoundException("No jackpot " + bet.jackpotId() + " for bet " + bet.betId());
        }

        publisher.publish(bet);
        return new PublishBetResponse(bet.betId(), "PUBLISHED");
    }
}
