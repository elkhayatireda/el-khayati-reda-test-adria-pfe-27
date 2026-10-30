package org.example.transactionservice.service;

import org.example.transactionservice.client.AccountClient;
import org.example.transactionservice.client.dto.AccountOperation;
import org.example.transactionservice.client.dto.AccountResponse;
import org.example.transactionservice.domain.Transaction;
import org.example.transactionservice.domain.TransactionType;
import org.example.transactionservice.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountClient accountClient;

    public TransactionService(TransactionRepository transactionRepository, AccountClient accountClient) {
        this.transactionRepository = transactionRepository;
        this.accountClient = accountClient;
    }

    @Transactional
    public Transaction processTransaction(UUID accountId, BigDecimal amount, String currency,
                                          String merchantCategory, String country) {
        validateAmount(amount);
        String normalizedCurrency = normalizeCurrency(currency);
        validateMetadata(merchantCategory, country);

        AccountResponse account = getActiveAccount(accountId);
        ensureCurrencyMatches(account, normalizedCurrency);
        ensureSufficientBalance(account, amount);

        Transaction transaction = transactionRepository.save(new Transaction(
                accountId, null, TransactionType.DEBIT, amount, normalizedCurrency, merchantCategory, country));
        accountClient.updateBalance(accountId, amount, normalizedCurrency, AccountOperation.DEBIT);
        return transaction;
    }

    @Transactional(readOnly = true)
    public List<Transaction> getTransactionsByAccountId(UUID accountId) {
        if (accountId == null) {
            throw new IllegalArgumentException("Account id must not be null");
        }
        return transactionRepository.findAllByAccountIdOrderByCreatedAtDesc(accountId);
    }

    @Transactional
    public Transaction transfer(UUID sourceAccountId, UUID destinationAccountId,
                                BigDecimal amount, String currency,
                                String merchantCategory, String country) {
        validateAmount(amount);
        String normalizedCurrency = normalizeCurrency(currency);
        validateMetadata(merchantCategory, country);
        if (sourceAccountId == null || destinationAccountId == null) {
            throw new IllegalArgumentException("Source and destination account ids must not be null");
        }
        if (sourceAccountId.equals(destinationAccountId)) {
            throw new IllegalArgumentException("Source and destination accounts must be different");
        }

        AccountResponse source = getActiveAccount(sourceAccountId);
        AccountResponse destination = getActiveAccount(destinationAccountId);
        ensureCurrencyMatches(source, normalizedCurrency);
        ensureCurrencyMatches(destination, normalizedCurrency);
        ensureSufficientBalance(source, amount);

        Transaction debitEntry = transactionRepository.save(new Transaction(
                sourceAccountId, destinationAccountId, TransactionType.DEBIT,
                amount, normalizedCurrency, merchantCategory, country));
        transactionRepository.save(new Transaction(
                destinationAccountId, sourceAccountId, TransactionType.CREDIT,
                amount, normalizedCurrency, merchantCategory, country));

        accountClient.updateBalance(sourceAccountId, amount, normalizedCurrency, AccountOperation.DEBIT);
        try {
            accountClient.updateBalance(destinationAccountId, amount, normalizedCurrency, AccountOperation.CREDIT);
        } catch (RuntimeException transferFailure) {
            try {
                accountClient.updateBalance(sourceAccountId, amount, normalizedCurrency, AccountOperation.CREDIT);
            } catch (RuntimeException compensationFailure) {
                transferFailure.addSuppressed(compensationFailure);
                throw new TransferCompensationException(
                        "Transfer failed and the source account could not be restored", transferFailure);
            }
            throw transferFailure;
        }

        return debitEntry;
    }

    private AccountResponse getActiveAccount(UUID accountId) {
        if (accountId == null) {
            throw new IllegalArgumentException("Account id must not be null");
        }
        AccountResponse account = accountClient.getAccount(accountId);
        if (account == null || account.id() == null || !accountId.equals(account.id())) {
            throw new IllegalStateException("Account service returned an invalid account");
        }
        if (!"ACTIVE".equalsIgnoreCase(account.status())) {
            throw new IllegalStateException("Account is not active: " + accountId);
        }
        return account;
    }

    private void ensureCurrencyMatches(AccountResponse account, String currency) {
        if (account.currency() == null || !currency.equalsIgnoreCase(account.currency())) {
            throw new IllegalArgumentException("Transaction currency must match account currency");
        }
    }

    private void ensureSufficientBalance(AccountResponse account, BigDecimal amount) {
        if (account.balance() == null || account.balance().compareTo(amount) < 0) {
            throw new IllegalStateException("Insufficient funds");
        }
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Transaction amount must be greater than zero");
        }
    }

    private void validateMetadata(String merchantCategory, String country) {
        if (merchantCategory == null || merchantCategory.isBlank()) {
            throw new IllegalArgumentException("Merchant category must not be blank");
        }
        if (country == null || !country.trim().matches("[A-Za-z]{2}")) {
            throw new IllegalArgumentException("Country must be a two-letter ISO code");
        }
    }

    private String normalizeCurrency(String currency) {
        if (currency == null || !currency.trim().matches("[A-Za-z]{3}")) {
            throw new IllegalArgumentException("Currency must be a three-letter ISO code");
        }
        return currency.trim().toUpperCase(Locale.ROOT);
    }
}
