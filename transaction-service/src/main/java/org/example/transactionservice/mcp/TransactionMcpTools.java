package org.example.transactionservice.mcp;

import org.example.transactionservice.domain.Transaction;
import org.example.transactionservice.service.TransactionService;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class TransactionMcpTools {

    private final TransactionService transactionService;

    public TransactionMcpTools(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @McpTool(name = "list_account_transactions",
            description = "List transactions for an account, newest first",
            generateOutputSchema = true)
    public List<Transaction> listAccountTransactions(
            @McpToolParam(description = "Account UUID", required = true) String accountId) {
        try {
            return transactionService.getTransactionsByAccountId(UUID.fromString(accountId));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Account id must be a valid UUID", exception);
        }
    }
}
