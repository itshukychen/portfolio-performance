#!/bin/bash

# Portfolio Performance API Server Startup Script
# This script starts the standalone API server
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "🚀 Starting Portfolio Performance API Server..."

# Kill any existing portfolio API server processes
echo "🔍 Checking for existing API server processes..."
EXISTING_PIDS=$(pgrep -f "portfolio-api-server.jar" 2>/dev/null || true)

if [ ! -z "$EXISTING_PIDS" ]; then
    echo "🛑 Found existing API server processes: $EXISTING_PIDS"
    echo "💀 Killing existing processes..."
    pkill -f "portfolio-api-server.jar" 2>/dev/null || true
    sleep 2
    
    # Double-check and force kill if necessary
    REMAINING_PIDS=$(pgrep -f "portfolio-api-server.jar" 2>/dev/null || true)
    if [ ! -z "$REMAINING_PIDS" ]; then
        echo "⚠️  Force killing remaining processes: $REMAINING_PIDS"
        pkill -9 -f "portfolio-api-server.jar" 2>/dev/null || true
        sleep 1
    fi
    echo "✅ Existing processes terminated"
else
    echo "✅ No existing API server processes found"
fi

echo "📦 JAR Location: $SCRIPT_DIR/target/portfolio-api-server.jar"
echo "🌐 Server will be available at: http://localhost:8080"
echo "📡 API endpoints: http://localhost:8080/api/v1"
echo "🛑 Press Ctrl+C to stop the server"
echo ""

# Check if JAR exists
if [ ! -f "$SCRIPT_DIR/target/portfolio-api-server.jar" ]; then
    echo "❌ Error: $SCRIPT_DIR/target/portfolio-api-server.jar not found!"
    echo "Please run 'mvn3 clean package' first to build the executable JAR."
    exit 1
fi

# Check if port 8080 is available
if lsof -Pi :8080 -sTCP:LISTEN -t >/dev/null 2>&1; then
    echo "⚠️  Warning: Port 8080 is still in use. The server may not start properly."
    echo "   You may need to wait a moment for the port to be released."
    sleep 3
fi

# Start the server
PORTFOLIO_DIR=$(realpath $SCRIPT_DIR/../portfolios) java -jar $SCRIPT_DIR/target/portfolio-api-server.jar 8082 "$@"
