# Performance Comparison Report

**Scenario:** `thread-contention`

| Metric | Before (FAULTY) | After (CORRECTED) | Delta | Change |
| :--- | :--- | :--- | :--- | :--- |
| **Throughput (req/s)** | `868.41` | `3737109.62` | `+430239.04%` | 🚀 Improvement |
| **P50 Latency (ms)** | `1.14` | `0.00` | `-99.96%` | 🚀 Improvement |
| **P95 Latency (ms)** | `69.99` | `0.00` | `-100.00%` | 🚀 Improvement |
| **P99 Latency (ms)** | `148.24` | `0.00` | `-100.00%` | 🚀 Improvement |
| **Total Requests** | `4349` | `18681811` | `+18677462` | — |
| **Failed Requests** | `0` | `0` | `+0` | — |

## Workload Parameters

- **Concurrency:** 8 threads (After: 8 threads)
- **Duration:** 5s (After: 5s)
- **Warm-up:** 1s
- **JFR Recording Before:** `thread-contention-faulty-20261010-091259.jfr`
- **JFR Recording After:** `thread-contention-corrected-20261010-091310.jfr`
