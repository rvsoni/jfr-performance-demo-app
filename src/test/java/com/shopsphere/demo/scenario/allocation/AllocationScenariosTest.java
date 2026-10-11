package com.shopsphere.demo.scenario.allocation;

import com.shopsphere.demo.scenario.ScenarioMode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AllocationScenariosTest {

    @Test
    void testAllocationPressureScenario() throws Exception {
        AllocationPressureScenario scenario = new AllocationPressureScenario();
        scenario.executeWorkloadStep(1, ScenarioMode.FAULTY);
        scenario.executeWorkloadStep(2, ScenarioMode.CORRECTED);
        assertThat(scenario.getId()).isEqualTo("allocation-pressure");
    }

    @Test
    void testGcPressureScenario() throws Exception {
        GcPressureScenario scenario = new GcPressureScenario();
        scenario.executeWorkloadStep(1, ScenarioMode.FAULTY);
        scenario.executeWorkloadStep(2, ScenarioMode.CORRECTED);
        assertThat(scenario.getId()).isEqualTo("gc-pressure");
    }
}
