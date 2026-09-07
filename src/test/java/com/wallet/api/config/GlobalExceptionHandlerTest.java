package com.wallet.api.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler();

    @AfterEach
    void clearTraceId() {
        MDC.remove(TraceIdFilter.MDC_KEY);
    }

    @Test
    void validationErrorsUseTheUnifiedTraceableContract() {
        MDC.put(TraceIdFilter.MDC_KEY, "15aa9e9c-593c-44fe-bb98-eb3fbcaf134b");

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleIllegalArgument(
                new IllegalArgumentException("Amount must be positive"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).satisfies(error -> {
            assertThat(error.status()).isEqualTo(HttpStatus.BAD_REQUEST.value());
            assertThat(error.error()).isEqualTo("VALIDATION_ERROR");
            assertThat(error.message()).isEqualTo("Amount must be positive");
            assertThat(error.timestamp()).isNotNull();
            assertThat(error.traceId()).isEqualTo("15aa9e9c-593c-44fe-bb98-eb3fbcaf134b");
        });
    }

    @Test
    void unexpectedErrorsDoNotExposeInternalDetails() {
        MDC.put(TraceIdFilter.MDC_KEY, "15aa9e9c-593c-44fe-bb98-eb3fbcaf134b");

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleGeneric(
                new RuntimeException("database password=secret"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).satisfies(error -> {
            assertThat(error.error()).isEqualTo("INTERNAL_ERROR");
            assertThat(error.message()).isEqualTo("An unexpected error occurred.");
            assertThat(error.traceId()).isEqualTo("15aa9e9c-593c-44fe-bb98-eb3fbcaf134b");
        });
    }
}