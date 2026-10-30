package org.example.accountservice.web.graphql;

import org.example.accountservice.domain.Account;
import org.example.accountservice.domain.AccountStatus;

import java.util.UUID;

public record GraphqlAccount(
        UUID id,
        String customerReference,
        AccountStatus status,
        String currency,
        String balance) {

    public static GraphqlAccount from(Account account) {
        return new GraphqlAccount(
                account.getId(),
                account.getCustomerReference(),
                account.getStatus(),
                account.getCurrency(),
                account.getBalance().toPlainString());
    }
}
