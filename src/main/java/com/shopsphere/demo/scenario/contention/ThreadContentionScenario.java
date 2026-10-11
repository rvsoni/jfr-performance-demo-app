package com.shopsphere.demo.scenario.contention;

import com.shopsphere.demo.domain.InventoryReservationRequest;
import com.shopsphere.demo.domain.InventoryReservationResult;
import com.shopsphere.demo.domain.OrderItem;
import com.shopsphere.demo.scenario.BaseScenario;
import com.shopsphere.demo.service.InventoryService;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class ThreadContentionScenario extends BaseScenario {

    public static final String ID = "thread-contention";

    private final InventoryService inventoryService;

    public ThreadContentionScenario(InventoryService inventoryService) {
        super(
                ID,
                "Monitor Contention on Shared Inventory Lock",
                "Demonstrates thread serialization and high monitor wait times caused by a single coarse-grained synchronized lock across all inventory reservations.",
                "jdk.JavaMonitorEnter events on com.shopsphere.demo.service.InventoryService.globalMonitor"
        );
        this.inventoryService = inventoryService;
    }

    @Override
    protected void executeFaulty(int stepIndex) {
        InventoryReservationRequest request = createReservationRequest(stepIndex);
        // Coarse-grained lock with critical section work
        InventoryReservationResult result = inventoryService.reserveWithHeavyLock(request, 1);
        if (!result.success()) {
            inventoryService.replenishAll();
        }
    }

    @Override
    protected void executeCorrected(int stepIndex) {
        InventoryReservationRequest request = createReservationRequest(stepIndex);
        // Fine-grained lock-free atomic reservations per product
        InventoryReservationResult result = inventoryService.reserveFineGrained(request);
        if (!result.success()) {
            inventoryService.replenishAll();
        }
    }

    private InventoryReservationRequest createReservationRequest(int stepIndex) {
        // Repeatedly target popular products to create realistic hot-item contention
        String prodId1 = "PROD-" + String.format("%03d", 1 + (stepIndex % 5));
        String prodId2 = "PROD-" + String.format("%03d", 1 + ((stepIndex + 1) % 5));

        List<OrderItem> items = List.of(
                new OrderItem(prodId1, "Popular Item A", 1, BigDecimal.valueOf(25.0)),
                new OrderItem(prodId2, "Popular Item B", 1, BigDecimal.valueOf(40.0))
        );
        return new InventoryReservationRequest("ORD-CONT-" + stepIndex, items);
    }
}
