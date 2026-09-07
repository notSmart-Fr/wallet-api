package com.wallet.api.config;

import java.time.OffsetDateTime;

public record ErrorResponse(
    String traceId,
    int status,
    String error,
    String message,
    OffsetDateTime timestamp
) {}