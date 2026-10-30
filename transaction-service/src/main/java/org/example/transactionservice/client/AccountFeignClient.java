package org.example.transactionservice.client;

import org.example.transactionservice.client.dto.AccountResponse;
import org.example.transactionservice.client.dto.UpdateBalanceRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.UUID;

@FeignClient(name = "account-service", path = "/api/accounts")
public interface AccountFeignClient {

    @GetMapping("/{id}")
    AccountResponse getAccount(@PathVariable("id") UUID id);

    @PutMapping("/{id}/balance")
    AccountResponse updateBalance(@PathVariable("id") UUID id,
                                 @RequestBody UpdateBalanceRequest request);
}
