package com.shopsphere.demo.scenario;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopsphere.demo.scenario.contention.LockAndIoScenario;
import com.shopsphere.demo.scenario.exception.ExceptionStormScenario;
import com.shopsphere.demo.scenario.io.SlowIoScenario;
import com.shopsphere.demo.scenario.jvm.JvmConfigurationScenario;
import com.shopsphere.demo.scenario.serialization.SerializationOverheadScenario;
import com.shopsphere.demo.scenario.threadpool.ThreadPoolStarvationScenario;
import com.shopsphere.demo.service.DownstreamSimulator;
import com.shopsphere.demo.service.InventoryService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AdditionalScenariosTest {

    @Test
    void testThreadPoolStarvation() throws Exception {
        ThreadPoolStarvationScenario scenario = new ThreadPoolStarvationScenario();
        scenario.executeWorkloadStep(1, ScenarioMode.FAULTY);
        scenario.executeWorkloadStep(2, ScenarioMode.CORRECTED);
        assertThat(scenario.getId()).isEqualTo("thread-pool-starvation");
    }

    @Test
    void testSlowIo() throws Exception {
        SlowIoScenario scenario = new SlowIoScenario(new DownstreamSimulator());
        scenario.executeWorkloadStep(1, ScenarioMode.CORRECTED);
        assertThat(scenario.getId()).isEqualTo("slow-io");
    }

    @Test
    void testExceptionStorm() throws Exception {
        ExceptionStormScenario scenario = new ExceptionStormScenario();
        scenario.executeWorkloadStep(1, ScenarioMode.FAULTY);
        scenario.executeWorkloadStep(2, ScenarioMode.CORRECTED);
        assertThat(scenario.getId()).isEqualTo("exception-storm");
    }

    @Test
    void testSerializationOverhead() throws Exception {
        SerializationOverheadScenario scenario = new SerializationOverheadScenario(new ObjectMapper());
        scenario.executeWorkloadStep(1, ScenarioMode.FAULTY);
        scenario.executeWorkloadStep(2, ScenarioMode.CORRECTED);
        assertThat(scenario.getId()).isEqualTo("serialization-overhead");
    }

    @Test
    void testLockAndIo() throws Exception {
        InventoryService inventoryService = new InventoryService();
        inventoryService.init();
        LockAndIoScenario scenario = new LockAndIoScenario(inventoryService, new DownstreamSimulator());
        scenario.executeWorkloadStep(1, ScenarioMode.CORRECTED);
        assertThat(scenario.getId()).isEqualTo("lock-and-io");
    }

    @Test
    void testJvmConfiguration() throws Exception {
        JvmConfigurationScenario scenario = new JvmConfigurationScenario();
        scenario.executeWorkloadStep(1, ScenarioMode.FAULTY);
        scenario.executeWorkloadStep(2, ScenarioMode.CORRECTED);
        assertThat(scenario.getId()).isEqualTo("jvm-configuration");
    }
}
