package org.example.transactionservice.web.graphql;

import org.example.transactionservice.domain.Transaction;
import org.example.transactionservice.domain.TransactionType;

import java.util.UUID;

public record GraphqlTransaction(
        UUID id,
        UUID accountId,
        UUID relatedAccountId,
        TransactionType type,
        String amount,
        String currency,
        String merchantCategory,
        String country,
        String createdAt) {

    public static GraphqlTransaction from(Transaction transaction) {
        return new GraphqlTransaction(
                transaction.getId(),
                transaction.getAccountId(),
                transaction.getRelatedAccountId(),
                transaction.getType(),
                transaction.getAmount().toPlainString(),
                transaction.getCurrency(),
                transaction.getMerchantCategory(),
                transaction.getCountry(),
                transaction.getCreatedAt().toString());
    }
}
