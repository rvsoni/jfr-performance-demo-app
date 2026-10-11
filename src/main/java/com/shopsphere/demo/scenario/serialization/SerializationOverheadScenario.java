package com.shopsphere.demo.scenario.serialization;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopsphere.demo.domain.Order;
import com.shopsphere.demo.domain.OrderItem;
import com.shopsphere.demo.domain.PriceBreakdown;
import com.shopsphere.demo.scenario.BaseScenario;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class SerializationOverheadScenario extends BaseScenario {

    public static final String ID = "serialization-overhead";

    private final ObjectMapper objectMapper;

    public SerializationOverheadScenario(ObjectMapper objectMapper) {
        super(
                ID,
                "Inefficient JSON Serialization Roundtrips",
                "Demonstrates excessive CPU and allocation overhead caused by repeated serialization and deserialization hops during response building.",
                "jdk.ExecutionSample and jdk.ObjectAllocationSample in com.fasterxml.jackson serialization methods"
        );
        this.objectMapper = objectMapper.copy().findAndRegisterModules();
    }

    @Override
    protected void executeFaulty(int stepIndex) throws Exception {
        Order order = createSampleOrder(stepIndex);
        // Faulty: serializes to JSON string, parses into JsonNode tree, writes to byte[], converts to Map, re-serializes
        for (int i = 0; i < 5; i++) {
            String json1 = objectMapper.writeValueAsString(order);
            JsonNode tree = objectMapper.readTree(json1);
            byte[] bytes = objectMapper.writeValueAsBytes(tree);
            Map<String, Object> map = objectMapper.readValue(bytes, new TypeReference<Map<String, Object>>() {});
            String finalJson = objectMapper.writeValueAsString(map);
            if (finalJson.isEmpty()) {
                log.trace("Empty json");
            }
        }
    }

    @Override
    protected void executeCorrected(int stepIndex) throws Exception {
        Order order = createSampleOrder(stepIndex);
        // Corrected: direct single-pass serialization
        for (int i = 0; i < 5; i++) {
            String finalJson = objectMapper.writeValueAsString(order);
            if (finalJson.isEmpty()) {
                log.trace("Empty json");
            }
        }
    }

    private Order createSampleOrder(int stepIndex) {
        List<OrderItem> items = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            items.add(new OrderItem("PROD-" + String.format("%03d", i), "Product " + i, i, BigDecimal.valueOf(12.50)));
        }
        PriceBreakdown pb = new PriceBreakdown(BigDecimal.valueOf(125.0), BigDecimal.valueOf(10.31), BigDecimal.ZERO, BigDecimal.valueOf(15.0), BigDecimal.valueOf(120.31), List.of("Rule A", "Rule B"));
        return new Order("ORD-SER-" + stepIndex, "CUST-999", "PLATINUM", items, pb, "COMPLETED", Instant.now());
    }
}
