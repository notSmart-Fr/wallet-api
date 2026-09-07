package com.wallet.api.features.accounts.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record AccountResponse(
    UUID id,
    BigDecimal balance,
    OffsetDateTime createdAt
) {}