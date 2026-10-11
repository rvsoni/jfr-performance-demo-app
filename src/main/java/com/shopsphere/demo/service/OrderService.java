package com.shopsphere.demo.service;

import com.shopsphere.demo.domain.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class OrderService {

    private final Map<String, Order> orderStore = new ConcurrentHashMap<>();
    private final AtomicLong orderCounter = new AtomicLong(1000);
    private final PricingService pricingService;
    private final InventoryService inventoryService;

    public OrderService(PricingService pricingService, InventoryService inventoryService) {
        this.pricingService = pricingService;
        this.inventoryService = inventoryService;
        initializeSampleOrders();
    }

    private void initializeSampleOrders() {
        for (int i = 1; i <= 20; i++) {
            String orderId = "ORD-" + orderCounter.incrementAndGet();
            List<OrderItem> items = List.of(
                    new OrderItem("PROD-001", "Electronics Item 1", 2, BigDecimal.valueOf(13.50)),
                    new OrderItem("PROD-002", "Apparel Item 2", 1, BigDecimal.valueOf(17.00))
            );
            PricingRequest req = new PricingRequest("CUST-" + (100 + i), "GOLD", items, "SAVE20", "US-EAST");
            PriceBreakdown breakdown = pricingService.calculatePrice(req);
            Order order = new Order(orderId, req.customerId(), req.membershipTier(), items, breakdown, "COMPLETED");
            orderStore.put(orderId, order);
        }
    }

    public Order createOrder(PricingRequest request) {
        String orderId = "ORD-" + orderCounter.incrementAndGet();
        PriceBreakdown breakdown = pricingService.calculatePrice(request);

        // Reserve inventory
        InventoryReservationRequest resReq = new InventoryReservationRequest(orderId, request.items());
        InventoryReservationResult resResult = inventoryService.reserve(resReq);

        String status = resResult.success() ? "CONFIRMED" : "FAILED_INVENTORY";
        Order order = new Order(orderId, request.customerId(), request.membershipTier(), request.items(), breakdown, status);
        orderStore.put(orderId, order);
        return order;
    }

    public Optional<Order> getOrder(String id) {
        return Optional.ofNullable(orderStore.get(id));
    }

    public List<Order> getRecentOrders() {
        return new ArrayList<>(orderStore.values());
    }

    public OrderSummary getOrderSummary(String orderId) {
        Order order = orderStore.get(orderId);
        if (order == null) {
            throw new IllegalArgumentException("Order not found: " + orderId);
        }

        int totalItems = order.items().stream().mapToInt(OrderItem::quantity).sum();
        String summaryText = String.format("Order %s for customer %s containing %d items. Total: $%s. Status: %s.",
                order.id(), order.customerId(), totalItems, order.priceBreakdown().total(), order.status());

        return new OrderSummary(
                order.id(),
                order.customerId(),
                totalItems,
                order.priceBreakdown().total(),
                order.status(),
                summaryText
        );
    }
}
