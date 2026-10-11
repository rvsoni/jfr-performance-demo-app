package com.shopsphere.demo.scenario.contention;

import com.shopsphere.demo.domain.InventoryReservationRequest;
import com.shopsphere.demo.domain.InventoryReservationResult;
import com.shopsphere.demo.domain.OrderItem;
import com.shopsphere.demo.service.InventoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class ThreadContentionScenarioTest {

    private InventoryService inventoryService;
    private ThreadContentionScenario scenario;

    @BeforeEach
    void setUp() {
        inventoryService = new InventoryService();
        inventoryService.init();
        scenario = new ThreadContentionScenario(inventoryService);
    }

    @Test
    void testConcurrentInventoryConsistency() throws Exception {
        int threads = 10;
        int reservationsPerThread = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);
        AtomicInteger successCount = new AtomicInteger(0);

        int initialStock = inventoryService.getStock("PROD-001");

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < reservationsPerThread; j++) {
                        InventoryReservationRequest req = new InventoryReservationRequest(
                                "TEST-ORD",
                                List.of(new OrderItem("PROD-001", "Item 1", 1, BigDecimal.valueOf(10.0)))
                        );
                        InventoryReservationResult res = inventoryService.reserveFineGrained(req);
                        if (res.success()) {
                            successCount.incrementAndGet();
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        int remainingStock = inventoryService.getStock("PROD-001");
        assertThat(remainingStock).isEqualTo(initialStock - successCount.get());
    }

    @Test
    void testScenarioStepExecution() throws Exception {
        scenario.executeWorkloadStep(1, com.shopsphere.demo.scenario.ScenarioMode.FAULTY);
        scenario.executeWorkloadStep(2, com.shopsphere.demo.scenario.ScenarioMode.CORRECTED);
    }
}
