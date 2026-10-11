package com.shopsphere.demo.domain;

import java.math.BigDecimal;

public record Product(
        String id,
        String name,
        String category,
        BigDecimal basePrice,
        int stockQuantity
) {
}
