package com.sportygroup.jackpot.exception;

/** Thrown by {@code JackpotService.create} when a jackpot's strategy parameters are invalid (A1, A14, A15). */
public class InvalidJackpotConfigException extends RuntimeException {
    public InvalidJackpotConfigException(String message) {
        super(message);
    }
}
