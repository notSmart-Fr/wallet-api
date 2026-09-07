package com.wallet.api.features.accounts;

import com.wallet.api.features.transactions.Transaction;
import com.wallet.api.features.transactions.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountControllerIntegrationTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private AccountService accountService;

    @Test
    void depositUpdatesTheBalanceAndCreatesAnAuditRecord() {
        UUID userId = UUID.randomUUID();
        Account account = new Account(userId, new BigDecimal("10.0000"));
        when(accountRepository.findByUserId(userId)).thenReturn(Optional.of(account));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Account updatedAccount = accountService.deposit(userId, new BigDecimal("5.0000"));

        ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(transactionCaptor.capture());
        assertThat(updatedAccount.getBalance()).isEqualByComparingTo("15.0000");
        assertThat(transactionCaptor.getValue().getSenderAccount()).isNull();
        assertThat(transactionCaptor.getValue().getRecipientAccount()).isSameAs(account);
        assertThat(transactionCaptor.getValue().getAmount()).isEqualByComparingTo("5.0000");
        assertThat(transactionCaptor.getValue().getType()).isEqualTo(Transaction.TransactionType.DEPOSIT);
        assertThat(transactionCaptor.getValue().getStatus()).isEqualTo(Transaction.TransactionStatus.SUCCESS);
    }

    @Test
    void depositRejectsAmountsWithMoreThanFourDecimalPlaces() {
        assertThatThrownBy(() -> accountService.deposit(UUID.randomUUID(), new BigDecimal("1.00001")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Deposit amount supports at most 4 decimal places.");
    }
}