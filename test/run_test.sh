#!/usr/bin/env bash
# ==============================================================================
# Helper Script: Run Maestro E2E Test & Save Output Directly into test/results/
# ==============================================================================

set -e

# Default to master suite if no file specified
TEST_FILE="${1:-test/master_e2e_suite.yaml}"

# Output directories
OUTPUT_DIR="test/results"
mkdir -p "$OUTPUT_DIR"

TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
REPORT_HTML="$OUTPUT_DIR/report_${TIMESTAMP}.html"
RUN_DIR="$OUTPUT_DIR/run_${TIMESTAMP}"

MAESTRO_BIN="$HOME/.maestro/bin/maestro"
if ! command -v "$MAESTRO_BIN" &> /dev/null; then
    MAESTRO_BIN="maestro"
fi

echo "============================================================"
echo "🚀 Running Maestro E2E Test: $TEST_FILE"
echo "📂 Artifacts output: $RUN_DIR"
echo "📄 HTML Report:      $REPORT_HTML"
echo "============================================================"

"$MAESTRO_BIN" test "$TEST_FILE" \
    --format HTML \
    --output "$REPORT_HTML" \
    --test-output-dir "$RUN_DIR"

echo ""
echo "============================================================"
echo "✅ Test run finished!"
echo "📄 View HTML Report: file://$(pwd)/$REPORT_HTML"
echo "📂 Artifacts saved:  file://$(pwd)/$RUN_DIR"
echo "============================================================"
