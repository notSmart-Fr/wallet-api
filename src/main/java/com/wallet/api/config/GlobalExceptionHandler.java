package com.wallet.api.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private String getOrCreateTraceId() {
        String traceId = MDC.get("traceId");
        return (traceId != null && !traceId.isBlank()) ? traceId : UUID.randomUUID().toString();
    }

    // Handles DTO @Valid constraints (e.g. @DecimalMin, @NotNull)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex) {
        String traceId = getOrCreateTraceId();
        String detailedMessage = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));

        log.warn("[Trace ID: {}] Validation failed: {}", traceId, detailedMessage);

        return ResponseEntity.badRequest().body(new ErrorResponse(
            traceId,
            HttpStatus.BAD_REQUEST.value(),
            "VALIDATION_ERROR",
            detailedMessage,
            OffsetDateTime.now()
        ));
    }

    // Handles domain/service assertion failures
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        String traceId = getOrCreateTraceId();
        log.warn("[Trace ID: {}] Bad request: {}", traceId, ex.getMessage());

        return ResponseEntity.badRequest().body(new ErrorResponse(
            traceId,
            HttpStatus.BAD_REQUEST.value(),
            "VALIDATION_ERROR",
            ex.getMessage(),
            OffsetDateTime.now()
        ));
    }

    // Re-throws Security exceptions so SecurityConfig handlers capture them
    @ExceptionHandler({AccessDeniedException.class, AuthenticationException.class})
    public void handleSecurityExceptions(Exception ex) throws Exception {
        throw ex;
    }

    // Catch-all fallback for unhandled 500 crashes
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        String traceId = getOrCreateTraceId();
        log.error("[Trace ID: {}] Unhandled exception caught:", traceId, ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponse(
            traceId,
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "INTERNAL_ERROR",
            "An unexpected error occurred.",
            OffsetDateTime.now()
        ));
    }
}