package org.example.accountservice.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

@ConfigurationProperties(prefix = "account.operations")
public class AccountOperationProperties {

    private BigDecimal maxAmount = new BigDecimal("10000.00");

    public BigDecimal getMaxAmount() {
        return maxAmount;
    }

    public void setMaxAmount(BigDecimal maxAmount) {
        this.maxAmount = maxAmount;
    }
}
