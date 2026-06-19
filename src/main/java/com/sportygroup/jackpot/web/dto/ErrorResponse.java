package com.sportygroup.jackpot.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

/**
 * Uniform error response shape. {@code errorDetails} carries field-level violations and
 * is populated only for bean-validation failures; it is omitted otherwise.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        String error,
        String message,
        Instant timestamp,
        List<FieldErrorDetail> errorDetails) {

    public static ErrorResponse of(String error, String message) {
        return new ErrorResponse(error, message, Instant.now(), null);
    }

    public static ErrorResponse of(String error, String message, List<FieldErrorDetail> errorDetails) {
        return new ErrorResponse(error, message, Instant.now(), errorDetails);
    }

    public record FieldErrorDetail(String field, String message) {
    }
}
