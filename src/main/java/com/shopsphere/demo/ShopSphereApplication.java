package com.shopsphere.demo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.shopsphere.demo.comparison.ComparisonCli;
import com.shopsphere.demo.comparison.RunResult;
import com.shopsphere.demo.scenario.Scenario;
import com.shopsphere.demo.scenario.ScenarioConfig;
import com.shopsphere.demo.scenario.ScenarioMode;
import com.shopsphere.demo.scenario.ScenarioRegistry;
import com.shopsphere.demo.workload.WorkloadGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.File;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

@SpringBootApplication
public class ShopSphereApplication {

    public static void main(String[] args) {
        Map<String, String> params = parseArgs(args);

        if (params.containsKey("compare")) {
            ComparisonCli.main(args);
            return;
        }

        if (params.containsKey("scenario")) {
            runWorkloadMode(args, params);
            return;
        }

        // Standard Server Mode
        SpringApplication.run(ShopSphereApplication.class, args);
    }

    private static void runWorkloadMode(String[] args, Map<String, String> params) {
        String scenarioId = params.getOrDefault("scenario", "cpu-hotspot");
        ScenarioMode mode = ScenarioMode.valueOf(params.getOrDefault("mode", "FAULTY").toUpperCase());
        int duration = Integer.parseInt(params.getOrDefault("duration", "20"));
        int warmup = Integer.parseInt(params.getOrDefault("warmup", "3"));
        int concurrency = Integer.parseInt(params.getOrDefault("concurrency", "8"));
        int intensity = Integer.parseInt(params.getOrDefault("intensity", "100"));
        boolean recordJfr = Boolean.parseBoolean(params.getOrDefault("record-jfr", "true"));
        String outputPath = params.get("output");

        System.out.println("=================================================");
        System.out.println(" ShopSphere Workload Generator (Standalone Mode)");
        System.out.printf(" Scenario: %s | Mode: %s | Duration: %ds | Concurrency: %d | JFR: %b%n",
                scenarioId, mode, duration, concurrency, recordJfr);
        System.out.println("=================================================");

        SpringApplication app = new SpringApplication(ShopSphereApplication.class);
        app.setWebApplicationType(WebApplicationType.NONE);
        ConfigurableApplicationContext context = app.run(args);

        try {
            ScenarioRegistry registry = context.getBean(ScenarioRegistry.class);
            WorkloadGenerator generator = context.getBean(WorkloadGenerator.class);

            Scenario scenario = registry.getScenario(scenarioId)
                    .orElseThrow(() -> new IllegalArgumentException("Unknown scenario: " + scenarioId));

            ScenarioConfig config = new ScenarioConfig(duration, warmup, intensity, concurrency, mode, Map.of());
            RunResult result = generator.runScenarioWorkload(scenario, config, recordJfr);

            System.out.println("\n--- Execution Summary ---");
            System.out.printf("Throughput:   %.2f req/s%n", result.throughputRps());
            System.out.printf("P50 Latency:  %.2f ms%n", result.p50LatencyMs());
            System.out.printf("P95 Latency:  %.2f ms%n", result.p95LatencyMs());
            System.out.printf("P99 Latency:  %.2f ms%n", result.p99LatencyMs());
            System.out.printf("Mean Latency: %.2f ms%n", result.meanLatencyMs());
            System.out.printf("Total Reqs:   %d (Failed: %d)%n", result.totalRequests(), result.failedRequests());
            System.out.printf("JFR File:     %s%n", result.jfrRecordingFile());

            if (outputPath != null) {
                ObjectMapper mapper = new ObjectMapper();
                mapper.registerModule(new JavaTimeModule());
                mapper.enable(SerializationFeature.INDENT_OUTPUT);
                File file = Paths.get(outputPath).toFile();
                if (file.getParentFile() != null) {
                    file.getParentFile().mkdirs();
                }
                mapper.writeValue(file, result);
                System.out.printf("Results saved to: %s%n", outputPath);
            }
        } catch (Exception e) {
            System.err.println("Execution failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        } finally {
            context.close();
        }
    }

    private static Map<String, String> parseArgs(String[] args) {
        Map<String, String> map = new HashMap<>();
        for (String arg : args) {
            if (arg.startsWith("--")) {
                if (arg.contains("=")) {
                    String[] parts = arg.substring(2).split("=", 2);
                    map.put(parts[0].trim().toLowerCase(), parts[1].trim());
                } else {
                    map.put(arg.substring(2).trim().toLowerCase(), "true");
                }
            }
        }
        return map;
    }
}
