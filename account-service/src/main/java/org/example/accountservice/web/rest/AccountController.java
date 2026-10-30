package org.example.accountservice.web.rest;

import org.example.accountservice.domain.Account;
import org.example.accountservice.service.AccountOperation;
import org.example.accountservice.service.AccountService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Account createAccount(@RequestBody CreateAccountRequest request) {
        return accountService.createAccount(request.customerReference(), request.currency());
    }

    @GetMapping("/{id}")
    public Account getAccount(@PathVariable UUID id) {
        return accountService.getAccount(id);
    }

    @PutMapping("/{id}/balance")
    public Account updateBalance(@PathVariable UUID id, @RequestBody UpdateBalanceRequest request) {
        return accountService.updateBalance(id, request.amount(), request.currency(), request.operation());
    }

    public record CreateAccountRequest(String customerReference, String currency) {
    }

    public record UpdateBalanceRequest(BigDecimal amount, String currency, AccountOperation operation) {
    }
}
