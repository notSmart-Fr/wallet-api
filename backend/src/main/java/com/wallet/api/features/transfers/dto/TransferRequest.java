package com.wallet.api.features.transfers.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record TransferRequest(
    @NotNull(message = "Recipient account ID is required") 
    UUID recipientAccountId,

    @NotNull(message = "Transfer amount is required") 
    @DecimalMin(value = "0.01", message = "Transfer amount must be greater than zero") 
    BigDecimal amount
) {}