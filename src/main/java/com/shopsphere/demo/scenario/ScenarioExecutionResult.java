package com.shopsphere.demo.scenario;

import java.time.Instant;
import java.util.Map;

public record ScenarioExecutionResult(
        String scenarioId,
        ScenarioMode mode,
        ScenarioStatus status,
        Instant startTime,
        Instant endTime,
        long durationMs,
        long totalRequests,
        long successfulRequests,
        long failedRequests,
        double throughputRps,
        double p50LatencyMs,
        double p95LatencyMs,
        double p99LatencyMs,
        double meanLatencyMs,
        double maxLatencyMs,
        String message,
        Map<String, Object> customMetrics
) {
}
