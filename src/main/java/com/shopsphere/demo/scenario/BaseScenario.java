package com.shopsphere.demo.scenario;

import com.shopsphere.demo.workload.LatencyTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public abstract class BaseScenario implements Scenario {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    private final String id;
    private final String name;
    private final String description;
    private final String jfrSignature;

    private final AtomicReference<ScenarioStatus> status = new AtomicReference<>(ScenarioStatus.IDLE);
    private final AtomicReference<ScenarioMode> currentMode = new AtomicReference<>(ScenarioMode.FAULTY);
    private final AtomicReference<ScenarioConfig> currentConfig = new AtomicReference<>(ScenarioConfig.defaultConfig(ScenarioMode.FAULTY));
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicInteger activeWorkers = new AtomicInteger(0);

    private final LatencyTracker measurementTracker = new LatencyTracker();
    private final LatencyTracker warmupTracker = new LatencyTracker();

    private ExecutorService scenarioExecutor;
    private CompletableFuture<ScenarioExecutionResult> activeFuture;

    protected BaseScenario(String id, String name, String description, String jfrSignature) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.jfrSignature = jfrSignature;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String getJfrSignature() {
        return jfrSignature;
    }

    @Override
    public ScenarioStatus getStatus() {
        return status.get();
    }

    @Override
    public ScenarioMode getMode() {
        return currentMode.get();
    }

    @Override
    public void setMode(ScenarioMode mode) {
        this.currentMode.set(mode);
    }

    @Override
    public ScenarioInfo getInfo() {
        return new ScenarioInfo(
                id,
                name,
                description,
                jfrSignature,
                status.get(),
                currentMode.get(),
                currentConfig.get(),
                getMetrics()
        );
    }

    @Override
    public synchronized CompletableFuture<ScenarioExecutionResult> start(ScenarioConfig config) {
        if (!status.compareAndSet(ScenarioStatus.IDLE, ScenarioStatus.WARMING_UP) &&
                !status.compareAndSet(ScenarioStatus.COMPLETED, ScenarioStatus.WARMING_UP) &&
                !status.compareAndSet(ScenarioStatus.STOPPED, ScenarioStatus.WARMING_UP) &&
                !status.compareAndSet(ScenarioStatus.FAILED, ScenarioStatus.WARMING_UP)) {
            throw new IllegalStateException("Scenario '" + id + "' is already running with status: " + status.get());
        }

        this.currentConfig.set(config);
        this.currentMode.set(config.mode());
        this.running.set(true);
        this.warmupTracker.reset();
        this.measurementTracker.reset();

        final Instant startTime = Instant.now();
        log.info("[SCENARIO-EVENT] Scenario='{}' Mode='{}' Concurrency={} Duration={}s Warmup={}s - START",
                id, config.mode(), config.concurrency(), config.durationSeconds(), config.warmupSeconds());

        final int concurrency = config.concurrency();
        final ThreadFactory threadFactory = r -> {
            Thread t = new Thread(r, "scenario-" + id + "-worker");
            t.setDaemon(true);
            return t;
        };
        this.scenarioExecutor = Executors.newFixedThreadPool(concurrency, threadFactory);

        activeFuture = CompletableFuture.supplyAsync(() -> {
            try {
                // Phase 1: Warmup
                if (config.warmupSeconds() > 0 && running.get()) {
                    log.info("[SCENARIO-EVENT] Scenario='{}' - WARMUP START ({}s)", id, config.warmupSeconds());
                    runWorkloadPhase(config, config.warmupSeconds(), warmupTracker);
                    log.info("[SCENARIO-EVENT] Scenario='{}' - WARMUP END", id);
                }

                if (!running.get()) {
                    status.set(ScenarioStatus.STOPPED);
                    return buildResult(startTime, Instant.now(), ScenarioStatus.STOPPED, "Stopped during warmup");
                }

                // Phase 2: Measurement
                status.set(ScenarioStatus.MEASURING);
                log.info("[SCENARIO-EVENT] Scenario='{}' - MEASUREMENT START ({}s)", id, config.durationSeconds());
                final Instant measurementStart = Instant.now();
                runWorkloadPhase(config, config.durationSeconds(), measurementTracker);
                final Instant measurementEnd = Instant.now();
                log.info("[SCENARIO-EVENT] Scenario='{}' - MEASUREMENT END", id);

                ScenarioStatus finalStatus = running.get() ? ScenarioStatus.COMPLETED : ScenarioStatus.STOPPED;
                status.set(finalStatus);
                log.info("[SCENARIO-EVENT] Scenario='{}' Mode='{}' - STOPPED/COMPLETED. Requests: {}",
                        id, config.mode(), measurementTracker.getTotalRequests());

                return buildResult(measurementStart, measurementEnd, finalStatus, "Execution completed successfully");
            } catch (Exception e) {
                log.error("[SCENARIO-EVENT] Scenario='{}' - FAILED: {}", id, e.getMessage(), e);
                status.set(ScenarioStatus.FAILED);
                return buildResult(startTime, Instant.now(), ScenarioStatus.FAILED, "Failed: " + e.getMessage());
            } finally {
                running.set(false);
                shutdownExecutor();
            }
        });

        return activeFuture;
    }

    private void runWorkloadPhase(ScenarioConfig config, int durationSeconds, LatencyTracker tracker) {
        final long deadlineMs = System.currentTimeMillis() + (durationSeconds * 1000L);
        final int concurrency = config.concurrency();
        final CountDownLatch latch = new CountDownLatch(concurrency);
        final AtomicInteger stepCounter = new AtomicInteger(0);

        for (int i = 0; i < concurrency; i++) {
            scenarioExecutor.submit(() -> {
                activeWorkers.incrementAndGet();
                try {
                    while (running.get() && System.currentTimeMillis() < deadlineMs) {
                        int step = stepCounter.incrementAndGet();
                        long startNanos = System.nanoTime();
                        boolean success = true;
                        try {
                            executeWorkloadStep(step, config.mode());
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        } catch (Exception e) {
                            success = false;
                        } finally {
                            long durationNanos = System.nanoTime() - startNanos;
                            tracker.recordDurationNanos(durationNanos, success);
                        }

                        // Rate limiting / yielding if intensity is low
                        if (config.intensity() < 100) {
                            int sleepMicros = (100 - config.intensity()) * 50;
                            try {
                                TimeUnit.MICROSECONDS.sleep(sleepMicros);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                break;
                            }
                        }
                    }
                } finally {
                    activeWorkers.decrementAndGet();
                    latch.countDown();
                }
            });
        }

        try {
            latch.await((durationSeconds + 5L), TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("[SCENARIO-EVENT] Scenario='{}' interrupted during workload phase", id);
        }
    }

    @Override
    public synchronized void stop() {
        if (running.compareAndSet(true, false)) {
            log.info("[SCENARIO-EVENT] Scenario='{}' stop requested", id);
            status.set(ScenarioStatus.STOPPED);
            shutdownExecutor();
        }
    }

    private void shutdownExecutor() {
        if (scenarioExecutor != null && !scenarioExecutor.isShutdown()) {
            scenarioExecutor.shutdownNow();
            try {
                if (!scenarioExecutor.awaitTermination(3, TimeUnit.SECONDS)) {
                    log.warn("Scenario executor for '{}' did not terminate in time", id);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    public void executeWorkloadStep(int stepIndex, ScenarioMode mode) throws Exception {
        switch (mode) {
            case FAULTY -> executeFaulty(stepIndex);
            case CORRECTED -> executeCorrected(stepIndex);
            case NORMAL -> executeNormal(stepIndex);
        }
    }

    protected abstract void executeFaulty(int stepIndex) throws Exception;

    protected abstract void executeCorrected(int stepIndex) throws Exception;

    protected void executeNormal(int stepIndex) throws Exception {
        executeCorrected(stepIndex);
    }

    protected ScenarioExecutionResult buildResult(Instant start, Instant end, ScenarioStatus finalStatus, String message) {
        long durationMs = Math.max(1, java.time.Duration.between(start, end).toMillis());
        long total = measurementTracker.getTotalRequests();
        double throughput = (total * 1000.0) / durationMs;

        return new ScenarioExecutionResult(
                id,
                currentMode.get(),
                finalStatus,
                start,
                end,
                durationMs,
                total,
                measurementTracker.getSuccessfulRequests(),
                measurementTracker.getFailedRequests(),
                throughput,
                measurementTracker.getP50Ms(),
                measurementTracker.getP95Ms(),
                measurementTracker.getP99Ms(),
                measurementTracker.getMeanMs(),
                measurementTracker.getMaxMs(),
                message,
                getCustomMetrics()
        );
    }

    @Override
    public Map<String, Object> getMetrics() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("status", status.get().name());
        m.put("mode", currentMode.get().name());
        m.put("activeWorkers", activeWorkers.get());
        m.put("totalRequests", measurementTracker.getTotalRequests());
        m.put("successfulRequests", measurementTracker.getSuccessfulRequests());
        m.put("failedRequests", measurementTracker.getFailedRequests());
        m.put("p50Ms", measurementTracker.getP50Ms());
        m.put("p95Ms", measurementTracker.getP95Ms());
        m.put("p99Ms", measurementTracker.getP99Ms());
        m.put("meanMs", measurementTracker.getMeanMs());
        m.putAll(getCustomMetrics());
        return m;
    }

    protected Map<String, Object> getCustomMetrics() {
        return Collections.emptyMap();
    }
}
