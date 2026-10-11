package com.shopsphere.demo.scenario.cpu;

import com.shopsphere.demo.domain.OrderItem;
import com.shopsphere.demo.domain.PriceBreakdown;
import com.shopsphere.demo.domain.PricingRequest;
import com.shopsphere.demo.scenario.ScenarioConfig;
import com.shopsphere.demo.scenario.ScenarioMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class CpuHotspotScenarioTest {

    private CpuHotspotScenario scenario;

    @BeforeEach
    void setUp() {
        scenario = new CpuHotspotScenario();
    }

    @Test
    void testEquivalenceOfFaultyAndCorrected() {
        List<OrderItem> items = List.of(
                new OrderItem("PROD-001", "Item 1", 2, BigDecimal.valueOf(25.0)),
                new OrderItem("PROD-002", "Item 2", 1, BigDecimal.valueOf(50.0))
        );
        PricingRequest request = new PricingRequest("CUST-100", "GOLD", items, "SAVE20", "US-EAST");

        PriceBreakdown faulty = scenario.calculatePriceWithCpuHotspot(request);
        PriceBreakdown corrected = scenario.calculatePriceOptimized(request);

        assertThat(faulty.subtotal()).isEqualTo(corrected.subtotal());
        assertThat(faulty.total()).isNotNull();
        assertThat(corrected.total()).isNotNull();
    }

    @Test
    void testScenarioStartAndCancellation() throws Exception {
        ScenarioConfig config = new ScenarioConfig(10, 1, 10, 2, ScenarioMode.FAULTY, java.util.Map.of());
        CompletableFuture<?> future = scenario.start(config);

        TimeUnit.MILLISECONDS.sleep(200);
        scenario.stop();

        var result = scenario.getInfo();
        assertThat(result.status()).isNotNull();
    }
}
