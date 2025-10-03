#!/bin/bash

# Test script for Portfolio Performance API Server Graceful Shutdown
# This script demonstrates that the server now responds properly to Ctrl+C

echo "Portfolio Performance API Server - Graceful Shutdown Test"
echo "========================================================"
echo ""

echo "Starting the API server..."
echo "You can now test Ctrl+C to see if it shuts down gracefully"
echo ""

# Start the server
cd /Users/shuky/dev/portfolio/name.abuchen.portfolio.api
mvn3 spring-boot:run
