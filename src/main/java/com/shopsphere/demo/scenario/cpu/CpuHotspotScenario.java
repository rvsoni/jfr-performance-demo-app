package com.shopsphere.demo.scenario.cpu;

import com.shopsphere.demo.domain.OrderItem;
import com.shopsphere.demo.domain.PriceBreakdown;
import com.shopsphere.demo.domain.PricingRequest;
import com.shopsphere.demo.scenario.BaseScenario;
import com.shopsphere.demo.scenario.ScenarioMode;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Component
public class CpuHotspotScenario extends BaseScenario {

    public static final String ID = "cpu-hotspot";

    public CpuHotspotScenario() {
        super(
                ID,
                "CPU Hotspot in Pricing Calculation",
                "Demonstrates high CPU consumption caused by redundant, unmemoized polynomial discount calculations inside business methods.",
                "jdk.ExecutionSample concentrated in com.shopsphere.demo.scenario.cpu.CpuHotspotScenario.calculateComplexDiscountMatrix"
        );
    }

    @Override
    protected void executeFaulty(int stepIndex) {
        PricingRequest request = createSampleRequest(stepIndex);
        calculatePriceWithCpuHotspot(request);
    }

    @Override
    protected void executeCorrected(int stepIndex) {
        PricingRequest request = createSampleRequest(stepIndex);
        calculatePriceOptimized(request);
    }

    private PricingRequest createSampleRequest(int stepIndex) {
        int itemsCount = 3 + (stepIndex % 4);
        List<OrderItem> items = new ArrayList<>(itemsCount);
        for (int i = 1; i <= itemsCount; i++) {
            items.add(new OrderItem("PROD-" + String.format("%03d", i), "Item " + i, 2 + (i % 3), BigDecimal.valueOf(15.0 + i)));
        }
        return new PricingRequest("CUST-" + (1000 + stepIndex % 50), "GOLD", items, "SAVE20", "US-EAST");
    }

    public PriceBreakdown calculatePriceWithCpuHotspot(PricingRequest request) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (OrderItem item : request.items()) {
            subtotal = subtotal.add(item.unitPrice().multiply(BigDecimal.valueOf(item.quantity())));
        }

        // Intentionally faulty: calls redundant, expensive polynomial evaluation repeatedly
        double rawDiscount = calculateComplexDiscountMatrix(request.customerId(), subtotal.doubleValue(), request.items().size());
        BigDecimal discount = BigDecimal.valueOf(rawDiscount).setScale(2, RoundingMode.HALF_UP);
        BigDecimal discountedSubtotal = subtotal.subtract(discount).max(BigDecimal.ZERO);

        BigDecimal tax = discountedSubtotal.multiply(BigDecimal.valueOf(0.0825)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal shipping = subtotal.compareTo(BigDecimal.valueOf(100)) >= 0 ? BigDecimal.ZERO : BigDecimal.valueOf(9.99);
        BigDecimal total = discountedSubtotal.add(tax).add(shipping).setScale(2, RoundingMode.HALF_UP);

        List<String> rules = List.of("Gold Tier", "Promo SAVE20", "Dynamic Polynomial Discount Matrix");
        return new PriceBreakdown(subtotal.setScale(2, RoundingMode.HALF_UP), tax, shipping, discount, total, rules);
    }

    public PriceBreakdown calculatePriceOptimized(PricingRequest request) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (OrderItem item : request.items()) {
            subtotal = subtotal.add(item.unitPrice().multiply(BigDecimal.valueOf(item.quantity())));
        }

        // Optimized equivalent: constant-time closed-form formula
        double rawDiscount = calculateClosedFormDiscount(request.customerId(), subtotal.doubleValue(), request.items().size());
        BigDecimal discount = BigDecimal.valueOf(rawDiscount).setScale(2, RoundingMode.HALF_UP);
        BigDecimal discountedSubtotal = subtotal.subtract(discount).max(BigDecimal.ZERO);

        BigDecimal tax = discountedSubtotal.multiply(BigDecimal.valueOf(0.0825)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal shipping = subtotal.compareTo(BigDecimal.valueOf(100)) >= 0 ? BigDecimal.ZERO : BigDecimal.valueOf(9.99);
        BigDecimal total = discountedSubtotal.add(tax).add(shipping).setScale(2, RoundingMode.HALF_UP);

        List<String> rules = List.of("Gold Tier", "Promo SAVE20", "Optimized Constant-Time Discount");
        return new PriceBreakdown(subtotal.setScale(2, RoundingMode.HALF_UP), tax, shipping, discount, total, rules);
    }

    /**
     * Faulty CPU-intensive method: Performs redundant polynomial loop evaluations.
     * Hot method easily identified in JMC and JFR execution samples.
     */
    public double calculateComplexDiscountMatrix(String customerId, double subtotal, int itemCount) {
        double result = 0.0;
        int seed = Math.abs(customerId.hashCode() % 100);
        int iterations = 120_000;

        for (int i = 0; i < iterations; i++) {
            double angle = (i + seed) * 0.001;
            double term1 = Math.sin(angle) * Math.cos(angle);
            double term2 = Math.sqrt(Math.abs(term1) + 1.0);
            result += (term2 - 1.0) / (iterations * 10.0);
        }

        double baseRate = 0.30; // 30% discount base
        return (subtotal * baseRate) + (result % (subtotal * 0.05));
    }

    /**
     * Optimized O(1) closed-form calculation producing mathematically equivalent business result.
     */
    public double calculateClosedFormDiscount(String customerId, double subtotal, int itemCount) {
        double baseRate = 0.30;
        return subtotal * baseRate;
    }
}
