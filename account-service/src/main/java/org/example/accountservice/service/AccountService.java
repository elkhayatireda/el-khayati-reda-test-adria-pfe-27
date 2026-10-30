package org.example.accountservice.service;

import org.example.accountservice.domain.Account;
import org.example.accountservice.domain.AccountStatus;
import org.example.accountservice.repository.AccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.UUID;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final AccountOperationProperties operationProperties;

    public AccountService(AccountRepository accountRepository,
                          AccountOperationProperties operationProperties) {
        this.accountRepository = accountRepository;
        this.operationProperties = operationProperties;
    }

    @Transactional
    public Account createAccount(String customerReference, String currency) {
        if (customerReference == null || customerReference.isBlank()) {
            throw new IllegalArgumentException("Customer reference must not be blank");
        }

        String normalizedCurrency = normalizeCurrency(currency);
        return accountRepository.save(new Account(customerReference.trim(), normalizedCurrency));
    }

    @Transactional(readOnly = true)
    public Account getAccount(UUID id) {
        if (id == null) {
            throw new IllegalArgumentException("Account id must not be null");
        }
        Account account = accountRepository.findById(id).orElseThrow(() -> new AccountNotFoundException(id));
        ensureActive(account);
        return account;
    }

    @Transactional
    public Account updateBalance(UUID id, BigDecimal amount, String currency, AccountOperation operation) {
        if (id == null) {
            throw new IllegalArgumentException("Account id must not be null");
        }

        Account account = accountRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new AccountNotFoundException(id));
        ensureActive(account);
        validateAmount(amount);
        if (operation == null) {
            throw new IllegalArgumentException("Operation must not be null");
        }
        if (!account.getCurrency().equals(normalizeCurrency(currency))) {
            throw new IllegalArgumentException("Operation currency must match account currency");
        }

        BigDecimal normalizedAmount = amount.stripTrailingZeros();
        BigDecimal updatedBalance = operation == AccountOperation.CREDIT
                ? account.getBalance().add(normalizedAmount)
                : account.getBalance().subtract(normalizedAmount);
        if (updatedBalance.signum() < 0) {
            throw new IllegalStateException("Insufficient funds");
        }
        BigDecimal normalizedBalance = updatedBalance.stripTrailingZeros();
        if (normalizedBalance.scale() > 4
                || normalizedBalance.precision() - normalizedBalance.scale() > 15) {
            throw new IllegalArgumentException("Updated balance exceeds supported precision");
        }

        account.setBalance(normalizedBalance);
        return accountRepository.save(account);
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
        if (amount.stripTrailingZeros().scale() > 4) {
            throw new IllegalArgumentException("Amount supports at most four decimal places");
        }
        BigDecimal maxAmount = operationProperties.getMaxAmount();
        if (maxAmount == null || maxAmount.signum() <= 0) {
            throw new IllegalStateException("Configured maximum operation amount must be greater than zero");
        }
        if (amount.compareTo(maxAmount) > 0) {
            throw new IllegalArgumentException("Amount exceeds maximum operation amount of " + maxAmount);
        }
    }

    private String normalizeCurrency(String currency) {
        if (currency == null || !currency.trim().matches("[A-Za-z]{3}")) {
            throw new IllegalArgumentException("Currency must be a three-letter ISO code");
        }
        return currency.trim().toUpperCase(Locale.ROOT);
    }

    private void ensureActive(Account account) {
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new IllegalStateException("Account is not active");
        }
    }
}
