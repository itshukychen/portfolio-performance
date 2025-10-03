#!/bin/bash

# Test script for Portfolio Performance API Server File Operations
# This script demonstrates the file opening functionality

echo "Portfolio Performance API Server - File Operations Test"
echo "======================================================"
echo ""

# Test basic endpoints
echo "1. Testing basic API endpoints..."
echo "---------------------------------"

echo "Hello endpoint:"
curl -s http://localhost:8080/api/v1/hello | jq .
echo ""

echo "File service health:"
curl -s http://localhost:8080/api/v1/files/health | jq .
echo ""

# Test file info endpoint (without actually opening a file)
echo "2. Testing file info endpoint..."
echo "--------------------------------"

echo "Testing file info for a non-existent file:"
curl -s -X GET "http://localhost:8080/api/v1/files/info?filePath=/nonexistent/file.portfolio" | jq .
echo ""

# Test cache endpoints
echo "3. Testing cache management..."
echo "-------------------------------"

echo "Cache statistics:"
curl -s http://localhost:8080/api/v1/files/cache/stats | jq .
echo ""

echo "Clearing cache:"
curl -s -X POST http://localhost:8080/api/v1/files/cache/clear | jq .
echo ""

echo "4. Testing file opening with sample data..."
echo "--------------------------------------------"

# Create a sample request for opening a file
echo "Sample file opening request:"
cat << 'EOF' | curl -s -X POST http://localhost:8080/api/v1/files/open \
  -H "Content-Type: application/json" \
  -d @- | jq .
{
  "filePath": "/path/to/sample/portfolio.portfolio",
  "password": null
}
EOF

echo ""
echo "======================================================"
echo "File operations test completed!"
echo ""
echo "Available endpoints:"
echo "- GET  /api/v1/hello                    - Hello world"
echo "- GET  /api/v1/health                   - Health check"
echo "- GET  /api/v1/info                     - API information"
echo "- GET  /api/v1/portfolio/info           - Portfolio info"
echo "- POST /api/v1/files/open               - Open portfolio file"
echo "- GET  /api/v1/files/info?filePath=...  - Get file info"
echo "- GET  /api/v1/files/health             - File service health"
echo "- GET  /api/v1/files/cache/stats          - Cache statistics"
echo "- POST /api/v1/files/cache/clear         - Clear cache"
echo ""
echo "Example usage:"
echo "curl -X POST http://localhost:8080/api/v1/files/open \\"
echo "  -H 'Content-Type: application/json' \\"
echo "  -d '{\"filePath\": \"/path/to/your/portfolio.portfolio\"}'"
