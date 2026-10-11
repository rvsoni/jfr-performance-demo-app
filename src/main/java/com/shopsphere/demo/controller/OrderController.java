package com.shopsphere.demo.controller;

import com.shopsphere.demo.domain.*;
import com.shopsphere.demo.service.OrderService;
import com.shopsphere.demo.service.PricingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;
    private final PricingService pricingService;

    public OrderController(OrderService orderService, PricingService pricingService) {
        this.orderService = orderService;
        this.pricingService = pricingService;
    }

    @PostMapping
    public ResponseEntity<Order> createOrder(@RequestBody PricingRequest request) {
        Order order = orderService.createOrder(request);
        return ResponseEntity.ok(order);
    }

    @GetMapping
    public List<Order> getRecentOrders() {
        return orderService.getRecentOrders();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Order> getOrder(@PathVariable String id) {
        return orderService.getOrder(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/calculate-price")
    public ResponseEntity<PriceBreakdown> calculatePrice(@RequestBody PricingRequest request) {
        PriceBreakdown breakdown = pricingService.calculatePrice(request);
        return ResponseEntity.ok(breakdown);
    }

    @GetMapping("/{id}/summary")
    public ResponseEntity<OrderSummary> getSummary(@PathVariable String id) {
        try {
            OrderSummary summary = orderService.getOrderSummary(id);
            return ResponseEntity.ok(summary);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
