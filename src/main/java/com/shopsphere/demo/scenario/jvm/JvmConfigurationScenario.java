package com.shopsphere.demo.scenario.jvm;

import com.shopsphere.demo.scenario.BaseScenario;
import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.RuntimeMXBean;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class JvmConfigurationScenario extends BaseScenario {

    public static final String ID = "jvm-configuration";

    private final MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
    private final RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();

    public JvmConfigurationScenario() {
        super(
                ID,
                "JVM Configuration and Heap Sizing Comparison",
                "Demonstrates how memory allocation interacts with heap configuration (-Xms/-Xmx) and why tuning must be guided by JFR evidence.",
                "JVM metadata, heap occupancy, and GC pause events across different JVM configurations"
        );
    }

    @Override
    protected void executeFaulty(int stepIndex) {
        // Generates bursty objects that trigger GC under small heaps
        List<String> list = new ArrayList<>(1000);
        for (int i = 0; i < 1000; i++) {
            list.add("JVM-CONFIG-DATA-" + stepIndex + "-" + i + "-" + System.currentTimeMillis());
        }
        if (list.size() == -1) {
            log.trace("Unreachable");
        }
    }

    @Override
    protected void executeCorrected(int stepIndex) {
        // Minimal allocation
        long val = stepIndex * 31L;
        if (val == -1) {
            log.trace("Unreachable");
        }
    }

    @Override
    protected Map<String, Object> getCustomMetrics() {
        return Map.of(
                "jvmInputArguments", runtimeMXBean.getInputArguments(),
                "vmName", runtimeMXBean.getVmName(),
                "vmVersion", runtimeMXBean.getVmVersion(),
                "heapUsedBytes", memoryMXBean.getHeapMemoryUsage().getUsed(),
                "heapMaxBytes", memoryMXBean.getHeapMemoryUsage().getMax()
        );
    }
}
