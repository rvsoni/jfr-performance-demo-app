package com.shopsphere.demo.scenario;

import java.util.Collections;
import java.util.Map;

public record ScenarioConfig(
        int durationSeconds,
        int warmupSeconds,
        int intensity,
        int concurrency,
        ScenarioMode mode,
        Map<String, Object> parameters
) {
    public static final int DEFAULT_DURATION_SECONDS = 20;
    public static final int DEFAULT_WARMUP_SECONDS = 3;
    public static final int DEFAULT_INTENSITY = 10;
    public static final int DEFAULT_CONCURRENCY = 8;
    public static final int MAX_DURATION_SECONDS = 120;
    public static final int MAX_CONCURRENCY = 50;

    public ScenarioConfig {
        if (durationSeconds <= 0) {
            durationSeconds = DEFAULT_DURATION_SECONDS;
        } else if (durationSeconds > MAX_DURATION_SECONDS) {
            durationSeconds = MAX_DURATION_SECONDS;
        }

        if (warmupSeconds < 0) {
            warmupSeconds = DEFAULT_WARMUP_SECONDS;
        }

        if (intensity <= 0) {
            intensity = DEFAULT_INTENSITY;
        }

        if (concurrency <= 0) {
            concurrency = DEFAULT_CONCURRENCY;
        } else if (concurrency > MAX_CONCURRENCY) {
            concurrency = MAX_CONCURRENCY;
        }

        if (mode == null) {
            mode = ScenarioMode.FAULTY;
        }

        if (parameters == null) {
            parameters = Collections.emptyMap();
        }
    }

    public static ScenarioConfig defaultConfig(ScenarioMode mode) {
        return new ScenarioConfig(
                DEFAULT_DURATION_SECONDS,
                DEFAULT_WARMUP_SECONDS,
                DEFAULT_INTENSITY,
                DEFAULT_CONCURRENCY,
                mode,
                Collections.emptyMap()
        );
    }
}
