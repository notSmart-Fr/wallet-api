package com.wallet.api.features.accounts;

import com.wallet.api.features.accounts.dto.AccountResponse;
import com.wallet.api.features.accounts.dto.DepositRequest;
import com.wallet.api.features.accounts.dto.DepositResponse;
import com.wallet.api.features.transactions.Transaction;
import com.wallet.api.features.transactions.TransactionRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {

    private final AccountService accountService;
    private final TransactionRepository transactionRepository;

    public AccountController(AccountService accountService, TransactionRepository transactionRepository) {
        this.accountService = accountService;
        this.transactionRepository = transactionRepository;
    }

    @GetMapping("/me")
    public ResponseEntity<AccountResponse> getMyAccount(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        Account account = accountService.getOrCreateAccount(userId);
        return ResponseEntity.ok(new AccountResponse(
                account.getId(),
                account.getBalance(),
                account.getCreatedAt()
        ));
    }

        @PostMapping("/deposits")
        public ResponseEntity<DepositResponse> deposit(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody DepositRequest request) {
        UUID userId = UUID.fromString(jwt.getSubject());
        Account updatedAccount = accountService.deposit(userId, request.amount());
        Transaction deposit = transactionRepository.findTopByRecipientAccountIdAndTypeOrderByCreatedAtDesc(
                updatedAccount.getId(), Transaction.TransactionType.DEPOSIT)
            .orElseThrow(() -> new IllegalStateException("Deposit transaction was not recorded."));
        return ResponseEntity.status(HttpStatus.CREATED).body(new DepositResponse(
            deposit.getId(),
            updatedAccount.getId(),
            updatedAccount.getBalance(),
            deposit.getStatus().name(),
            deposit.getCreatedAt()
        ));
    }
}