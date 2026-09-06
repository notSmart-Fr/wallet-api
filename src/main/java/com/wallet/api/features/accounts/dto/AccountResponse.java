package com.wallet.api.features.accounts.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record AccountResponse(
    UUID accountId,
    UUID userId,
    BigDecimal balance,
    OffsetDateTime createdAt
) {}