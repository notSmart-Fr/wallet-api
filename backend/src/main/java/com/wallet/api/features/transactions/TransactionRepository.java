package com.wallet.api.features.transactions;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    @Query("SELECT t FROM Transaction t WHERE t.senderAccount.id = :accountId OR t.recipientAccount.id = :accountId ORDER BY t.createdAt DESC")
    Page<Transaction> findAllByAccountId(UUID accountId, Pageable pageable);

    Optional<Transaction> findTopByRecipientAccountIdAndTypeOrderByCreatedAtDesc(
            UUID accountId, Transaction.TransactionType type);
}