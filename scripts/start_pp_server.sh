#!/usr/bin/env bash
set -Eeuo pipefail

# Start Portfolio Performance Server (REST API)

export DISPLAY="${DISPLAY:-:0}"

SERVER_BIN="/opt/pp/server/portfolio-server/PortfolioPerformanceServer"
if [[ ! -x "${SERVER_BIN}" ]]; then
  echo "ERROR: Portfolio Performance Server not found at ${SERVER_BIN}" >&2
  exit 1
fi

WORKSPACE_DIR="${WORKSPACE_DIR:-/home/ppuser/workspace}"
PORTFOLIO_SERVER_PORT="${PORTFOLIO_SERVER_PORT:-8080}"

echo "Starting Portfolio Performance Server..."
echo "  Port: ${PORTFOLIO_SERVER_PORT}"
echo "  Workspace: ${WORKSPACE_DIR}"
echo "  Portfolio Directory: ${PORTFOLIO_DIR}"

# Run server with xvfb (virtual X server for headless SWT)
exec xvfb-run -a "${SERVER_BIN}" \
  -nosplash \
  -consoleLog \
  -vmargs \
  -Dportfolio.server.port="${PORTFOLIO_SERVER_PORT}" \
  -Dosgi.instance.area="${WORKSPACE_DIR}"

