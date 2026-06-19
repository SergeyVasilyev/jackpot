package com.sportygroup.jackpot.web;

import com.sportygroup.jackpot.exception.BetNotFoundException;
import com.sportygroup.jackpot.exception.InvalidJackpotConfigException;
import com.sportygroup.jackpot.exception.JackpotNotFoundException;
import com.sportygroup.jackpot.web.dto.ErrorResponse;
import com.sportygroup.jackpot.web.dto.ErrorResponse.FieldErrorDetail;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

/** Maps exceptions to the uniform error response shape. */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        List<FieldErrorDetail> details = ex.getBindingResult().getFieldErrors().stream()
                .map(this::toDetail)
                .toList();
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of("VALIDATION_ERROR", "Request validation failed", details));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadable(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of("VALIDATION_ERROR", "Malformed request body"));
    }

    @ExceptionHandler(JackpotNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleJackpotNotFound(JackpotNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT)
                .body(ErrorResponse.of("JACKPOT_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(BetNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleBetNotFound(BetNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of("BET_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(InvalidJackpotConfigException.class)
    public ResponseEntity<ErrorResponse> handleInvalidConfig(InvalidJackpotConfigException ex) {
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of("INVALID_JACKPOT_CONFIG", ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of("INTERNAL_ERROR", "Unexpected error"));
    }

    private FieldErrorDetail toDetail(FieldError error) {
        return new FieldErrorDetail(error.getField(), error.getDefaultMessage());
    }
}
