package com.sportygroup.jackpot.exception;

/** Thrown when a bet references a jackpot id that does not exist (A3 → 422 on publish). */
public class JackpotNotFoundException extends RuntimeException {
    public JackpotNotFoundException(String message) {
        super(message);
    }
}
