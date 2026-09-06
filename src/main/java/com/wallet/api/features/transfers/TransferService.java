package com.wallet.api.features.transfers;

import com.wallet.api.features.accounts.Account;
import com.wallet.api.features.accounts.AccountRepository;
import com.wallet.api.features.transactions.Transaction;
import com.wallet.api.features.transactions.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class TransferService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public TransferService(AccountRepository accountRepository, TransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public Transaction executeTransfer(UUID senderUserId, UUID recipientAccountId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Transfer amount must be strictly greater than zero.");
        }

        // 1. Fetch sender account by authenticated user ID
        Account sender = accountRepository.findByUserId(senderUserId)
                .orElseThrow(() -> new IllegalArgumentException("Sender account not found."));

        if (sender.getId().equals(recipientAccountId)) {
            throw new IllegalArgumentException("Cannot transfer funds to your own account.");
        }

        // 2. Deterministic Lock Acquisition Ordering to prevent Deadlocks (Sort by UUID)
        UUID firstLockId = sender.getId().compareTo(recipientAccountId) < 0 ? sender.getId() : recipientAccountId;
        UUID secondLockId = sender.getId().compareTo(recipientAccountId) < 0 ? recipientAccountId : sender.getId();

        Account firstAccount = accountRepository.findByIdForUpdate(firstLockId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + firstLockId));
        Account secondAccount = accountRepository.findByIdForUpdate(secondLockId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + secondLockId));

        Account lockedSender = sender.getId().equals(firstLockId) ? firstAccount : secondAccount;
        Account lockedRecipient = sender.getId().equals(firstLockId) ? secondAccount : firstAccount;

        // 3. Balance verification
        if (lockedSender.getBalance().compareTo(amount) < 0) {
            throw new IllegalStateException("Insufficient funds for transfer.");
        }

        // 4. Update balances using high-precision BigDecimal arithmetic
        lockedSender.setBalance(lockedSender.getBalance().subtract(amount));
        lockedRecipient.setBalance(lockedRecipient.getBalance().add(amount));

        accountRepository.save(lockedSender);
        accountRepository.save(lockedRecipient);

        // 5. Immutably record audit transaction
        Transaction transaction = new Transaction(
                lockedSender,
                lockedRecipient,
                amount,
                Transaction.TransactionType.TRANSFER,
                Transaction.TransactionStatus.SUCCESS
        );

        return transactionRepository.save(transaction);
    }
}