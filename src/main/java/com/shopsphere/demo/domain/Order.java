package com.shopsphere.demo.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record Order(
        String id,
        String customerId,
        String membershipTier,
        List<OrderItem> items,
        PriceBreakdown priceBreakdown,
        String status,
        Instant createdAt
) {
    public Order(String id, String customerId, String membershipTier, List<OrderItem> items, PriceBreakdown priceBreakdown, String status) {
        this(id, customerId, membershipTier, items, priceBreakdown, status, Instant.now());
    }
}
