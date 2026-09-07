package com.wallet.api.features.accounts.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record DepositRequest(@NotNull BigDecimal amount) {
}