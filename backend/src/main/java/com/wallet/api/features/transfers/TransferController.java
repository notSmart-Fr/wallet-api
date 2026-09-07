package com.wallet.api.features.transfers;

import com.wallet.api.features.transfers.dto.TransferRequest;
import com.wallet.api.features.transactions.Transaction;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/transfers")
public class TransferController {

    private final TransferService transferService;

    public TransferController(TransferService transferService) {
        this.transferService = transferService;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> transfer(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody TransferRequest request) {
        UUID senderUserId = UUID.fromString(jwt.getSubject());
        Transaction tx = transferService.executeTransfer(senderUserId, request.recipientAccountId(), request.amount());

        return ResponseEntity.ok(Map.of(
                "transactionId", tx.getId(),
                "status", tx.getStatus(),
                "amount", tx.getAmount(),
                "timestamp", tx.getCreatedAt()
        ));
    }
}