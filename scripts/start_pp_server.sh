#!/usr/bin/env bash
set -Eeuo pipefail

# Start Portfolio Performance Server (REST API)

# Unbuffer output immediately
export PYTHONUNBUFFERED=1

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

# Ensure workspace directory exists with proper permissions
mkdir -p "${WORKSPACE_DIR}"
chmod -R 755 "${WORKSPACE_DIR}" 2>/dev/null || true

# Clean up any stale workspace locks and X11 files
echo "Cleaning up stale locks and temporary files..."
rm -f "${WORKSPACE_DIR}/.metadata/.lock" 2>/dev/null || true
rm -f /tmp/.X*-lock /tmp/.X11-unix/X* 2>/dev/null || true
rm -rf /tmp/.ICE-unix/* /tmp/.X11-unix/* 2>/dev/null || true

# Wait a moment for filesystem to settle (race condition fix)
sleep 1

# Check if there's a previous workspace log that might have error info
WORKSPACE_LOG="${WORKSPACE_DIR}/.metadata/.log"
if [ -f "${WORKSPACE_LOG}" ]; then
  echo "Found existing workspace log, showing last 10 lines:"
  tail -n 10 "${WORKSPACE_LOG}" 2>/dev/null || true
  echo "-------------------------------------"
fi

# Cleanup function for Xvfb
cleanup_xvfb() {
  echo "Cleaning up Xvfb (PID: ${XVFB_PID:-none})..."
  if [ -n "${XVFB_PID:-}" ]; then
    kill ${XVFB_PID} 2>/dev/null || true
    wait ${XVFB_PID} 2>/dev/null || true
  fi
}

# Set trap to cleanup Xvfb on exit
trap cleanup_xvfb EXIT TERM INT

# Start Xvfb manually for better control and visibility
export DISPLAY=:99
echo "Starting Xvfb on display ${DISPLAY}..."
Xvfb ${DISPLAY} -screen 0 1024x768x24 -ac +extension GLX +render -noreset 2>&1 &
XVFB_PID=$!
echo "Xvfb started with PID ${XVFB_PID}"

# Wait for Xvfb to be ready
echo "Waiting for X server to be ready..."
for i in {1..10}; do
  if xdpyinfo -display ${DISPLAY} >/dev/null 2>&1; then
    echo "X server ready after ${i} seconds!"
    break
  fi
  if [ $i -eq 10 ]; then
    echo "ERROR: X server did not become ready after 10 seconds" >&2
    ps aux | grep Xvfb || true
    exit 1
  fi
  sleep 1
done

# Verify X server is still running
if ! ps -p ${XVFB_PID} > /dev/null 2>&1; then
  echo "ERROR: Xvfb process died" >&2
  exit 1
fi

echo "====================================="
echo "Starting Portfolio Performance Server..."
echo "====================================="

# Run the server with all output unbuffered and visible
# Add OSGi console for debugging
set -x  # Enable command tracing
"${SERVER_BIN}" \
  -nosplash \
  -consoleLog \
  -debug \
  -vmargs \
  -Dportfolio.server.port="${PORTFOLIO_SERVER_PORT}" \
  -Dosgi.instance.area="${WORKSPACE_DIR}" \
  -Dosgi.console.enable.builtin=true \
  < /dev/null &

SERVER_PID=$!
echo "Server started with PID ${SERVER_PID}"

# Wait for server and monitor it
wait ${SERVER_PID}
SERVER_EXIT=$?

echo "Server exited with code ${SERVER_EXIT}"
exit ${SERVER_EXIT}

