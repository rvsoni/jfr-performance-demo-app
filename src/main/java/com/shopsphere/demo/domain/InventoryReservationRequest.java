package com.shopsphere.demo.domain;

import java.util.List;

public record InventoryReservationRequest(
        String orderId,
        List<OrderItem> items
) {
}
