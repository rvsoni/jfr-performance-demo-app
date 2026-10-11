package com.shopsphere.demo.scenario;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ScenarioRegistry {

    private static final Logger log = LoggerFactory.getLogger(ScenarioRegistry.class);

    private final Map<String, Scenario> scenarios = new ConcurrentHashMap<>();

    public ScenarioRegistry(List<Scenario> scenarioBeans) {
        for (Scenario scenario : scenarioBeans) {
            scenarios.put(scenario.getId(), scenario);
            log.info("Registered scenario: '{}' - {}", scenario.getId(), scenario.getName());
        }
    }

    public List<ScenarioInfo> getAllScenarios() {
        return scenarios.values().stream()
                .map(Scenario::getInfo)
                .sorted(Comparator.comparing(ScenarioInfo::id))
                .toList();
    }

    public Optional<Scenario> getScenario(String id) {
        return Optional.ofNullable(scenarios.get(id));
    }

    public Optional<Scenario> getActiveScenario() {
        return scenarios.values().stream()
                .filter(s -> s.getStatus() == ScenarioStatus.WARMING_UP || s.getStatus() == ScenarioStatus.MEASURING)
                .findFirst();
    }

    public synchronized CompletableFuture<ScenarioExecutionResult> startScenario(String id, ScenarioConfig config) {
        Scenario scenario = scenarios.get(id);
        if (scenario == null) {
            throw new IllegalArgumentException("Unknown scenario: " + id);
        }

        Optional<Scenario> active = getActiveScenario();
        if (active.isPresent() && !active.get().getId().equals(id)) {
            throw new IllegalStateException("Another scenario is currently active: '" + active.get().getId() + "'. Stop it before starting a new one.");
        }

        return scenario.start(config);
    }

    public void stopScenario(String id) {
        Scenario scenario = scenarios.get(id);
        if (scenario != null) {
            scenario.stop();
        }
    }

    public void stopAll() {
        scenarios.values().forEach(Scenario::stop);
    }
}
