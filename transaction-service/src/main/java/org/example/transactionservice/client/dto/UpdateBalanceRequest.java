package org.example.transactionservice.client.dto;

import java.math.BigDecimal;

public record UpdateBalanceRequest(
        BigDecimal amount,
        String currency,
        AccountOperation operation) {
}
