#!/usr/bin/env bash
# Explicit local discovery only: no MCP/browser/test launch.
# Usage: ./scripts/copilot-discovery.sh mcp-template|mcp-setup [consumer-root]
#        ./scripts/copilot-discovery.sh validate <candidate-yaml> [consumer-root]
#        ./scripts/copilot-discovery.sh draft <candidate-yaml> [consumer-root]
set -euo pipefail
SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/copilot-common.sh"
OPERATION="${1:-}"
case "$OPERATION" in
  mcp-template|mcp-setup)
    ROOT="${2:-.}"
    fnb_copilot_java "$ROOT" com.ptaf.copilot.CopilotDiscoveryTool "$OPERATION" --root .
    ;;
  validate)
    INPUT="${2:?Provide candidate YAML relative to consumer root or an absolute path.}"
    ROOT="${3:-.}"
    fnb_copilot_java "$ROOT" com.ptaf.copilot.CopilotDiscoveryTool validate --input "$INPUT"
    ;;
  draft)
    INPUT="${2:?Provide candidate YAML relative to consumer root or an absolute path.}"
    ROOT="${3:-.}"
    fnb_copilot_java "$ROOT" com.ptaf.copilot.CopilotDiscoveryTool draft --input "$INPUT" --root .
    ;;
  *) echo "Usage: $0 mcp-template|mcp-setup [root] | validate|draft <candidate-yaml> [root]" >&2; exit 2 ;;
esac
