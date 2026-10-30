package org.example.accountservice.mcp;

import org.example.accountservice.domain.Account;
import org.example.accountservice.service.AccountOperation;
import org.example.accountservice.service.AccountService;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class AccountMcpTools {

    private final AccountService accountService;

    public AccountMcpTools(AccountService accountService) {
        this.accountService = accountService;
    }

    @McpTool(name = "create_account", description = "Create a new active bank account with a zero balance")
    public Account createAccount(
            @McpToolParam(description = "Customer reference", required = true) String customerReference,
            @McpToolParam(description = "Three-letter ISO currency code", required = true) String currency) {
        return accountService.createAccount(customerReference, currency);
    }

    @McpTool(name = "get_account", description = "Retrieve an account and its current balance")
    public Account getAccount(
            @McpToolParam(description = "Account UUID", required = true) String id) {
        return accountService.getAccount(parseId(id));
    }

    @McpTool(name = "update_account_balance", description = "Credit or debit an active account")
    public Account updateBalance(
            @McpToolParam(description = "Account UUID", required = true) String id,
            @McpToolParam(description = "Positive amount not exceeding the configured limit", required = true)
            BigDecimal amount,
            @McpToolParam(description = "Three-letter ISO currency code matching the account", required = true)
            String currency,
            @McpToolParam(description = "CREDIT or DEBIT", required = true) AccountOperation operation) {
        return accountService.updateBalance(parseId(id), amount, currency, operation);
    }

    private UUID parseId(String id) {
        try {
            return UUID.fromString(id);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Account id must be a valid UUID", exception);
        }
    }
}
