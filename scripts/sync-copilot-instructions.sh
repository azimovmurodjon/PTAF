#!/usr/bin/env bash
# Generates or verifies IntelliJ/GitHub Copilot repository instructions from the FNB-ETAF embedded profile.
# Usage: ./scripts/sync-copilot-instructions.sh [sync|verify] [consumer-repository-root]
# This script is explicit by design and is never called by Maven test or normal framework execution.

set -euo pipefail

OPERATION="${1:-sync}"
CONSUMER_ROOT="${2:-.}"

case "$OPERATION" in
  sync|verify) ;;
  *)
    echo "Usage: $0 [sync|verify] [consumer-repository-root]" >&2
    exit 2
    ;;
esac

mvn -q -DskipTests test-compile dependency:build-classpath \
  -Dmdep.outputFile=target/copilot-runtime-classpath.txt

CLASSPATH="target/classes:$(cat target/copilot-runtime-classpath.txt)"
java -cp "$CLASSPATH" com.ptaf.copilot.CopilotInstructionTool "$OPERATION" --root "$CONSUMER_ROOT"
