package org.example.transactionservice.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import feign.FeignException;
import org.example.transactionservice.client.dto.AccountResponse;
import org.example.transactionservice.client.dto.UpdateBalanceRequest;
import org.example.transactionservice.client.dto.AccountOperation;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class AccountClient {

    private final AccountFeignClient accountFeignClient;

    AccountClient(AccountFeignClient accountFeignClient) {
        this.accountFeignClient = accountFeignClient;
    }

    @CircuitBreaker(name = "accountService", fallbackMethod = "getAccountFallback")
    public AccountResponse getAccount(UUID id) {
        return accountFeignClient.getAccount(id);
    }

    @CircuitBreaker(name = "accountService", fallbackMethod = "updateBalanceFallback")
    public AccountResponse updateBalance(UUID id, BigDecimal amount, String currency,
                                         AccountOperation operation) {
        return accountFeignClient.updateBalance(
                id, new UpdateBalanceRequest(amount, currency, operation));
    }

    private AccountResponse getAccountFallback(UUID id, Throwable cause) {
        rethrowClientError(cause);
        throw new AccountServiceUnavailableException("Account service is unavailable", cause);
    }

    private AccountResponse updateBalanceFallback(UUID id, BigDecimal amount, String currency,
                                                  AccountOperation operation, Throwable cause) {
        rethrowClientError(cause);
        throw new AccountServiceUnavailableException("Account service is unavailable", cause);
    }

    private void rethrowClientError(Throwable cause) {
        if (cause instanceof FeignException exception
                && exception.status() >= 400 && exception.status() < 500) {
            throw exception;
        }
    }
}
