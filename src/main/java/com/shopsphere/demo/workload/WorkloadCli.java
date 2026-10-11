package com.shopsphere.demo.workload;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.shopsphere.demo.ShopSphereApplication;
import com.shopsphere.demo.comparison.RunResult;
import com.shopsphere.demo.scenario.Scenario;
import com.shopsphere.demo.scenario.ScenarioConfig;
import com.shopsphere.demo.scenario.ScenarioMode;
import com.shopsphere.demo.scenario.ScenarioRegistry;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class WorkloadCli {

    public static void main(String[] args) {
        Map<String, String> params = parseArgs(args);

        String scenarioId = params.getOrDefault("scenario", "cpu-hotspot");
        ScenarioMode mode = ScenarioMode.valueOf(params.getOrDefault("mode", "FAULTY").toUpperCase());
        int duration = Integer.parseInt(params.getOrDefault("duration", "15"));
        int warmup = Integer.parseInt(params.getOrDefault("warmup", "3"));
        int concurrency = Integer.parseInt(params.getOrDefault("concurrency", "8"));
        int intensity = Integer.parseInt(params.getOrDefault("intensity", "100"));
        boolean recordJfr = Boolean.parseBoolean(params.getOrDefault("record-jfr", "true"));
        String outputPath = params.get("output");

        System.out.println("=================================================");
        System.out.println(" ShopSphere Workload Generator");
        System.out.printf(" Scenario: %s | Mode: %s | Duration: %ds | Concurrency: %d | JFR: %b%n",
                scenarioId, mode, duration, concurrency, recordJfr);
        System.out.println("=================================================");

        // Disable standard Spring banner and start minimal context
        System.setProperty("spring.main.banner-mode", "off");
        System.setProperty("server.port", "0"); // random unused port
        ConfigurableApplicationContext context = SpringApplication.run(ShopSphereApplication.class);

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
            if (arg.startsWith("--") && arg.contains("=")) {
                String[] parts = arg.substring(2).split("=", 2);
                map.put(parts[0].trim().toLowerCase(), parts[1].trim());
            }
        }
        return map;
    }
}
