package com.sportygroup.jackpot.exception;

/**
 * Signals a redelivered / duplicate bet: the unique {@code bet_id} constraint was violated on the
 * contribution insert (A5). Translated from {@code DataIntegrityViolationException} and caught at
 * the consumer's transaction boundary as a skip + ack.
 */
public class DuplicateBetException extends RuntimeException {
    public DuplicateBetException(String betId, Throwable cause) {
        super("Duplicate bet " + betId, cause);
    }
}
