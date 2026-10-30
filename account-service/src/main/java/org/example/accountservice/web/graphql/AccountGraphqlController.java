package org.example.accountservice.web.graphql;

import org.example.accountservice.service.AccountOperation;
import org.example.accountservice.service.AccountService;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.math.BigDecimal;
import java.util.UUID;

@Controller
public class AccountGraphqlController {

    private final AccountService accountService;

    public AccountGraphqlController(AccountService accountService) {
        this.accountService = accountService;
    }

    @QueryMapping
    public GraphqlAccount account(@Argument UUID id) {
        return GraphqlAccount.from(accountService.getAccount(id));
    }

    @MutationMapping
    public GraphqlAccount createAccount(@Argument String customerReference, @Argument String currency) {
        return GraphqlAccount.from(accountService.createAccount(customerReference, currency));
    }

    @MutationMapping
    public GraphqlAccount updateBalance(@Argument UUID id,
                                        @Argument String amount,
                                        @Argument String currency,
                                        @Argument AccountOperation operation) {
        try {
            return GraphqlAccount.from(
                    accountService.updateBalance(id, new BigDecimal(amount), currency, operation));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Amount must be a valid decimal number", exception);
        }
    }
}
