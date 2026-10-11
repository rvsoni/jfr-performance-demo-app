#!/usr/bin/env bash
set -euo pipefail

SCENARIO="${1:-cpu-hotspot}"
DURATION="${2:-10}"
CONCURRENCY="${3:-8}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"

JAR_FILE="${ROOT_DIR}/target/jfr-performance-demo-app-1.0.0-SNAPSHOT.jar"

if [[ ! -f "${JAR_FILE}" ]]; then
    echo "JAR not found. Building project..."
    (cd "${ROOT_DIR}" && mvn clean package -DskipTests)
fi

mkdir -p "${ROOT_DIR}/results" "${ROOT_DIR}/recordings"

echo "========================================================================"
echo " Running Performance Demonstration: ${SCENARIO}"
echo " Duration: ${DURATION}s | Concurrency: ${CONCURRENCY} workers"
echo "========================================================================"

echo ""
echo ">>> STEP 1: Running FAULTY implementation & Capturing JFR..."
java -jar "${JAR_FILE}" \
    --scenario="${SCENARIO}" \
    --mode=FAULTY \
    --duration="${DURATION}" \
    --warmup=2 \
    --concurrency="${CONCURRENCY}" \
    --record-jfr=true \
    --output="${ROOT_DIR}/results/${SCENARIO}-faulty.json"

echo ""
echo ">>> STEP 2: Running CORRECTED implementation & Capturing JFR..."
java -jar "${JAR_FILE}" \
    --scenario="${SCENARIO}" \
    --mode=CORRECTED \
    --duration="${DURATION}" \
    --warmup=2 \
    --concurrency="${CONCURRENCY}" \
    --record-jfr=true \
    --output="${ROOT_DIR}/results/${SCENARIO}-corrected.json"

echo ""
echo ">>> STEP 3: Generating Before-and-After Comparison Report..."
java -jar "${JAR_FILE}" \
    --compare \
    --before="${ROOT_DIR}/results/${SCENARIO}-faulty.json" \
    --after="${ROOT_DIR}/results/${SCENARIO}-corrected.json" \
    --output="${ROOT_DIR}/results/${SCENARIO}-comparison.md"

echo ""
echo "========================================================================"
echo " Demo complete for scenario: ${SCENARIO}"
echo " Comparison report: results/${SCENARIO}-comparison.md"
echo " JFR recordings saved in: recordings/"
echo "========================================================================"
