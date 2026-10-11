package com.shopsphere.demo.workload;

import com.shopsphere.demo.comparison.RunResult;
import com.shopsphere.demo.jfr.JfrRecordingHelper;
import com.shopsphere.demo.scenario.Scenario;
import com.shopsphere.demo.scenario.ScenarioConfig;
import com.shopsphere.demo.scenario.ScenarioMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class WorkloadGenerator {

    private static final Logger log = LoggerFactory.getLogger(WorkloadGenerator.class);

    private final JfrRecordingHelper jfrRecordingHelper;

    public WorkloadGenerator(JfrRecordingHelper jfrRecordingHelper) {
        this.jfrRecordingHelper = jfrRecordingHelper;
    }

    public RunResult runScenarioWorkload(Scenario scenario, ScenarioConfig config, boolean recordJfr) {
        String recordingName = null;
        if (recordJfr) {
            recordingName = jfrRecordingHelper.startRecording(scenario.getId(), config.mode().name().toLowerCase(), "profile");
        }

        final LatencyTracker warmupTracker = new LatencyTracker();
        final LatencyTracker measurementTracker = new LatencyTracker();
        final AtomicBoolean running = new AtomicBoolean(true);
        final AtomicInteger stepCounter = new AtomicInteger(0);

        final int concurrency = config.concurrency();
        final ExecutorService executor = Executors.newFixedThreadPool(concurrency, r -> {
            Thread t = new Thread(r, "workload-" + scenario.getId() + "-worker");
            t.setDaemon(true);
            return t;
        });

        try {
            // Warm-up Phase
            if (config.warmupSeconds() > 0) {
                log.info("[WORKLOAD-EVENT] Scenario='{}' Mode='{}' - WARMUP START ({}s)",
                        scenario.getId(), config.mode(), config.warmupSeconds());
                executePhase(scenario, config.mode(), config.warmupSeconds(), config.intensity(), concurrency, executor, running, stepCounter, warmupTracker);
                log.info("[WORKLOAD-EVENT] Scenario='{}' - WARMUP END", scenario.getId());
            }

            // Measurement Phase
            log.info("[WORKLOAD-EVENT] Scenario='{}' Mode='{}' - MEASUREMENT START ({}s)",
                    scenario.getId(), config.mode(), config.durationSeconds());
            final Instant measurementStart = Instant.now();
            executePhase(scenario, config.mode(), config.durationSeconds(), config.intensity(), concurrency, executor, running, stepCounter, measurementTracker);
            final Instant measurementEnd = Instant.now();
            log.info("[WORKLOAD-EVENT] Scenario='{}' - MEASUREMENT END", scenario.getId());

            long durationMs = Math.max(1, java.time.Duration.between(measurementStart, measurementEnd).toMillis());
            long total = measurementTracker.getTotalRequests();
            double throughput = (total * 1000.0) / durationMs;

            Path dumpedRecording = null;
            if (recordJfr && recordingName != null) {
                dumpedRecording = jfrRecordingHelper.stopRecording(recordingName);
            }

            return new RunResult(
                    scenario.getId(),
                    config.mode(),
                    config.durationSeconds(),
                    config.warmupSeconds(),
                    config.concurrency(),
                    config.intensity(),
                    total,
                    measurementTracker.getSuccessfulRequests(),
                    measurementTracker.getFailedRequests(),
                    throughput,
                    measurementTracker.getP50Ms(),
                    measurementTracker.getP95Ms(),
                    measurementTracker.getP99Ms(),
                    measurementTracker.getMeanMs(),
                    measurementTracker.getMaxMs(),
                    dumpedRecording != null ? dumpedRecording.getFileName().toString() : "none",
                    System.getProperty("java.version"),
                    ManagementFactory.getRuntimeMXBean().getInputArguments(),
                    Instant.now(),
                    scenario.getMetrics()
            );
        } finally {
            running.set(false);
            executor.shutdownNow();
        }
    }

    private void executePhase(
            Scenario scenario,
            ScenarioMode mode,
            int durationSeconds,
            int intensity,
            int concurrency,
            ExecutorService executor,
            AtomicBoolean running,
            AtomicInteger stepCounter,
            LatencyTracker tracker
    ) {
        final long deadlineMs = System.currentTimeMillis() + (durationSeconds * 1000L);
        final CountDownLatch latch = new CountDownLatch(concurrency);

        for (int i = 0; i < concurrency; i++) {
            executor.submit(() -> {
                try {
                    while (running.get() && System.currentTimeMillis() < deadlineMs) {
                        int step = stepCounter.incrementAndGet();
                        long startNanos = System.nanoTime();
                        boolean success = true;
                        try {
                            scenario.executeWorkloadStep(step, mode);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        } catch (Exception e) {
                            success = false;
                        } finally {
                            long durationNanos = System.nanoTime() - startNanos;
                            tracker.recordDurationNanos(durationNanos, success);
                        }

                        if (intensity < 100) {
                            int sleepMicros = (100 - intensity) * 50;
                            try {
                                TimeUnit.MICROSECONDS.sleep(sleepMicros);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                break;
                            }
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        try {
            latch.await(durationSeconds + 5L, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
