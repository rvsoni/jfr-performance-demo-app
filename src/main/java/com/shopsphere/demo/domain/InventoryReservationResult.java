package com.shopsphere.demo.domain;

import java.time.Instant;
import java.util.List;

public record InventoryReservationResult(
        boolean success,
        String orderId,
        String message,
        List<String> reservedProductIds,
        Instant timestamp
) {
    public InventoryReservationResult(boolean success, String orderId, String message, List<String> reservedProductIds) {
        this(success, orderId, message, reservedProductIds, Instant.now());
    }
}
