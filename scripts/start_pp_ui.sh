#!/usr/bin/env bash
set -Eeuo pipefail

# Start Portfolio Performance UI (Desktop GUI)

export DISPLAY="${DISPLAY:-:0}"

UI_BIN="/opt/pp/ui/portfolio/PortfolioPerformance"
if [[ ! -x "${UI_BIN}" ]]; then
  echo "ERROR: Portfolio Performance UI not found at ${UI_BIN}" >&2
  exit 1
fi

WORKSPACE_DIR="${WORKSPACE_DIR:-/app/workspace}"

echo "Starting Portfolio Performance UI..."
echo "  Display: ${DISPLAY}"
echo "  Workspace: ${WORKSPACE_DIR}"
echo "  Portfolio Directory: ${PORTFOLIO_DIR:-}"

# Ensure workspace exists
mkdir -p "${WORKSPACE_DIR}"

# Run UI (X server already started by entrypoint)
exec "${UI_BIN}" \
  -data "${WORKSPACE_DIR}" \
  -nosplash \
  -consoleLog

