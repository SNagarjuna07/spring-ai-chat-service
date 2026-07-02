package com.nagarjuna.aichatservice.exception;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // LLM provider errors (API timeout, rate limit, invalid key)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleAll(Exception ex) {

        log.error("Unhandled error: {}", ex.getMessage(), ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorBody("AI service error", ex.getMessage()));
    }

    // @Valid on @RequestBody failures
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(
            MethodArgumentNotValidException ex) {

        Map<String, String> fieldErrors = new HashMap<>();

        ex.getBindingResult().getFieldErrors()
                .forEach(e -> fieldErrors.put(e.getField(), e.getDefaultMessage()));

        Map<String, Object> body = errorBody("Validation failed", "See 'errors' field");

        body.put("errors", fieldErrors);

        return ResponseEntity.badRequest().body(body);
    }

    // @Validated on @RequestParam failures
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraint(
            ConstraintViolationException ex) {

        return ResponseEntity.badRequest()
                .body(errorBody("Invalid request parameter", ex.getMessage()));
    }

    private Map<String, Object> errorBody(String error, String detail) {

        Map<String, Object> body = new HashMap<>();

        body.put("error", error);

        body.put("detail", detail);

        body.put("timestamp", Instant.now().toString());

        return body;
    }
}