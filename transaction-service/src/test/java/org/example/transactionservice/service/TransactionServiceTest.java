package org.example.transactionservice.service;

import org.example.transactionservice.client.AccountClient;
import org.example.transactionservice.client.dto.AccountOperation;
import org.example.transactionservice.client.dto.AccountResponse;
import org.example.transactionservice.domain.Transaction;
import org.example.transactionservice.domain.TransactionType;
import org.example.transactionservice.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountClient accountClient;

    private TransactionService transactionService;
    private UUID accountId;
    private UUID destinationId;

    @BeforeEach
    void setUp() {
        transactionService = new TransactionService(transactionRepository, accountClient);
        accountId = UUID.randomUUID();
        destinationId = UUID.randomUUID();
    }

    @Test
    void debitsActiveAccountAndSavesTransaction() {
        when(accountClient.getAccount(accountId)).thenReturn(account(accountId, "ACTIVE", "EUR", "100.00"));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Transaction transaction = transactionService.processTransaction(
                accountId, new BigDecimal("25.00"), "eur", "GROCERY", "fr");

        assertEquals(accountId, transaction.getAccountId());
        assertEquals(TransactionType.DEBIT, transaction.getType());
        assertEquals("EUR", transaction.getCurrency());
        verify(accountClient).updateBalance(accountId, new BigDecimal("25.00"), "EUR", AccountOperation.DEBIT);
    }

    @Test
    void rejectsNonPositiveAmountBeforeCallingAccountService() {
        assertThrows(IllegalArgumentException.class, () -> transactionService.processTransaction(
                accountId, BigDecimal.ZERO, "EUR", "GROCERY", "FR"));

        verify(accountClient, never()).getAccount(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void rejectsBlockedAccount() {
        when(accountClient.getAccount(accountId)).thenReturn(account(accountId, "BLOCKED", "EUR", "100.00"));

        assertThrows(IllegalStateException.class, () -> transactionService.processTransaction(
                accountId, BigDecimal.ONE, "EUR", "GROCERY", "FR"));

        verify(transactionRepository, never()).save(any());
        verify(accountClient, never()).updateBalance(any(), any(), any(), any());
    }

    @Test
    void rejectsMismatchedCurrency() {
        when(accountClient.getAccount(accountId)).thenReturn(account(accountId, "ACTIVE", "USD", "100.00"));

        assertThrows(IllegalArgumentException.class, () -> transactionService.processTransaction(
                accountId, BigDecimal.ONE, "EUR", "GROCERY", "FR"));

        verify(transactionRepository, never()).save(any());
    }

    @Test
    void transferDebitsSourceAndCreditsDestination() {
        when(accountClient.getAccount(accountId)).thenReturn(account(accountId, "ACTIVE", "EUR", "100.00"));
        when(accountClient.getAccount(destinationId)).thenReturn(account(destinationId, "ACTIVE", "EUR", "10.00"));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        transactionService.transfer(accountId, destinationId, new BigDecimal("25.00"),
                "EUR", "TRANSFER", "FR");

        verify(accountClient).updateBalance(accountId, new BigDecimal("25.00"), "EUR", AccountOperation.DEBIT);
        verify(accountClient).updateBalance(destinationId, new BigDecimal("25.00"), "EUR", AccountOperation.CREDIT);
        verify(transactionRepository, times(2)).save(any(Transaction.class));
    }

    @Test
    void restoresSourceIfDestinationCreditFails() {
        when(accountClient.getAccount(accountId)).thenReturn(account(accountId, "ACTIVE", "EUR", "100.00"));
        when(accountClient.getAccount(destinationId)).thenReturn(account(destinationId, "ACTIVE", "EUR", "10.00"));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doAnswer(invocation -> {
            UUID id = invocation.getArgument(0);
            AccountOperation operation = invocation.getArgument(3);
            if (destinationId.equals(id) && operation == AccountOperation.CREDIT) {
                throw new IllegalStateException("Destination unavailable");
            }
            return null;
        }).when(accountClient).updateBalance(any(), any(), any(), any());

        assertThrows(IllegalStateException.class, () -> transactionService.transfer(
                accountId, destinationId, new BigDecimal("25.00"), "EUR", "TRANSFER", "FR"));

        verify(accountClient).updateBalance(accountId, new BigDecimal("25.00"), "EUR", AccountOperation.DEBIT);
        verify(accountClient).updateBalance(accountId, new BigDecimal("25.00"), "EUR", AccountOperation.CREDIT);
    }

    private AccountResponse account(UUID id, String status, String currency, String balance) {
        return new AccountResponse(id, "customer-1", status, currency, new BigDecimal(balance));
    }
}
