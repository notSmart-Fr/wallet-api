package com.wallet.api.features.accounts;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class AccountService {

    private final AccountRepository accountRepository;

    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Transactional
    public Account getOrCreateAccount(UUID userId) {
        return accountRepository.findByUserId(userId)
                .orElseGet(() -> accountRepository.save(new Account(userId, BigDecimal.ZERO)));
    }

    @Transactional
    public Account deposit(UUID userId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Deposit amount must be strictly greater than zero.");
        }

        Account account = accountRepository.findByUserId(userId)
                .orElseGet(() -> new Account(userId, BigDecimal.ZERO));

        account.setBalance(account.getBalance().add(amount));
        return accountRepository.save(account);
    }
}