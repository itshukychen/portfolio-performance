#!/usr/bin/env bash
set -Eeuo pipefail

# Unified entrypoint for Portfolio Performance
# Supports two modes: server (API) or ui (Desktop GUI with VNC)

RUN_MODE="${RUN_MODE:-server}"

echo "=================================================="
echo "Portfolio Performance Docker Container"
echo "Mode: ${RUN_MODE}"
echo "=================================================="

case "${RUN_MODE}" in
  server)
    echo "Starting in SERVER mode (REST API)"
    exec gosu ppuser:ppuser /opt/bin/start_pp_server.sh
    ;;
  
  ui)
    echo "Starting in UI mode (Desktop GUI with VNC)"
    
    # Start Xvfb + Fluxbox + VNC in background
    /opt/bin/start_x_vnc.sh &
    XVNC_PID=$!
    
    # Give the X server a moment to come up
    sleep 3
    
    echo "X server ready, starting Portfolio Performance UI..."
    
    # Run UI as ppuser
    if command -v gosu >/dev/null 2>&1; then
      exec gosu ppuser:ppuser /opt/bin/start_pp_ui.sh
    else
      exec su -s /bin/bash -c "/opt/bin/start_pp_ui.sh" ppuser
    fi
    
    # If UI exits, clean up VNC
    wait "${XVNC_PID}"
    ;;
  
  *)
    echo "ERROR: Invalid RUN_MODE '${RUN_MODE}'"
    echo "Valid modes: server, ui"
    exit 1
    ;;
esac

