# Performance Comparison Report

**Scenario:** `cpu-hotspot`

| Metric | Before (FAULTY) | After (CORRECTED) | Delta | Change |
| :--- | :--- | :--- | :--- | :--- |
| **Throughput (req/s)** | `2327.13` | `3067009.60` | `+131693.39%` | 🚀 Improvement |
| **P50 Latency (ms)** | `1.69` | `0.00` | `-99.94%` | 🚀 Improvement |
| **P95 Latency (ms)** | `1.80` | `0.00` | `-99.89%` | 🚀 Improvement |
| **P99 Latency (ms)** | `3.20` | `0.00` | `-99.90%` | 🚀 Improvement |
| **Total Requests** | `11638` | `15331981` | `+15320343` | — |
| **Failed Requests** | `0` | `0` | `+0` | — |

## Workload Parameters

- **Concurrency:** 4 threads (After: 4 threads)
- **Duration:** 5s (After: 5s)
- **Warm-up:** 1s
- **JFR Recording Before:** `cpu-hotspot-faulty-20261010-091234.jfr`
- **JFR Recording After:** `cpu-hotspot-corrected-20261010-091245.jfr`
