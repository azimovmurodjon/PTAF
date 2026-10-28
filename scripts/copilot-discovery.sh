#!/usr/bin/env bash
# Local FNB-ETAF Playwright MCP discovery helper.
# Usage:
#   ./scripts/copilot-discovery.sh mcp-template [consumer-root]
#   ./scripts/copilot-discovery.sh validate <candidate-yaml>
#   ./scripts/copilot-discovery.sh draft <candidate-yaml> [consumer-root]
# This command never starts Playwright MCP or normal FNB-ETAF tests.
set -euo pipefail

OPERATION="${1:-}"
if [[ -z "$OPERATION" ]]; then
  echo "Usage: $0 [mcp-template|validate|draft] <input-or-root> [consumer-root]" >&2
  exit 2
fi

mvn -q -DskipTests test-compile dependency:build-classpath \
  -Dmdep.outputFile=target/copilot-runtime-classpath.txt
CLASSPATH="target/classes:$(cat target/copilot-runtime-classpath.txt)"

case "$OPERATION" in
  mcp-template)
    ROOT="${2:-.}"
    java -cp "$CLASSPATH" com.ptaf.copilot.CopilotDiscoveryTool mcp-template --root "$ROOT"
    ;;
  validate)
    INPUT="${2:?Provide a candidate YAML file.}"
    java -cp "$CLASSPATH" com.ptaf.copilot.CopilotDiscoveryTool validate --input "$INPUT"
    ;;
  draft)
    INPUT="${2:?Provide a candidate YAML file.}"
    ROOT="${3:-.}"
    java -cp "$CLASSPATH" com.ptaf.copilot.CopilotDiscoveryTool draft --input "$INPUT" --root "$ROOT"
    ;;
  *)
    echo "Usage: $0 [mcp-template|validate|draft] <input-or-root> [consumer-root]" >&2
    exit 2
    ;;
esac
