package com.wallet.api.features.accounts;

import com.wallet.api.features.transactions.Transaction;
import com.wallet.api.features.transactions.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public AccountService(AccountRepository accountRepository, TransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
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
        if (amount.scale() > 4) {
            throw new IllegalArgumentException("Deposit amount supports at most 4 decimal places.");
        }

        Account account = accountRepository.findByUserId(userId)
            .orElseGet(() -> accountRepository.save(new Account(userId, BigDecimal.ZERO)));

        account.setBalance(account.getBalance().add(amount));
        Account updatedAccount = accountRepository.save(account);
        transactionRepository.save(new Transaction(
            null,
            updatedAccount,
            amount,
            Transaction.TransactionType.DEPOSIT,
            Transaction.TransactionStatus.SUCCESS
        ));
        return updatedAccount;
    }
}