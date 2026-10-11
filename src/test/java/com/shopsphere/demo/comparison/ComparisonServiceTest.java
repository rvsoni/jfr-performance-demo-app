package com.shopsphere.demo.comparison;

import com.shopsphere.demo.scenario.ScenarioMode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ComparisonServiceTest {

    @Test
    void testRunComparisonCalculation() {
        RunResult before = new RunResult(
                "cpu-hotspot",
                ScenarioMode.FAULTY,
                20,
                3,
                8,
                100,
                1000,
                1000,
                0,
                50.0,
                20.0,
                35.0,
                50.0,
                22.0,
                75.0,
                "cpu-hotspot-faulty.jfr",
                "21.0.1",
                List.of("-Xmx512m"),
                Instant.now(),
                Map.of()
        );

        RunResult after = new RunResult(
                "cpu-hotspot",
                ScenarioMode.CORRECTED,
                20,
                3,
                8,
                100,
                10000,
                10000,
                0,
                500.0,
                2.0,
                3.5,
                5.0,
                2.2,
                10.0,
                "cpu-hotspot-corrected.jfr",
                "21.0.1",
                List.of("-Xmx512m"),
                Instant.now(),
                Map.of()
        );

        RunComparison comparison = RunComparison.compare(before, after);

        assertThat(comparison.throughputDeltaPercent()).isGreaterThan(0.0);
        assertThat(comparison.p50LatencyDeltaPercent()).isLessThan(0.0);
        assertThat(comparison.comparabilityWarnings()).isEmpty();
        assertThat(comparison.markdownReport()).contains("Improvement");
    }

    @Test
    void testSerializationAndDeserialization(@TempDir Path tempDir) throws Exception {
        RunComparisonService service = new RunComparisonService();
        RunResult result = new RunResult(
                "cpu-hotspot",
                ScenarioMode.FAULTY,
                20,
                3,
                8,
                100,
                500,
                500,
                0,
                25.0,
                40.0,
                60.0,
                80.0,
                42.0,
                120.0,
                "rec.jfr",
                "21.0.1",
                List.of("-Xmx512m"),
                Instant.now(),
                Map.of("key", "val")
        );

        Path jsonPath = tempDir.resolve("result.json");
        service.saveResult(result, jsonPath);

        RunResult loaded = service.loadResult(jsonPath);
        assertThat(loaded.scenarioId()).isEqualTo(result.scenarioId());
        assertThat(loaded.throughputRps()).isEqualTo(result.throughputRps());
    }
}
