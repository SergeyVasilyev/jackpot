package com.sportygroup.jackpot.web;

import com.sportygroup.jackpot.service.BetService;
import com.sportygroup.jackpot.web.dto.PublishBetRequest;
import com.sportygroup.jackpot.web.dto.PublishBetResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Publish a bet to Kafka (FR1). */
@RestController
@RequestMapping("/api/v1/bets")
@RequiredArgsConstructor
public class BetController {

    private final BetService betService;

    @PostMapping
    public ResponseEntity<PublishBetResponse> publish(@Valid @RequestBody PublishBetRequest request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(betService.publish(request));
    }
}
