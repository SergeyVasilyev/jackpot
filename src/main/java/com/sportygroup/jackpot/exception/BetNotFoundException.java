package com.sportygroup.jackpot.exception;

/** Thrown when no processed (contributed) bet exists for an id (A4 → 404 on reward query). */
public class BetNotFoundException extends RuntimeException {
    public BetNotFoundException(String message) {
        super(message);
    }
}
