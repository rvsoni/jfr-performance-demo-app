package com.shopsphere.demo.scenario.io;

import com.shopsphere.demo.scenario.BaseScenario;
import com.shopsphere.demo.service.DownstreamSimulator;
import org.springframework.stereotype.Component;

@Component
public class SlowIoScenario extends BaseScenario {

    public static final String ID = "slow-io";

    private final DownstreamSimulator downstreamSimulator;

    public SlowIoScenario(DownstreamSimulator downstreamSimulator) {
        super(
                ID,
                "Slow Downstream I/O Latency",
                "Demonstrates application request latency caused by synchronous downstream service delays without high CPU usage.",
                "Thread waiting/parked state during com.shopsphere.demo.service.DownstreamSimulator calls with low CPU usage"
        );
        this.downstreamSimulator = downstreamSimulator;
    }

    @Override
    protected void executeFaulty(int stepIndex) {
        // Simulates a slow 40ms downstream partner API call
        downstreamSimulator.callPaymentGateway("CUST-" + stepIndex, 99.50, 40);
    }

    @Override
    protected void executeCorrected(int stepIndex) {
        // Optimized downstream interaction with 2ms response time
        downstreamSimulator.callPaymentGateway("CUST-" + stepIndex, 99.50, 2);
    }
}
