# Troubleshooting Guide: AnthropicIoException: Request Failed

## Overview
The error `com.anthropic.errors.AnthropicIoException: Request failed` indicates a communication issue between your application and the Anthropic API. This guide helps you diagnose and resolve the issue.

## Common Causes & Solutions

### 1. **Missing or Invalid API Key**
**Symptoms:** 
- "Request failed" with 401 Unauthorized
- "Invalid API key" error

**Solutions:**
```bash
# Check if ANTHROPIC_API_KEY is set
echo $ANTHROPIC_API_KEY

# Get your API key from https://console.anthropic.com/
# Then set it:
export ANTHROPIC_API_KEY=sk-ant-xxxxxxxxxxxxx

# Verify it's correctly set in your environment
java -jar mcp-client-demo-0.0.1-SNAPSHOT.jar
```

### 2. **Network Connectivity Issues**
**Symptoms:**
- "Request failed" with connection timeout
- Cannot reach API.anthropic.com

**Solutions:**
```bash
# Test connectivity to Anthropic API
curl -I https://api.anthropic.com/
ping api.anthropic.com

# Check for firewall/proxy issues
curl -v https://api.anthropic.com/ 2>&1 | grep -i "connect\|timeout"

# If behind a proxy, configure it
export HTTP_PROXY=http://proxy.example.com:8080
export HTTPS_PROXY=http://proxy.example.com:8080
```

### 3. **API Rate Limiting**
**Symptoms:**
- "Request failed" with 429 Too Many Requests
- Requests intermittently fail

**Solutions:**
- The application now includes automatic retry logic with exponential backoff
- Default: 3 retries with 100ms, 200ms, 400ms delays
- Check Anthropic dashboard for rate limits: https://console.anthropic.com/

### 4. **API Endpoint Timeout**
**Symptoms:**
- "Request timeout" error
- Operation takes too long

**Solutions:**
Configuration in `application.yaml`:
```yaml
spring:
  ai:
    anthropic:
      timeout: 60s              # Total request timeout
      connect-timeout: 30s      # Initial connection timeout
```

Increase timeouts if needed:
```yaml
spring:
  ai:
    anthropic:
      timeout: 120s             # Increase to 2 minutes
      connect-timeout: 45s      # Increase to 45 seconds
```

### 5. **MCP Server Issues**
**Symptoms:**
- Chat works but MCP tool calls fail
- "Direct MCP" endpoint returns errors

**Solutions:**
```bash
# Verify MCP server is running
curl http://localhost:8080/mcp

# Check MCP server configuration
curl http://localhost:8081/api/chat/tools

# If MCP server URL is wrong, update:
export MCP_SERVER_URL=http://localhost:8080/mcp
```

## Diagnostic Steps

### Step 1: Verify Environment Variables
```bash
# Check all required variables
echo "ANTHROPIC_API_KEY: ${ANTHROPIC_API_KEY:?(Missing ANTHROPIC_API_KEY)}"
echo "MCP_SERVER_URL: ${MCP_SERVER_URL:-(default: http://localhost:8080/mcp)}"
echo "PORT: ${PORT:-(default: 8081)}"
```

### Step 2: Check Application Logs
```bash
# Run application with debug logging
export LOGGING_LEVEL_ORG_SPRINGFRAMEWORK_AI=DEBUG
java -jar mcp-client-demo-0.0.1-SNAPSHOT.jar

# Look for:
# - "Retrying in X ms"
# - "Request validation"
# - "API Connection error"
```

### Step 3: Test API Connectivity
```bash
# Test Anthropic API directly
curl -X POST https://api.anthropic.com/v1/messages \
  -H "x-api-key: $ANTHROPIC_API_KEY" \
  -H "anthropic-version: 2023-06-01" \
  -H "content-type: application/json" \
  -d '{
    "model": "claude-3-5-haiku-20241022",
    "max_tokens": 100,
    "messages": [{"role": "user", "content": "Hello"}]
  }' | jq .
```

### Step 4: Test Local MCP Server
```bash
# If using direct MCP endpoint
curl -X POST http://localhost:8081/api/chat/direct \
  -H "Content-Type: application/json" \
  -d '{"message": "Get top 10 candidates", "pageSize": 10}' | jq .

# List available tools
curl http://localhost:8081/api/chat/tools | jq .
```

## Retry Logic Details

The application now includes automatic retry logic:
- **Maximum Retries:** 3 attempts
- **Backoff Strategy:** Exponential (100ms → 200ms → 400ms)
- **Transient Errors:** Network timeouts, connection errors, rate limits
- **Non-transient Errors:** Authentication failures (no retry)

Example flow:
```
Request → Fails (Attempt 1) → Wait 100ms → Retry
         → Fails (Attempt 2) → Wait 200ms → Retry
         → Fails (Attempt 3) → Return error
```

## HTTP Status Codes

| Status | Meaning | Solution |
|--------|---------|----------|
| 400 | Bad Request | Check message format |
| 401 | Unauthorized | Verify API key |
| 429 | Rate Limited | Wait or upgrade plan |
| 500 | Server Error | Check logs, retry |
| 503 | Service Unavailable | API down, try later |

## Performance Optimization

If you're experiencing slow requests:

1. **Use Direct MCP Endpoint** (faster, no Claude processing):
```bash
curl -X POST http://localhost:8081/api/chat/direct \
  -H "Content-Type: application/json" \
  -d '{"message": "Show candidates"}'
```

2. **Reduce Response Size:**
```bash
curl -X POST http://localhost:8081/api/chat/send \
  -H "Content-Type: application/json" \
  -d '{"message": "Top 5 candidates", "pageSize": 5}'
```

3. **Optimize Timeouts:**
```yaml
spring:
  ai:
    anthropic:
      chat:
        options:
          max-tokens: 4096  # Reduce from 8192 if not needed
```

## Getting Help

1. **Check logs for error details:**
   ```bash
   grep -i "error\|exception" application.log
   ```

2. **Enable verbose logging:**
   ```yaml
   logging:
     level:
       org.springframework.ai: TRACE
       com.mcp.mcp_client: TRACE
   ```

3. **Contact Anthropic Support:**
   - Visit: https://support.anthropic.com/
   - Check status: https://status.anthropic.com/

4. **Verify configuration:**
   - API key: https://console.anthropic.com/
   - Usage: https://console.anthropic.com/usage
   - Rate limits: https://console.anthropic.com/account/rate-limits

## Monitoring

Add health checks to your setup:
```bash
# Application health
curl http://localhost:8081/actuator/health | jq .

# Chat endpoint health
curl http://localhost:8081/api/chat/health
```

## Quick Fix Checklist

- [ ] ANTHROPIC_API_KEY is set and valid
- [ ] Network connectivity to api.anthropic.com is working
- [ ] API rate limits not exceeded
- [ ] MCP server is running and accessible
- [ ] Application logs show retry attempts
- [ ] Timeout values are reasonable for your use case

