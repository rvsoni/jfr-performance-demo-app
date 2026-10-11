package com.shopsphere.demo.domain;

import java.math.BigDecimal;
import java.util.List;

public record PriceBreakdown(
        BigDecimal subtotal,
        BigDecimal tax,
        BigDecimal shipping,
        BigDecimal discount,
        BigDecimal total,
        List<String> appliedRules
) {
}
