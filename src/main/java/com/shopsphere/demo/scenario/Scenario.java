package com.shopsphere.demo.scenario;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface Scenario {

    String getId();

    String getName();

    String getDescription();

    String getJfrSignature();

    ScenarioStatus getStatus();

    ScenarioMode getMode();

    void setMode(ScenarioMode mode);

    ScenarioInfo getInfo();

    CompletableFuture<ScenarioExecutionResult> start(ScenarioConfig config);

    void stop();

    void executeWorkloadStep(int stepIndex, ScenarioMode mode) throws Exception;

    Map<String, Object> getMetrics();
}
