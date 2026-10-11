package com.shopsphere.demo.domain;

import java.util.List;

public record PricingRequest(
        String customerId,
        String membershipTier,
        List<OrderItem> items,
        String promoCode,
        String shippingRegion
) {
}
