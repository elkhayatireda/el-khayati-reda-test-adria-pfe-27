package org.example.transactionservice.client.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record AccountResponse(
        UUID id,
        String customerReference,
        String status,
        String currency,
        BigDecimal balance) {
}
