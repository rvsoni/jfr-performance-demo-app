# ShopSphere Scenario Catalog

This catalog documents all 10 performance scenarios implemented in `jfr-performance-demo-app`. Each scenario reproduces a distinct JVM performance pathology, emits genuine JFR runtime events, and provides a corresponding corrected implementation.

---

## 1. Scenario 1: CPU Hotspot (`cpu-hotspot`)

- **Identifier:** `cpu-hotspot`
- **Component:** `com.shopsphere.demo.scenario.cpu.CpuHotspotScenario`
- **Incident Description:** High CPU consumption caused by redundant, unmemoized polynomial discount calculations inside pricing methods.

### Root Cause
During price calculation for each line item, the faulty implementation calls `calculateComplexDiscountMatrix()`, which executes 120,000 iterations of polynomial and trigonometric calculations (`Math.sin`, `Math.cos`, `Math.sqrt`) per order request.

### How to Reproduce
```bash
# Faulty run
java -jar target/jfr-performance-demo-app-1.0.0-SNAPSHOT.jar --scenario=cpu-hotspot --mode=FAULTY --duration=15 --concurrency=8

# Corrected run
java -jar target/jfr-performance-demo-app-1.0.0-SNAPSHOT.jar --scenario=cpu-hotspot --mode=CORRECTED --duration=15 --concurrency=8
```

### Expected JFR Evidence
- **Event:** `jdk.ExecutionSample`
- **Method:** `com.shopsphere.demo.scenario.cpu.CpuHotspotScenario.calculateComplexDiscountMatrix`
- **Thread State:** `STATE_RUNNABLE` (consuming CPU)
- Execution samples will heavily concentrate in the trigonometric loop.

### How to Inspect in JMC
1. Open the recording in **JDK Mission Control**.
2. Navigate to **Method Profiling** -> **Call Tree** or **Hot Methods**.
3. Group by `Method` and locate `CpuHotspotScenario.calculateComplexDiscountMatrix`.
4. Observe the sample count and call path.

### MCP Tools to Call
- `analyze_cpu(recordingPath)`
- `get_hot_methods(recordingPath, topN=10)`
- `get_call_tree(recordingPath, method="calculateComplexDiscountMatrix")`

### Code Correction
Replace the loop with the O(1) closed-form calculation:
```java
// Corrected constant-time calculation
public double calculateClosedFormDiscount(String customerId, double subtotal, int itemCount) {
    double baseRate = 0.30;
    return subtotal * baseRate;
}
```

---

## 2. Scenario 2: Excessive Object Allocation (`allocation-pressure`)

- **Identifier:** `allocation-pressure`
- **Component:** `com.shopsphere.demo.scenario.allocation.AllocationPressureScenario`
- **Incident Description:** High allocation pressure and memory churn from redundant intermediate collections, repetitive string conversions, and throwaway DTOs.

### Root Cause
In `generateInefficientSummary()`, each order creates intermediate `HashMap` instances, converts strings to `byte[]` copies, wraps primitives into `Integer`, and performs iterative String concatenation with `+` in loops.

### How to Reproduce
```bash
./scripts/run-demo.sh allocation-pressure 10 4
```

### Expected JFR Evidence
- **Event:** `jdk.ObjectAllocationSample`
- **Allocated Classes:** `java.lang.String`, `byte[]`, `java.util.HashMap$Node`, `java.util.ArrayList`
- **Stack Trace:** Leading to `AllocationPressureScenario.generateInefficientSummary`

### How to Inspect in JMC
1. Open recording in JMC -> **Memory** -> **Allocations**.
2. Inspect **Allocation by Class** and **Allocation Call Tree**.

### MCP Tools to Call
- `analyze_memory(recordingPath)`
- `get_top_allocations(recordingPath)`

### Code Correction
Use `StringBuilder` and direct primitive aggregation without intermediate map allocations.

---

## 3. Scenario 3: Garbage Collection Pressure (`gc-pressure`)

- **Identifier:** `gc-pressure`
- **Component:** `com.shopsphere.demo.scenario.allocation.GcPressureScenario`
- **Incident Description:** Elevated garbage collection activity and pause times caused by burst object allocations.

### Root Cause
Allocates thousands of 1 KB byte arrays per transaction step, rapidly filling Eden space and triggering frequent Young Generation GCs.

### Expected JFR Evidence
- **Events:** `jdk.GarbageCollection`, `jdk.GCPhasePause`, `jdk.ObjectAllocationSample`
- Elevated GC frequency, pause durations, and heap occupancy shifts.

### How to Inspect in JMC
1. Open JMC -> **JVM Internals** -> **Garbage Collections**.
2. Review pause times, GC intervals, and collection phases.

### Code Correction
Reuse preallocated per-thread buffers rather than allocating new arrays per request.

---

## 4. Scenario 4: Thread Contention (`thread-contention`)

- **Identifier:** `thread-contention`
- **Component:** `com.shopsphere.demo.scenario.contention.ThreadContentionScenario`
- **Incident Description:** Severe monitor contention and latency spikes on a shared inventory monitor lock.

### Root Cause
`InventoryService.reserveWithHeavyLock()` acquires a single `globalMonitor` lock across all products and holds it while performing unnecessary checksum calculations and processing delays. Concurrent worker threads block in monitor entry queues.

### How to Reproduce
```bash
./scripts/run-demo.sh thread-contention 15 8
```

### Expected JFR Evidence
- **Event:** `jdk.JavaMonitorEnter`
- **Monitor Class:** `java.lang.Object` (`InventoryService.globalMonitor`)
- **Wait Duration:** Spikes up to 50ms-150ms per contested entry.
- **Thread State:** `BLOCKED`

### How to Inspect in JMC
1. Open JMC -> **Threads** -> **Lock Instances** / **Contention**.
2. Identify the contested monitor and threads waiting in `InventoryService.reserveWithHeavyLock`.

### MCP Tools to Call
- `analyze_threads(recordingPath)`
- `get_lock_contention(recordingPath)`

### Code Correction
Switch to fine-grained atomic CAS operations (`AtomicInteger.compareAndSet`) per product inventory item.

---

## 5. Scenario 5: Thread Pool Starvation (`thread-pool-starvation`)

- **Identifier:** `thread-pool-starvation`
- **Component:** `com.shopsphere.demo.scenario.threadpool.ThreadPoolStarvationScenario`
- **Incident Description:** Tasks waiting in bounded executor queues due to an undersized worker pool.

### Root Cause
Work is submitted to a 2-worker `ThreadPoolExecutor` while concurrent requests exceed worker processing capacity.

### Expected JFR Evidence
- **Event:** `jdk.ThreadPark`
- Threads blocked on `Future.get()` waiting for available workers; queue depth elevated.

### Code Correction
Properly dimension the worker thread pool or use virtual threads / non-blocking execution.

---

## 6. Scenario 6: Slow I/O (`slow-io`)

- **Identifier:** `slow-io`
- **Component:** `com.shopsphere.demo.scenario.io.SlowIoScenario`
- **Incident Description:** Request latency dominated by waiting for downstream partner payment/warehouse API delays.

### Root Cause
Synchronous blocking calls to downstream services with long latency (40ms) with low JVM CPU activity.

### Expected JFR Evidence
- Thread state in `TIMED_WAITING` / `ThreadPark` during `DownstreamSimulator` calls; CPU sample count is minimal.

---

## 7. Scenario 7: Exception Storm (`exception-storm`)

- **Identifier:** `exception-storm`
- **Component:** `com.shopsphere.demo.scenario.exception.ExceptionStormScenario`
- **Incident Description:** Performance overhead caused by using exceptions for normal validation control flow.

### Root Cause
Throws and catches `OrderValidationException` repeatedly during routine validation loops, incurring stack trace generation cost (`Throwable.fillInStackTrace`).

### Expected JFR Evidence
- **Event:** `jdk.JavaExceptionThrow`
- High count of thrown exceptions and CPU samples inside exception creation.

### Code Correction
Use explicit validation return types (`ValidationResult`) without throwing exceptions for standard business conditions.

---

## 8. Scenario 8: Serialization Overhead (`serialization-overhead`)

- **Identifier:** `serialization-overhead`
- **Component:** `com.shopsphere.demo.scenario.serialization.SerializationOverheadScenario`
- **Incident Description:** Excessive CPU and allocation overhead caused by redundant Jackson JSON serialization and deserialization roundtrips.

### Root Cause
Converts objects to JSON string, parses to `JsonNode`, writes to `byte[]`, reads into `Map`, and serializes again for a single response.

### Expected JFR Evidence
- `jdk.ExecutionSample` and `jdk.ObjectAllocationSample` concentrated in `com.fasterxml.jackson.databind` methods.

### Code Correction
Direct single-pass object serialization.

---

## 9. Scenario 9: Lock and I/O (`lock-and-io`)

- **Identifier:** `lock-and-io`
- **Component:** `com.shopsphere.demo.scenario.contention.LockAndIoScenario`
- **Incident Description:** Shared monitor lock held while waiting for downstream I/O network operations.

### Root Cause
Lock held across a 20ms downstream network simulation, blocking unrelated concurrent transactions.

### Code Correction
Move the downstream network call outside the synchronized block and hold the lock only for in-memory state updates.

---

## 10. Scenario 10: JVM Configuration (`jvm-configuration`)

- **Identifier:** `jvm-configuration`
- **Component:** `com.shopsphere.demo.scenario.jvm.JvmConfigurationScenario`
- **Incident Description:** Demonstrates GC behavior and memory pool dynamics under constrained vs properly configured heap settings.

### Expected JFR Evidence
- JVM arguments, memory pool commit/usage curves, and GC phase pause metrics.
