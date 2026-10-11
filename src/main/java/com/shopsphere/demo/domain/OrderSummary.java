package com.shopsphere.demo.domain;

import java.math.BigDecimal;
import java.time.Instant;

public record OrderSummary(
        String orderId,
        String customerId,
        int itemCount,
        BigDecimal totalAmount,
        String status,
        String summaryText,
        Instant generatedAt
) {
    public OrderSummary(String orderId, String customerId, int itemCount, BigDecimal totalAmount, String status, String summaryText) {
        this(orderId, customerId, itemCount, totalAmount, status, summaryText, Instant.now());
    }
}
