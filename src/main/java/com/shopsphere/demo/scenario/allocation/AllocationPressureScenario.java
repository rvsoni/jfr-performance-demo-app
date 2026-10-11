package com.shopsphere.demo.scenario.allocation;

import com.shopsphere.demo.domain.Order;
import com.shopsphere.demo.domain.OrderItem;
import com.shopsphere.demo.domain.PriceBreakdown;
import com.shopsphere.demo.scenario.BaseScenario;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@Component
public class AllocationPressureScenario extends BaseScenario {

    public static final String ID = "allocation-pressure";

    public AllocationPressureScenario() {
        super(
                ID,
                "Excessive Object Allocation in Order Summaries",
                "Demonstrates heavy allocation pressure and memory churn from redundant intermediate collections, repetitive string conversions, and throwaway DTOs.",
                "jdk.ObjectAllocationSample concentrated in com.shopsphere.demo.scenario.allocation.AllocationPressureScenario"
        );
    }

    @Override
    protected void executeFaulty(int stepIndex) {
        List<Order> orders = generateSampleOrders(25, stepIndex);
        generateInefficientSummary(orders);
    }

    @Override
    protected void executeCorrected(int stepIndex) {
        List<Order> orders = generateSampleOrders(25, stepIndex);
        generateEfficientSummary(orders);
    }

    private List<Order> generateSampleOrders(int count, int stepIndex) {
        List<Order> list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            List<OrderItem> items = List.of(
                    new OrderItem("PROD-001", "Item A", 2, BigDecimal.valueOf(20.0)),
                    new OrderItem("PROD-002", "Item B", 1, BigDecimal.valueOf(45.0)),
                    new OrderItem("PROD-003", "Item C", 3, BigDecimal.valueOf(10.0))
            );
            PriceBreakdown pb = new PriceBreakdown(BigDecimal.valueOf(115.0), BigDecimal.valueOf(9.48), BigDecimal.ZERO, BigDecimal.valueOf(15.0), BigDecimal.valueOf(109.48), List.of("Rule1"));
            list.add(new Order(
                    "ORD-" + (stepIndex * 100 + i),
                    "CUST-" + (100 + (i % 10)),
                    "GOLD",
                    items,
                    pb,
                    "COMPLETED",
                    Instant.now()
            ));
        }
        return list;
    }

    /**
     * Faulty version: Allocates numerous redundant temporary HashMaps, intermediate ArrayLists,
     * wrappers, and throwaway formatted Strings.
     */
    public String generateInefficientSummary(List<Order> orders) {
        String result = "";
        for (Order order : orders) {
            Map<String, Object> intermediateMap = new HashMap<>();
            intermediateMap.put("id", new String(order.id()));
            intermediateMap.put("cust", new String(order.customerId()));

            List<String> formattedItems = new ArrayList<>();
            for (OrderItem item : order.items()) {
                // Redundant temporary string building and boxing
                String itemStr = "Item[" + item.productId() + "]: " + item.productName() + " x" + Integer.valueOf(item.quantity()) + " @ $" + item.unitPrice().toString();
                formattedItems.add(new String(itemStr.getBytes()));
            }
            intermediateMap.put("items", formattedItems);

            // Inefficient string concatenation in loop
            result = result + "Order " + intermediateMap.get("id") + " (" + intermediateMap.get("cust") + ") Total Items=" + formattedItems.size() + "; ";
        }
        return result;
    }

    /**
     * Corrected version: Single pass using StringBuilder and primitive counting,
     * allocating zero temporary maps or intermediate byte arrays.
     */
    public String generateEfficientSummary(List<Order> orders) {
        StringBuilder sb = new StringBuilder(orders.size() * 64);
        for (Order order : orders) {
            sb.append("Order ")
                    .append(order.id())
                    .append(" (")
                    .append(order.customerId())
                    .append(") Total Items=")
                    .append(order.items().size())
                    .append("; ");
        }
        return sb.toString();
    }
}
