# Java Flight Recorder (JFR) Events Reference

This guide details the JFR event types utilized across the demo scenarios and explains how to interpret runtime evidence.

---

## Key JFR Events & Signatures

| Event Identifier | Category | Sampling / Threshold | What It Measures | Relevant Scenarios |
| :--- | :--- | :--- | :--- | :--- |
| `jdk.ExecutionSample` | Java Application | Sampled (10ms-20ms) | CPU execution locations in Java code | `cpu-hotspot`, `serialization-overhead`, `exception-storm` |
| `jdk.JavaMonitorEnter` | Java Application | Threshold (>1ms) | Thread blocked waiting on `synchronized` monitor | `thread-contention`, `lock-and-io` |
| `jdk.ObjectAllocationSample` | Java Application | Throttle / Sampled | Object allocation stack traces and class names | `allocation-pressure`, `gc-pressure`, `serialization-overhead` |
| `jdk.GarbageCollection` | JVM Internals | All events | GC pauses, collector type, cause | `gc-pressure`, `jvm-configuration` |
| `jdk.GCPhasePause` | JVM Internals | All events | Detailed sub-phases of garbage collection | `gc-pressure` |
| `jdk.ThreadPark` | Java Application | Threshold (>10ms) | Threads parked waiting on locks/executors/conditions | `thread-pool-starvation`, `slow-io` |
| `jdk.JavaExceptionThrow` | Java Application | All / Sampled | Exceptions instantiated and thrown | `exception-storm` |
| `jdk.SocketRead` / `jdk.SocketWrite` | Java Application | Threshold (>20ms) | Network socket I/O duration | `slow-io` |

---

## Critical Rules for Evidence Interpretation

1. **Sampled Events vs. Exact Counters:**
   - `jdk.ExecutionSample` is a *statistical sample* of running threads taken at regular intervals. A method with 80% of samples was found running in 80% of samples; this strongly correlates with CPU usage but is not a direct measure of CPU nanoseconds.
   - `jdk.ObjectAllocationSample` samples object allocation at TLAB boundaries or large allocations; it shows dominant allocating code paths without tracking every individual byte.

2. **Thresholded Events:**
   - `jdk.JavaMonitorEnter` and `jdk.ThreadPark` only record when wait time exceeds the configured threshold (e.g. 1ms or 10ms). Fast locks with zero contention will produce zero events.

3. **Distinguishing Bottlenecks:**
   - High latency with **high CPU samples** in application code -> CPU Hotspot or Serialization overhead.
   - High latency with **low CPU samples** and **JavaMonitorEnter events** -> Thread / Lock Contention.
   - High latency with **low CPU samples** and **ThreadPark events** -> Downstream I/O or Thread Pool Starvation.
   - High CPU with **frequent GCPhasePause events** -> GC Churn / Memory Pressure.
