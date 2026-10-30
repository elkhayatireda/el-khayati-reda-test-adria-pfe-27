package org.example.accountservice.service;

import org.example.accountservice.domain.Account;
import org.example.accountservice.repository.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    private AccountService accountService;
    private Account account;

    @BeforeEach
    void setUp() {
        AccountOperationProperties properties = new AccountOperationProperties();
        properties.setMaxAmount(new BigDecimal("1000.00"));
        accountService = new AccountService(accountRepository, properties);
        account = new Account("customer-123", "EUR");
    }

    @Test
    void createsAccountWithZeroBalance() {
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Account created = accountService.createAccount("customer-123", "eur");

        assertEquals("EUR", created.getCurrency());
        assertEquals(BigDecimal.ZERO, created.getBalance());
    }

    @Test
    void creditsActiveAccountAndPersistsUpdatedBalance() {
        when(accountRepository.findByIdForUpdate(account.getId())).thenReturn(Optional.of(account));
        when(accountRepository.save(account)).thenReturn(account);

        Account updated = accountService.updateBalance(
                account.getId(), new BigDecimal("25.50"), "EUR", AccountOperation.CREDIT);

        assertEquals(new BigDecimal("25.5"), updated.getBalance());
        verify(accountRepository).save(account);
    }

    @Test
    void rejectsOperationInDifferentCurrency() {
        when(accountRepository.findByIdForUpdate(account.getId())).thenReturn(Optional.of(account));

        assertThrows(IllegalArgumentException.class, () -> accountService.updateBalance(
                account.getId(), BigDecimal.ONE, "USD", AccountOperation.CREDIT));
    }

    @Test
    void rejectsAmountAboveConfiguredLimit() {
        when(accountRepository.findByIdForUpdate(account.getId())).thenReturn(Optional.of(account));

        assertThrows(IllegalArgumentException.class, () -> accountService.updateBalance(
                account.getId(), new BigDecimal("1000.01"), "EUR", AccountOperation.CREDIT));
    }

    @Test
    void rejectsDebitThatWouldOverdrawAccount() {
        when(accountRepository.findByIdForUpdate(account.getId())).thenReturn(Optional.of(account));

        assertThrows(IllegalStateException.class, () -> accountService.updateBalance(
                account.getId(), BigDecimal.ONE, "EUR", AccountOperation.DEBIT));
    }
}
