#!/bin/bash

# Smoke test for Portfolio Performance Server
# This script starts the server, tests the health endpoint, and stops the server

set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PORT=${1:-8080}
TIMEOUT=30

echo "🧪 Portfolio Performance Server Smoke Test"
echo "==========================================="
echo "Port: $PORT"
echo ""

# Detect OS and set appropriate paths
if [[ "$OSTYPE" == "darwin"* ]]; then
    # macOS
    PRODUCT_DIR="$SCRIPT_DIR/../portfolio-product/target/products/name.abuchen.portfolio.server.product/macosx/cocoa/$(uname -m)"
    LAUNCHER="./PortfolioPerformanceServer"
elif [[ "$OSTYPE" == "linux-gnu"* ]]; then
    # Linux
    PRODUCT_DIR="$SCRIPT_DIR/../portfolio-product/target/products/name.abuchen.portfolio.server.product/linux/gtk/x86_64"
    LAUNCHER="xvfb-run -a ./PortfolioPerformanceServer"
else
    echo "❌ Unsupported OS: $OSTYPE"
    exit 1
fi

# Check if product exists
if [ ! -d "$PRODUCT_DIR" ]; then
    echo "❌ Product not found at: $PRODUCT_DIR"
    echo "   Please run 'mvn clean verify' first"
    exit 1
fi

cd "$PRODUCT_DIR"

# Start the server in background
echo "🚀 Starting server..."
$LAUNCHER -nosplash -consoleLog -vmargs -Dportfolio.server.port=$PORT > server.log 2>&1 &
SERVER_PID=$!

# Function to cleanup on exit
cleanup() {
    if [ ! -z "$SERVER_PID" ]; then
        echo ""
        echo "🛑 Stopping server (PID: $SERVER_PID)..."
        kill $SERVER_PID 2>/dev/null || true
        wait $SERVER_PID 2>/dev/null || true
    fi
}
trap cleanup EXIT

# Wait for server to start
echo "⏳ Waiting for server to start (timeout: ${TIMEOUT}s)..."
ELAPSED=0
while [ $ELAPSED -lt $TIMEOUT ]; do
    if curl -s http://localhost:$PORT/api/v1/portfolios/health > /dev/null 2>&1; then
        echo "✅ Server started successfully!"
        break
    fi
    sleep 1
    ELAPSED=$((ELAPSED + 1))
    
    # Check if server process is still running
    if ! kill -0 $SERVER_PID 2>/dev/null; then
        echo "❌ Server process died during startup"
        echo "Last 20 lines of server.log:"
        tail -n 20 server.log
        exit 1
    fi
done

if [ $ELAPSED -eq $TIMEOUT ]; then
    echo "❌ Server failed to start within ${TIMEOUT}s"
    echo "Last 20 lines of server.log:"
    tail -n 20 server.log
    exit 1
fi

# Test health endpoint
echo ""
echo "🔍 Testing health endpoint..."
HEALTH_RESPONSE=$(curl -s -w "\n%{http_code}" http://localhost:$PORT/api/v1/portfolios/health)
HTTP_CODE=$(echo "$HEALTH_RESPONSE" | tail -n1)
RESPONSE_BODY=$(echo "$HEALTH_RESPONSE" | head -n-1)

if [ "$HTTP_CODE" = "200" ]; then
    echo "✅ Health check passed (HTTP $HTTP_CODE)"
    echo "   Response: $RESPONSE_BODY"
else
    echo "❌ Health check failed (HTTP $HTTP_CODE)"
    echo "   Response: $RESPONSE_BODY"
    exit 1
fi

# Test hello endpoint
echo ""
echo "🔍 Testing hello endpoint..."
HELLO_RESPONSE=$(curl -s -w "\n%{http_code}" http://localhost:$PORT/api/hello)
HTTP_CODE=$(echo "$HELLO_RESPONSE" | tail -n1)
RESPONSE_BODY=$(echo "$HELLO_RESPONSE" | head -n-1)

if [ "$HTTP_CODE" = "200" ]; then
    echo "✅ Hello endpoint passed (HTTP $HTTP_CODE)"
    echo "   Response: $RESPONSE_BODY"
else
    echo "❌ Hello endpoint failed (HTTP $HTTP_CODE)"
    echo "   Response: $RESPONSE_BODY"
    exit 1
fi

# Success
echo ""
echo "🎉 All smoke tests passed!"
echo ""
echo "Server log excerpt:"
echo "-------------------"
tail -n 10 server.log

exit 0

