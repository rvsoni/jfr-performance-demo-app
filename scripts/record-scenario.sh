#!/usr/bin/env bash
set -euo pipefail

SCENARIO="${1:-cpu-hotspot}"
MODE="${2:-FAULTY}"
DURATION="${3:-15}"
CONCURRENCY="${4:-8}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
JAR_FILE="${ROOT_DIR}/target/jfr-performance-demo-app-1.0.0-SNAPSHOT.jar"

if [[ ! -f "${JAR_FILE}" ]]; then
    echo "JAR not found. Building project..."
    (cd "${ROOT_DIR}" && mvn clean package -DskipTests)
fi

mkdir -p "${ROOT_DIR}/results" "${ROOT_DIR}/recordings"

echo "Capturing JFR for Scenario='${SCENARIO}' Mode='${MODE}' (Duration=${DURATION}s, Concurrency=${CONCURRENCY})..."
java -jar "${JAR_FILE}" \
    --scenario="${SCENARIO}" \
    --mode="${MODE}" \
    --duration="${DURATION}" \
    --warmup=2 \
    --concurrency="${CONCURRENCY}" \
    --record-jfr=true \
    --output="${ROOT_DIR}/results/${SCENARIO}-${MODE,,}.json"
