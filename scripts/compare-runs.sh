#!/usr/bin/env bash
set -euo pipefail

BEFORE="${1:-results/cpu-faulty.json}"
AFTER="${2:-results/cpu-corrected.json}"
OUTPUT="${3:-results/comparison-report.md}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
JAR_FILE="${ROOT_DIR}/target/jfr-performance-demo-app-1.0.0-SNAPSHOT.jar"

if [[ ! -f "${JAR_FILE}" ]]; then
    echo "JAR not found. Building project..."
    (cd "${ROOT_DIR}" && mvn clean package -DskipTests)
fi

java -jar "${JAR_FILE}" \
    --compare \
    --before="${BEFORE}" \
    --after="${AFTER}" \
    --output="${OUTPUT}"
