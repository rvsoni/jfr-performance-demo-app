package com.shopsphere.demo.workload;

import org.HdrHistogram.Histogram;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

public class LatencyTracker {

    private final Histogram histogram;
    private final LongAdder totalRequests = new LongAdder();
    private final LongAdder successfulRequests = new LongAdder();
    private final LongAdder failedRequests = new LongAdder();

    public LatencyTracker() {
        // Track latencies from 1 microsecond to 1 hour with 3 significant digits
        this.histogram = new Histogram(TimeUnit.MICROSECONDS.toNanos(1), TimeUnit.HOURS.toNanos(1), 3);
    }

    public synchronized void recordDurationNanos(long durationNanos, boolean success) {
        totalRequests.increment();
        if (success) {
            successfulRequests.increment();
        } else {
            failedRequests.increment();
        }
        if (durationNanos > 0) {
            histogram.recordValue(durationNanos);
        }
    }

    public long getTotalRequests() {
        return totalRequests.sum();
    }

    public long getSuccessfulRequests() {
        return successfulRequests.sum();
    }

    public long getFailedRequests() {
        return failedRequests.sum();
    }

    public synchronized double getP50Ms() {
        return nanosToMs(histogram.getValueAtPercentile(50.0));
    }

    public synchronized double getP95Ms() {
        return nanosToMs(histogram.getValueAtPercentile(95.0));
    }

    public synchronized double getP99Ms() {
        return nanosToMs(histogram.getValueAtPercentile(99.0));
    }

    public synchronized double getMeanMs() {
        return nanosToMs((long) histogram.getMean());
    }

    public synchronized double getMaxMs() {
        return nanosToMs(histogram.getMaxValue());
    }

    public synchronized void reset() {
        histogram.reset();
        totalRequests.reset();
        successfulRequests.reset();
        failedRequests.reset();
    }

    private double nanosToMs(long nanos) {
        return nanos / 1_000_000.0;
    }
}
