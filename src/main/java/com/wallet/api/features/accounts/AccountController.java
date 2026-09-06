package com.wallet.api.features.accounts;

import com.wallet.api.features.accounts.dto.AccountResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/me")
    public ResponseEntity<AccountResponse> getMyAccount(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        Account account = accountService.getOrCreateAccount(userId);
        return ResponseEntity.ok(new AccountResponse(
                account.getId(),
                account.getUserId(),
                account.getBalance(),
                account.getCreatedAt()
        ));
    }

    @PostMapping("/deposit")
    public ResponseEntity<AccountResponse> deposit(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody Map<String, BigDecimal> request) {
        UUID userId = UUID.fromString(jwt.getSubject());
        BigDecimal amount = request.get("amount");
        Account updatedAccount = accountService.deposit(userId, amount);
        return ResponseEntity.ok(new AccountResponse(
                updatedAccount.getId(),
                updatedAccount.getUserId(),
                updatedAccount.getBalance(),
                updatedAccount.getCreatedAt()
        ));
    }
}