#!/usr/bin/env bash
set -Eeuo pipefail

# Start Portfolio Performance Server (REST API)

# Unbuffer output immediately
export PYTHONUNBUFFERED=1
export DISPLAY="${DISPLAY:-:0}"

SERVER_DIR="/opt/pp/server/portfolio-server"
SERVER_BIN="${SERVER_DIR}/PortfolioPerformanceServer"

echo "Checking server binary..."
if [[ ! -d "${SERVER_DIR}" ]]; then
  echo "ERROR: Server directory not found at ${SERVER_DIR}" >&2
  echo "Directory contents of /opt/pp/server:" >&2
  ls -la /opt/pp/server/ >&2 || true
  exit 1
fi

if [[ ! -f "${SERVER_BIN}" ]]; then
  echo "ERROR: Server binary not found at ${SERVER_BIN}" >&2
  echo "Directory contents of ${SERVER_DIR}:" >&2
  ls -la "${SERVER_DIR}/" >&2 || true
  exit 1
fi

if [[ ! -x "${SERVER_BIN}" ]]; then
  echo "ERROR: Server binary is not executable at ${SERVER_BIN}" >&2
  ls -la "${SERVER_BIN}" >&2
  exit 1
fi

WORKSPACE_DIR="${WORKSPACE_DIR:-/app/workspace}"
PORTFOLIO_SERVER_PORT="${PORTFOLIO_SERVER_PORT:-8080}"

echo "Starting Portfolio Performance Server..."
echo "  Port: ${PORTFOLIO_SERVER_PORT}"
echo "  Workspace: ${WORKSPACE_DIR}"
echo "  Portfolio Directory: ${PORTFOLIO_DIR:-}"
echo "  Server Binary: ${SERVER_BIN}"

# Ensure workspace directory exists
mkdir -p "${WORKSPACE_DIR}"

# Clean up any stale X11 lock files
rm -f /tmp/.X*-lock /tmp/.X11-unix/X* 2>/dev/null || true

# Run server with xvfb (virtual X server for headless SWT)
# Using -a flag to automatically choose display number
echo "Launching xvfb-run..."
echo "Command: xvfb-run -a -e /dev/stderr -s \"-screen 0 1024x768x24\" ${SERVER_BIN} -nosplash -consoleLog -vmargs -Dportfolio.server.port=${PORTFOLIO_SERVER_PORT} -Dosgi.instance.area=${WORKSPACE_DIR}"

# Test if we can get java version from the JVM in the server
echo "Testing Java runtime..."
if [ -f "${SERVER_DIR}/jre/bin/java" ]; then
  "${SERVER_DIR}/jre/bin/java" -version 2>&1 || echo "Warning: Could not get Java version"
elif compgen -G "${SERVER_DIR}/plugins/org.eclipse.justj.openjdk.hotspot.jre.full.linux.x86_64_*/jre/bin/java" > /dev/null; then
  echo "Found embedded JRE in plugins"
  ls -la "${SERVER_DIR}/plugins/" | grep -i jre || true
fi

# Check if stdbuf is available
if ! command -v stdbuf &> /dev/null; then
  echo "Warning: stdbuf not found, output may be buffered"
fi

echo "Starting server process..."
echo "====================================="

# Use stdbuf to force unbuffered output from both stdout and stderr
# This ensures we see Java output immediately
if command -v stdbuf &> /dev/null; then
  exec stdbuf -oL -eL xvfb-run -a -e /dev/stderr -s "-screen 0 1024x768x24" \
    "${SERVER_BIN}" \
    -nosplash \
    -consoleLog \
    -vmargs \
    -Dportfolio.server.port="${PORTFOLIO_SERVER_PORT}" \
    -Dosgi.instance.area="${WORKSPACE_DIR}"
else
  # Fallback without stdbuf
  exec xvfb-run -a -e /dev/stderr -s "-screen 0 1024x768x24" \
    "${SERVER_BIN}" \
    -nosplash \
    -consoleLog \
    -vmargs \
    -Dportfolio.server.port="${PORTFOLIO_SERVER_PORT}" \
    -Dosgi.instance.area="${WORKSPACE_DIR}"
fi

