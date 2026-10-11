package com.shopsphere.demo.controller;

import com.shopsphere.demo.scenario.Scenario;
import com.shopsphere.demo.scenario.ScenarioRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.ThreadMXBean;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/metrics")
public class MetricsController {

    private final ScenarioRegistry scenarioRegistry;
    private final MeterRegistry meterRegistry;
    private final MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
    private final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();

    public MetricsController(ScenarioRegistry scenarioRegistry, MeterRegistry meterRegistry) {
        this.scenarioRegistry = scenarioRegistry;
        this.meterRegistry = meterRegistry;
    }

    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getSummary() {
        Map<String, Object> summary = new LinkedHashMap<>();

        // JVM Runtime metrics
        Map<String, Object> jvm = new LinkedHashMap<>();
        jvm.put("heapUsedBytes", memoryMXBean.getHeapMemoryUsage().getUsed());
        jvm.put("heapMaxBytes", memoryMXBean.getHeapMemoryUsage().getMax());
        jvm.put("heapCommittedBytes", memoryMXBean.getHeapMemoryUsage().getCommitted());
        jvm.put("nonHeapUsedBytes", memoryMXBean.getNonHeapMemoryUsage().getUsed());
        jvm.put("threadCount", threadMXBean.getThreadCount());
        jvm.put("peakThreadCount", threadMXBean.getPeakThreadCount());
        jvm.put("daemonThreadCount", threadMXBean.getDaemonThreadCount());
        summary.put("jvm", jvm);

        // Active scenario metrics
        var activeOpt = scenarioRegistry.getActiveScenario();
        if (activeOpt.isPresent()) {
            Scenario active = activeOpt.get();
            summary.put("activeScenario", active.getInfo());
        } else {
            summary.put("activeScenario", null);
        }

        // All scenario metrics summaries
        Map<String, Object> scenarioMetrics = new LinkedHashMap<>();
        for (var s : scenarioRegistry.getAllScenarios()) {
            scenarioMetrics.put(s.id(), s.metrics());
        }
        summary.put("scenarios", scenarioMetrics);

        return ResponseEntity.ok(summary);
    }
}
