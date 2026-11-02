#!/usr/bin/env bash
# Explicit instruction export only; not bound to normal tests or CI.
set -euo pipefail
SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/copilot-common.sh"
OPERATION="${1:-sync}"
ROOT="${2:-.}"
case "$OPERATION" in
  sync|verify) ;;
  *) echo "Usage: $0 [sync|verify] [consumer-root]" >&2; exit 2 ;;
esac
fnb_copilot_java "$ROOT" com.ptaf.copilot.CopilotInstructionTool "$OPERATION" --root .
