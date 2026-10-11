#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"

scenarios=(
    "cpu-hotspot"
    "thread-contention"
    "allocation-pressure"
    "gc-pressure"
    "thread-pool-starvation"
    "slow-io"
    "exception-storm"
    "serialization-overhead"
    "lock-and-io"
    "jvm-configuration"
)

echo "Generating before and after recordings for all 10 scenarios..."

for s in "${scenarios[@]}"; do
    echo "=================================================="
    echo " Running Scenario: $s"
    echo "=================================================="
    "${SCRIPT_DIR}/run-demo.sh" "$s" 5 4
done

echo ""
echo "All 10 scenarios recorded and compared successfully!"
