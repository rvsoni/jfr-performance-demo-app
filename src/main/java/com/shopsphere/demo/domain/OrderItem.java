package com.shopsphere.demo.domain;

import java.math.BigDecimal;

public record OrderItem(
        String productId,
        String productName,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal
) {
    public OrderItem(String productId, String productName, int quantity, BigDecimal unitPrice) {
        this(productId, productName, quantity, unitPrice, unitPrice.multiply(BigDecimal.valueOf(quantity)));
    }
}
