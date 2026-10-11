package com.shopsphere.demo.controller;

import com.shopsphere.demo.scenario.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/scenarios")
public class ScenarioController {

    private final ScenarioRegistry scenarioRegistry;

    public ScenarioController(ScenarioRegistry scenarioRegistry) {
        this.scenarioRegistry = scenarioRegistry;
    }

    @GetMapping
    public List<ScenarioInfo> listScenarios() {
        return scenarioRegistry.getAllScenarios();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ScenarioInfo> getScenario(@PathVariable String id) {
        return scenarioRegistry.getScenario(id)
                .map(s -> ResponseEntity.ok(s.getInfo()))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/active")
    public ResponseEntity<?> getActiveScenario() {
        return scenarioRegistry.getActiveScenario()
                .map(s -> ResponseEntity.ok((Object) s.getInfo()))
                .orElseGet(() -> ResponseEntity.ok(Map.of("active", false, "message", "No scenario is currently active")));
    }

    @PostMapping("/{id}/start")
    public ResponseEntity<?> startScenario(
            @PathVariable String id,
            @RequestBody(required = false) ScenarioConfig customConfig,
            @RequestParam(required = false) ScenarioMode mode,
            @RequestParam(required = false, defaultValue = "20") Integer durationSeconds,
            @RequestParam(required = false, defaultValue = "3") Integer warmupSeconds,
            @RequestParam(required = false, defaultValue = "10") Integer intensity,
            @RequestParam(required = false, defaultValue = "8") Integer concurrency
    ) {
        ScenarioConfig config;
        if (customConfig != null) {
            config = customConfig;
        } else {
            ScenarioMode selectedMode = mode != null ? mode : ScenarioMode.FAULTY;
            config = new ScenarioConfig(durationSeconds, warmupSeconds, intensity, concurrency, selectedMode, Map.of());
        }

        try {
            scenarioRegistry.startScenario(id, config);
            return ResponseEntity.accepted().body(Map.of(
                    "status", "STARTED",
                    "scenarioId", id,
                    "mode", config.mode().name(),
                    "durationSeconds", config.durationSeconds(),
                    "concurrency", config.concurrency()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/stop")
    public ResponseEntity<Map<String, String>> stopScenario(@PathVariable String id) {
        scenarioRegistry.stopScenario(id);
        return ResponseEntity.ok(Map.of("status", "STOPPED", "scenarioId", id));
    }

    @PostMapping("/stop-all")
    public ResponseEntity<Map<String, String>> stopAll() {
        scenarioRegistry.stopAll();
        return ResponseEntity.ok(Map.of("status", "ALL_STOPPED"));
    }

    @PostMapping("/{id}/execute-step")
    public ResponseEntity<?> executeStep(
            @PathVariable String id,
            @RequestParam(required = false, defaultValue = "0") int stepIndex,
            @RequestParam(required = false) ScenarioMode mode
    ) {
        var opt = scenarioRegistry.getScenario(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Scenario scenario = opt.get();
        ScenarioMode targetMode = mode != null ? mode : scenario.getMode();
        try {
            scenario.executeWorkloadStep(stepIndex, targetMode);
            return ResponseEntity.ok(Map.of("scenarioId", id, "mode", targetMode.name(), "stepIndex", stepIndex, "status", "SUCCESS"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("scenarioId", id, "mode", targetMode.name(), "stepIndex", stepIndex, "error", e.getMessage()));
        }
    }
}
