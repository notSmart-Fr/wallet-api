package com.wallet.api.features.transactions;

import com.wallet.api.features.accounts.Account;
import com.wallet.api.features.accounts.AccountService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {

    private final TransactionRepository transactionRepository;
    private final AccountService accountService;

    public TransactionController(TransactionRepository transactionRepository, AccountService accountService) {
        this.transactionRepository = transactionRepository;
        this.accountService = accountService;
    }

    @GetMapping
    public ResponseEntity<Page<TransactionDto>> getHistory(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        UUID userId = UUID.fromString(jwt.getSubject());
        Account account = accountService.getOrCreateAccount(userId);

        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        Page<Transaction> transactions = transactionRepository.findAllByAccountId(account.getId(), pageable);

        Page<TransactionDto> dtos = transactions.map(tx -> new TransactionDto(
                tx.getId(),
                tx.getSenderAccount() != null ? tx.getSenderAccount().getId() : null,
                tx.getRecipientAccount().getId(),
                tx.getAmount(),
                tx.getType().name(),
                tx.getStatus().name(),
                tx.getCreatedAt()
        ));

        return ResponseEntity.ok(dtos);
    }

    public record TransactionDto(
            UUID id,
            UUID senderAccountId,
            UUID recipientAccountId,
            java.math.BigDecimal amount,
            String type,
            String status,
            java.time.OffsetDateTime createdAt
    ) {}
}