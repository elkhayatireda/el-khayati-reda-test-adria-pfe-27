package org.example.transactionservice.web.graphql;

import org.example.transactionservice.domain.Transaction;
import org.example.transactionservice.service.TransactionService;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Controller
public class TransactionGraphqlController {

    private final TransactionService transactionService;

    public TransactionGraphqlController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @QueryMapping
    public List<GraphqlTransaction> transactionsByAccount(@Argument UUID accountId) {
        return transactionService.getTransactionsByAccountId(accountId).stream()
                .map(GraphqlTransaction::from)
                .toList();
    }

    @MutationMapping
    public GraphqlTransaction createTransaction(@Argument UUID accountId,
                                                @Argument String amount,
                                                @Argument String currency,
                                                @Argument String merchantCategory,
                                                @Argument String country) {
        Transaction transaction = transactionService.processTransaction(
                accountId, parseAmount(amount), currency, merchantCategory, country);
        return GraphqlTransaction.from(transaction);
    }

    @MutationMapping
    public GraphqlTransaction transfer(@Argument UUID sourceAccountId,
                                       @Argument UUID destinationAccountId,
                                       @Argument String amount,
                                       @Argument String currency,
                                       @Argument String merchantCategory,
                                       @Argument String country) {
        Transaction transaction = transactionService.transfer(
                sourceAccountId, destinationAccountId, parseAmount(amount),
                currency, merchantCategory, country);
        return GraphqlTransaction.from(transaction);
    }

    private BigDecimal parseAmount(String amount) {
        try {
            return new BigDecimal(amount);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Amount must be a valid decimal number", exception);
        }
    }
}
