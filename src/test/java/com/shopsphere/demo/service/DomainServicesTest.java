package com.shopsphere.demo.service;

import com.shopsphere.demo.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DomainServicesTest {

    private CatalogService catalogService;
    private PricingService pricingService;
    private InventoryService inventoryService;
    private OrderService orderService;

    @BeforeEach
    void setUp() {
        catalogService = new CatalogService();
        catalogService.init();

        pricingService = new PricingService();

        inventoryService = new InventoryService();
        inventoryService.init();

        orderService = new OrderService(pricingService, inventoryService);
    }

    @Test
    void testCatalogPopulated() {
        List<Product> products = catalogService.getAllProducts();
        assertThat(products).hasSize(100);
        assertThat(catalogService.getProduct("PROD-001")).isPresent();
    }

    @Test
    void testPricingCalculations() {
        List<OrderItem> items = List.of(
                new OrderItem("PROD-001", "Item 1", 2, BigDecimal.valueOf(50.00))
        );
        PricingRequest request = new PricingRequest("CUST-1", "PLATINUM", items, "SAVE20", "US-EAST");
        PriceBreakdown breakdown = pricingService.calculatePrice(request);

        assertThat(breakdown.subtotal()).isEqualByComparingTo("100.00");
        // Platinum 15% ($15) + SAVE20 20% ($20) = $35 discount
        assertThat(breakdown.discount()).isEqualByComparingTo("35.00");
        // Discounted = 65.00, Tax (8.25%) = 5.36, Shipping = 0.00 (subtotal >= 100) -> total = 70.36
        assertThat(breakdown.total()).isEqualByComparingTo("70.36");
    }

    @Test
    void testInventoryReservation() {
        InventoryReservationRequest req = new InventoryReservationRequest(
                "ORD-999",
                List.of(new OrderItem("PROD-001", "Item 1", 5, BigDecimal.valueOf(10.00)))
        );

        int initialStock = inventoryService.getStock("PROD-001");
        InventoryReservationResult result = inventoryService.reserve(req);

        assertThat(result.success()).isTrue();
        assertThat(inventoryService.getStock("PROD-001")).isEqualTo(initialStock - 5);
    }

    @Test
    void testOrderCreation() {
        List<OrderItem> items = List.of(
                new OrderItem("PROD-001", "Item 1", 1, BigDecimal.valueOf(25.00))
        );
        PricingRequest request = new PricingRequest("CUST-2", "REGULAR", items, "", "US-WEST");
        Order order = orderService.createOrder(request);

        assertThat(order).isNotNull();
        assertThat(order.status()).isEqualTo("CONFIRMED");
        assertThat(orderService.getOrder(order.id())).isPresent();
    }
}
