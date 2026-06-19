package com.sportygroup.jackpot.web;

import com.sportygroup.jackpot.service.RewardService;
import com.sportygroup.jackpot.web.dto.RewardResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Read a bet's stored win/loss outcome (FR5, read-only — A2). */
@RestController
@RequestMapping("/api/v1/bets")
@RequiredArgsConstructor
public class RewardController {

    private final RewardService rewardService;

    @GetMapping("/{betId}/reward")
    public RewardResponse reward(@PathVariable String betId) {
        return rewardService.getOutcome(betId);
    }
}
