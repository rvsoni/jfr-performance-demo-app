package com.shopsphere.demo.scenario;

import java.util.Map;

public record ScenarioInfo(
        String id,
        String name,
        String description,
        String jfrSignature,
        ScenarioStatus status,
        ScenarioMode activeMode,
        ScenarioConfig currentConfig,
        Map<String, Object> metrics
) {
}
