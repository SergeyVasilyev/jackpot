package com.sportygroup.jackpot.web.dto;

/** Response for an accepted bet placement (202): {@code { "betId": ..., "status": "PUBLISHED" }}. */
public record PublishBetResponse(String betId, String status) {
}
