package com.shopsphere.demo.scenario.threadpool;

import com.shopsphere.demo.scenario.BaseScenario;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.locks.LockSupport;

@Component
public class ThreadPoolStarvationScenario extends BaseScenario {

    public static final String ID = "thread-pool-starvation";

    private final ThreadPoolExecutor undersizedExecutor;
    private final ThreadPoolExecutor wellSizedExecutor;

    public ThreadPoolStarvationScenario() {
        super(
                ID,
                "Thread Pool Starvation and Task Queueing",
                "Demonstrates queue backup and task starvation when tasks are dispatched to an undersized thread pool.",
                "jdk.ThreadPark events and high task queue latency in ThreadPoolExecutor"
        );

        this.undersizedExecutor = new ThreadPoolExecutor(
                2, 2, 0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(500),
                r -> new Thread(r, "starved-worker")
        );

        this.wellSizedExecutor = new ThreadPoolExecutor(
                16, 16, 0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(500),
                r -> new Thread(r, "wellsized-worker")
        );
    }

    @Override
    protected void executeFaulty(int stepIndex) throws Exception {
        // Submit task to 2-worker executor
        Future<String> future = undersizedExecutor.submit(() -> {
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(15));
            return "Result-" + stepIndex;
        });
        future.get(5, TimeUnit.SECONDS);
    }

    @Override
    protected void executeCorrected(int stepIndex) throws Exception {
        // Submit task to properly sized 16-worker executor
        Future<String> future = wellSizedExecutor.submit(() -> {
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(15));
            return "Result-" + stepIndex;
        });
        future.get(5, TimeUnit.SECONDS);
    }

    @Override
    protected Map<String, Object> getCustomMetrics() {
        return Map.of(
                "undersizedQueueDepth", undersizedExecutor.getQueue().size(),
                "undersizedActiveWorkers", undersizedExecutor.getActiveCount(),
                "wellSizedQueueDepth", wellSizedExecutor.getQueue().size(),
                "wellSizedActiveWorkers", wellSizedExecutor.getActiveCount()
        );
    }
}
