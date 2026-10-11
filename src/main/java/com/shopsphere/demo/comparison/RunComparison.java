package com.shopsphere.demo.comparison;

import java.util.ArrayList;
import java.util.List;

public record RunComparison(
        RunResult before,
        RunResult after,
        double throughputDeltaPercent,
        double p50LatencyDeltaPercent,
        double p95LatencyDeltaPercent,
        double p99LatencyDeltaPercent,
        List<String> comparabilityWarnings,
        String markdownReport
) {

    public static RunComparison compare(RunResult before, RunResult after) {
        List<String> warnings = new ArrayList<>();

        if (!before.scenarioId().equals(after.scenarioId())) {
            warnings.add("Scenario ID mismatch: '" + before.scenarioId() + "' vs '" + after.scenarioId() + "'");
        }
        if (before.concurrency() != after.concurrency()) {
            warnings.add("Concurrency differs: " + before.concurrency() + " vs " + after.concurrency() + " threads");
        }
        if (before.durationSeconds() != after.durationSeconds()) {
            warnings.add("Duration differs: " + before.durationSeconds() + "s vs " + after.durationSeconds() + "s");
        }
        if (!before.jvmArguments().equals(after.jvmArguments())) {
            warnings.add("JVM input arguments differ: [" + String.join(" ", before.jvmArguments()) + "] vs [" + String.join(" ", after.jvmArguments()) + "]");
        }

        double throughputDelta = calcPercentChange(before.throughputRps(), after.throughputRps());
        double p50Delta = calcPercentChange(before.p50LatencyMs(), after.p50LatencyMs());
        double p95Delta = calcPercentChange(before.p95LatencyMs(), after.p95LatencyMs());
        double p99Delta = calcPercentChange(before.p99LatencyMs(), after.p99LatencyMs());

        StringBuilder sb = new StringBuilder();
        sb.append("# Performance Comparison Report\n\n");
        sb.append("**Scenario:** `").append(before.scenarioId()).append("`\n\n");
        sb.append("| Metric | Before (").append(before.mode()).append(") | After (").append(after.mode()).append(") | Delta | Change |\n");
        sb.append("| :--- | :--- | :--- | :--- | :--- |\n");
        sb.append(String.format("| **Throughput (req/s)** | `%.2f` | `%.2f` | `%+.2f%%` | %s |\n",
                before.throughputRps(), after.throughputRps(), throughputDelta, throughputDelta > 0 ? "🚀 Improvement" : "⚠️ Regression"));
        sb.append(String.format("| **P50 Latency (ms)** | `%.2f` | `%.2f` | `%+.2f%%` | %s |\n",
                before.p50LatencyMs(), after.p50LatencyMs(), p50Delta, p50Delta < 0 ? "🚀 Improvement" : "⚠️ Regression"));
        sb.append(String.format("| **P95 Latency (ms)** | `%.2f` | `%.2f` | `%+.2f%%` | %s |\n",
                before.p95LatencyMs(), after.p95LatencyMs(), p95Delta, p95Delta < 0 ? "🚀 Improvement" : "⚠️ Regression"));
        sb.append(String.format("| **P99 Latency (ms)** | `%.2f` | `%.2f` | `%+.2f%%` | %s |\n",
                before.p99LatencyMs(), after.p99LatencyMs(), p99Delta, p99Delta < 0 ? "🚀 Improvement" : "⚠️ Regression"));
        sb.append(String.format("| **Total Requests** | `%d` | `%d` | `%+d` | — |\n",
                before.totalRequests(), after.totalRequests(), (after.totalRequests() - before.totalRequests())));
        sb.append(String.format("| **Failed Requests** | `%d` | `%d` | `%+d` | — |\n",
                before.failedRequests(), after.failedRequests(), (after.failedRequests() - before.failedRequests())));

        sb.append("\n## Workload Parameters\n\n");
        sb.append("- **Concurrency:** ").append(before.concurrency()).append(" threads (After: ").append(after.concurrency()).append(" threads)\n");
        sb.append("- **Duration:** ").append(before.durationSeconds()).append("s (After: ").append(after.durationSeconds()).append("s)\n");
        sb.append("- **Warm-up:** ").append(before.warmupSeconds()).append("s\n");
        sb.append("- **JFR Recording Before:** `").append(before.jfrRecordingFile()).append("`\n");
        sb.append("- **JFR Recording After:** `").append(after.jfrRecordingFile()).append("`\n");

        if (!warnings.isEmpty()) {
            sb.append("\n> [!WARNING]\n");
            sb.append("> **Comparability Warnings:**\n");
            for (String w : warnings) {
                sb.append("> - ").append(w).append("\n");
            }
        }

        return new RunComparison(before, after, throughputDelta, p50Delta, p95Delta, p99Delta, warnings, sb.toString());
    }

    private static double calcPercentChange(double before, double after) {
        if (before <= 0.00001) {
            return 0.0;
        }
        return ((after - before) / before) * 100.0;
    }
}
