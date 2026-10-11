package com.shopsphere.demo.scenario.allocation;

import com.shopsphere.demo.scenario.BaseScenario;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class GcPressureScenario extends BaseScenario {

    public static final String ID = "gc-pressure";

    public GcPressureScenario() {
        super(
                ID,
                "Garbage Collection Pressure from High Allocation Churn",
                "Demonstrates how high-volume short-lived object allocations trigger frequent GC pause events and affect latency.",
                "jdk.GarbageCollection and jdk.GCPhasePause events correlated with high allocation"
        );
    }

    @Override
    protected void executeFaulty(int stepIndex) {
        // Generates hundreds of thousands of short-lived objects per step
        int batchSize = 2_000;
        List<byte[]> buffers = new ArrayList<>(batchSize);
        for (int i = 0; i < batchSize; i++) {
            byte[] buf = new byte[1024]; // 1 KB buffer
            buf[0] = (byte) (stepIndex + i);
            buffers.add(buf);
        }
        // Consume buffers
        long sum = 0;
        for (byte[] b : buffers) {
            sum += b[0];
        }
        if (sum == -999999) {
            log.trace("Checksum: {}", sum);
        }
    }

    @Override
    protected void executeCorrected(int stepIndex) {
        // Reuses a small fixed buffer per thread instead of allocating 2000 new byte arrays
        byte[] reusable = new byte[1024];
        long sum = 0;
        for (int i = 0; i < 2_000; i++) {
            reusable[0] = (byte) (stepIndex + i);
            sum += reusable[0];
        }
        if (sum == -999999) {
            log.trace("Checksum: {}", sum);
        }
    }
}
