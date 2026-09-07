package com.wallet.api.features.accounts.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record DepositResponse(
        UUID transactionId,
        UUID accountId,
        BigDecimal newBalance,
        String status,
        OffsetDateTime createdAt
) {
}