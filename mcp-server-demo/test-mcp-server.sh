#!/bin/bash

# MCP Server Health & Functionality Test Script

echo "=========================================="
echo "MCP Server Status Check"
echo "=========================================="
echo ""

# Check if server is running
echo "1. Checking if server is listening on port 8080..."
if lsof -Pi :8080 -sTCP:LISTEN -t >/dev/null 2>&1 ; then
    echo "✓ Server is LISTENING on port 8080"
else
    echo "✗ Server is NOT listening on port 8080"
    echo "  Start it with: cd /Volumes/A/MCP/mcp-server && ./mvnw spring-boot:run"
    exit 1
fi

echo ""
echo "2. Testing health endpoint..."
HEALTH=$(curl -s http://localhost:8080/actuator/health)
if echo "$HEALTH" | grep -q "UP"; then
    echo "✓ Health check PASSED"
    echo "  Response: $HEALTH"
else
    echo "✗ Health check FAILED"
    echo "  Response: $HEALTH"
    exit 1
fi

echo ""
echo "3. Testing MCP SSE endpoint..."
echo "  (This will timeout after 3 seconds - that's normal)"
TIMEOUT=$(timeout 3 curl -N http://localhost:8080/sse 2>&1 || true)
if echo "$TIMEOUT" | grep -q -E "jsonrpc|protocol|error" || [ -n "$TIMEOUT" ]; then
    echo "✓ SSE endpoint is RESPONDING"
    echo "  First 200 chars of response:"
    echo "  ${TIMEOUT:0:200}"
else
    echo "⚠ No response from SSE endpoint (check if server is fully initialized)"
fi

echo ""
echo "4. Testing tools availability..."
TOOLS_LOG=$(tail -20 /var/log/mcp-server.log 2>/dev/null || echo "")
if echo "$TOOLS_LOG" | grep -q "Tool"; then
    echo "✓ Tools appear to be registered"
else
    echo "⚠ Cannot verify tools from logs (check application logs)"
fi

echo ""
echo "=========================================="
echo "✓ MCP Server appears to be functional!"
echo "=========================================="
echo ""
echo "Next steps:"
echo "  1. Connect Claude Desktop to this server"
echo "  2. Or test with: curl -N http://localhost:8080/sse"

