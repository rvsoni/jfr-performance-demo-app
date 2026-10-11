package com.shopsphere.demo.comparison;

import com.shopsphere.demo.scenario.ScenarioMode;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record RunResult(
        String scenarioId,
        ScenarioMode mode,
        int durationSeconds,
        int warmupSeconds,
        int concurrency,
        int intensity,
        long totalRequests,
        long successfulRequests,
        long failedRequests,
        double throughputRps,
        double p50LatencyMs,
        double p95LatencyMs,
        double p99LatencyMs,
        double meanLatencyMs,
        double maxLatencyMs,
        String jfrRecordingFile,
        String jvmVersion,
        List<String> jvmArguments,
        Instant timestamp,
        Map<String, Object> customMetrics
) {
}
