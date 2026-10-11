# Performance Comparison Report

**Scenario:** `allocation-pressure`

| Metric | Before (FAULTY) | After (CORRECTED) | Delta | Change |
| :--- | :--- | :--- | :--- | :--- |
| **Throughput (req/s)** | `185220.24` | `521931.79` | `+181.79%` | 🚀 Improvement |
| **P50 Latency (ms)** | `0.02` | `0.01` | `-65.79%` | 🚀 Improvement |
| **P95 Latency (ms)** | `0.03` | `0.01` | `-65.08%` | 🚀 Improvement |
| **P99 Latency (ms)** | `0.04` | `0.02` | `-61.45%` | 🚀 Improvement |
| **Total Requests** | `925916` | `2609137` | `+1683221` | — |
| **Failed Requests** | `0` | `0` | `+0` | — |

## Workload Parameters

- **Concurrency:** 4 threads (After: 4 threads)
- **Duration:** 5s (After: 5s)
- **Warm-up:** 1s
- **JFR Recording Before:** `allocation-pressure-faulty-20261010-091325.jfr`
- **JFR Recording After:** `allocation-pressure-corrected-20261010-091336.jfr`
