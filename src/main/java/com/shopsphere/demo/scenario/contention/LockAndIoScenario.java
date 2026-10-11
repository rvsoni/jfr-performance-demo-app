package com.shopsphere.demo.scenario.contention;

import com.shopsphere.demo.domain.InventoryReservationRequest;
import com.shopsphere.demo.domain.OrderItem;
import com.shopsphere.demo.scenario.BaseScenario;
import com.shopsphere.demo.service.DownstreamSimulator;
import com.shopsphere.demo.service.InventoryService;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class LockAndIoScenario extends BaseScenario {

    public static final String ID = "lock-and-io";

    private final InventoryService inventoryService;
    private final DownstreamSimulator downstreamSimulator;
    private final Object sharedLock = new Object();

    public LockAndIoScenario(InventoryService inventoryService, DownstreamSimulator downstreamSimulator) {
        super(
                ID,
                "Excessive Lock Scope During Downstream Work",
                "Demonstrates how holding a shared monitor lock while waiting on downstream I/O blocks unrelated concurrent threads.",
                "jdk.JavaMonitorEnter contention combined with long critical sections holding locks during downstream calls"
        );
        this.inventoryService = inventoryService;
        this.downstreamSimulator = downstreamSimulator;
    }

    @Override
    protected void executeFaulty(int stepIndex) {
        InventoryReservationRequest request = createRequest(stepIndex);
        // Faulty: Holds sharedLock while waiting for 25ms downstream call!
        synchronized (sharedLock) {
            downstreamSimulator.callPaymentGateway("CUST-" + stepIndex, 50.0, 20);
            inventoryService.reserveFineGrained(request);
        }
    }

    @Override
    protected void executeCorrected(int stepIndex) {
        InventoryReservationRequest request = createRequest(stepIndex);
        // Corrected: Downstream I/O performed outside the lock
        downstreamSimulator.callPaymentGateway("CUST-" + stepIndex, 50.0, 20);
        synchronized (sharedLock) {
            inventoryService.reserveFineGrained(request);
        }
    }

    private InventoryReservationRequest createRequest(int stepIndex) {
        return new InventoryReservationRequest("ORD-LIO-" + stepIndex, List.of(
                new OrderItem("PROD-001", "Item A", 1, BigDecimal.valueOf(10.0))
        ));
    }
}
