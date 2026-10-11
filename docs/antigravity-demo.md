# Antigravity Demonstration Guide

This guide walks through using **Google Antigravity** paired with `jfr-performance-mcp` to investigate JVM performance bottlenecks captured from `jfr-performance-demo-app`.

---

## Conference Demonstration Workflow

```
+---------------------------+       +-------------------------+       +-------------------------+
|   ShopSphere Demo App     | ----> |     JFR Recording       | ----> |   jfr-performance-mcp   |
| (Trigger faulty workload) |       | (cpu-hotspot-faulty.jfr)|       |    (MCP Server API)     |
+---------------------------+       +-------------------------+       +-------------------------+
                                                                                   |
                                                                                   v
+---------------------------+       +-------------------------+       +-------------------------+
|    Verify in JMC GUI      | <---> |   Antigravity AI Agent  | <==== |   MCP Tool Responses    |
| (Manual Cross-Check)      |       | (Root Cause Analysis)   |       |  (Hot methods, Locks)   |
+---------------------------+       +-------------------------+       +-------------------------+
```

---

## Demo A: CPU Hotspot Investigation

### Step 1: Run Faulty Workload
```bash
./scripts/run-demo.sh cpu-hotspot 15 8
```
This produces:
- `recordings/cpu-hotspot-faulty-<timestamp>.jfr`
- `recordings/cpu-hotspot-corrected-<timestamp>.jfr`
- `results/cpu-hotspot-comparison.md`

### Step 2: Ask Antigravity AI
Give Antigravity the following prompt in the chat:

> **Prompt:**
> "Analyze the recording `recordings/cpu-hotspot-faulty-<timestamp>.jfr` using the JFR performance MCP tools. Identify the strongest CPU-related evidence, distinguish sample percentages from actual CPU time, and point me to the application methods that warrant investigation."

### Expected AI Investigation Path
1. Antigravity calls the MCP tool to analyze CPU execution samples.
2. It identifies `com.shopsphere.demo.scenario.cpu.CpuHotspotScenario.calculateComplexDiscountMatrix` as dominating execution samples.
3. It explains that execution samples reflect thread program counter positions at sampling intervals rather than continuous microsecond measurements.
4. It directs the developer to the loop in `calculateComplexDiscountMatrix` where trigonometric polynomials are repeatedly evaluated.
5. It suggests replacing the loop with a closed-form constant-time formula (`calculateClosedFormDiscount`).

---

## Demo B: Thread Contention Investigation

### Step 1: Run Contention Workload
```bash
./scripts/run-demo.sh thread-contention 15 8
```

### Step 2: Ask Antigravity AI

> **Prompt:**
> "Analyze `recordings/thread-contention-faulty-<timestamp>.jfr`. Find evidence of monitor contention, identify the relevant application classes and stack traces, and explain which conclusions are directly supported by JFR."

### Expected AI Investigation Path
1. Antigravity calls the thread contention MCP tool.
2. It detects `jdk.JavaMonitorEnter` events on `InventoryService.globalMonitor`.
3. It notes that threads are spending substantial time in `BLOCKED` state attempting to enter `reserveWithHeavyLock`.
4. It identifies the root cause: a single coarse lock held over long checksum computation and artificial delays.
5. It recommends switching to lock-free CAS operations (`reserveFineGrained`).

---

## Demo C: Allocation and Garbage Collection

### Step 1: Run Allocation Workload
```bash
./scripts/run-demo.sh allocation-pressure 15 8
```

### Step 2: Ask Antigravity AI

> **Prompt:**
> "Analyze `recordings/allocation-pressure-faulty-<timestamp>.jfr`. Determine whether the recording provides evidence of excessive allocation or significant GC pauses. Distinguish observations from hypotheses."

### Expected AI Investigation Path
1. Antigravity queries allocation samples (`jdk.ObjectAllocationSample`).
2. It highlights heavy allocation of `java.lang.String`, `byte[]`, and `HashMap$Node` inside `generateInefficientSummary`.
3. It notes that while GC frequency is elevated, pause times remain low, confirming that allocation churn rather than stop-the-world pauses is the primary driver of throughput reduction.
4. It recommends using `StringBuilder` and avoiding intermediate map allocations.

---

## Demo D: Before-and-After Comparison

> **Prompt:**
> "Compare the evidence from `results/cpu-hotspot-faulty.json` and `results/cpu-hotspot-corrected.json`. Explain which measurements improved, which did not, and whether the recordings are comparable. Do not invent metrics that are not present."

### Expected AI Output
- Antigravity summarizes throughput improvement (+130,000%), P50/P95/P99 latency reduction (-99.9%), and verifies that both runs used identical concurrency (8 threads) and duration (15s) on the same JVM configuration.
