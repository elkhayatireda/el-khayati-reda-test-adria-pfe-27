package org.example.transactionservice.web.rest;

import org.example.transactionservice.domain.Transaction;
import org.example.transactionservice.service.TransactionService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/transactions")
public class TransactionRestController {

    private final TransactionService transactionService;

    public TransactionRestController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Transaction createTransaction(@RequestBody CreateTransactionRequest request) {
        return transactionService.processTransaction(
                request.accountId(), request.amount(), request.currency(),
                request.merchantCategory(), request.country());
    }

    @GetMapping("/account/{accountId}")
    public List<Transaction> getTransactionsByAccount(@PathVariable UUID accountId) {
        return transactionService.getTransactionsByAccountId(accountId);
    }

    @PostMapping("/transfer")
    @ResponseStatus(HttpStatus.CREATED)
    public Transaction transfer(@RequestBody TransferRequest request) {
        return transactionService.transfer(
                request.sourceAccountId(), request.destinationAccountId(), request.amount(),
                request.currency(), request.merchantCategory(), request.country());
    }

    public record CreateTransactionRequest(
            UUID accountId,
            BigDecimal amount,
            String currency,
            String merchantCategory,
            String country) {
    }

    public record TransferRequest(
            UUID sourceAccountId,
            UUID destinationAccountId,
            BigDecimal amount,
            String currency,
            String merchantCategory,
            String country) {
    }
}
