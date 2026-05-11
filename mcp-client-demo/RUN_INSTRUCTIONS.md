# MCP Client - Run Instructions

## Important: OkHttp Timeout Configuration

The MCP client makes requests to Claude Sonnet 4, which can take 2-5 minutes to process large candidate datasets from the MCP server. By default, OkHttp has a 60-second timeout, which causes the request to fail.

### Option 1: Run with JVM Properties (Recommended)

```bash
mvn spring-boot:run \
  -Dokhttp.connectTimeout=300000 \
  -Dokhttp.readTimeout=300000 \
  -Dokhttp.writeTimeout=300000
```

Or for JAR execution:

```bash
java -Dokhttp.connectTimeout=300000 \
     -Dokhttp.readTimeout=300000 \
     -Dokhttp.writeTimeout=300000 \
     -jar target/mcp-client-0.0.1-SNAPSHOT.jar
```

### Option 2: Set Environment Variables

```bash
export JAVA_OPTS="-Dokhttp.connectTimeout=300000 -Dokhttp.readTimeout=300000 -Dokhttp.writeTimeout=300000"
mvn spring-boot:run
```

### Timeout Values Explanation

- `connectTimeout`: 5 minutes (300,000 ms) - time to establish connection
- `readTimeout`: 5 minutes (300,000 ms) - time to wait for response from server
- `writeTimeout`: 5 minutes (300,000 ms) - time to send request to server

These values allow Claude to spend up to 5 minutes processing the MCP tool results before timing out.

## Performance Notes

1. **First request**: ~30-50 seconds (includes MCP server call + Claude processing)
2. **Subsequent identical requests**: ~1 ms (cached response)
3. **Cache TTL**: 5 minutes

## Example Request

```bash
curl -X POST http://localhost:8081/api/chat/send \
  -H "Content-Type: application/json" \
  -d '{
    "message": "Find top 10 candidates",
    "pageSize": 10
  }'
```

## Troubleshooting

If you see `io.micrometer.http.exception.AnthropicIoException: Request failed` with `okhttp3.internal.http2.StreamResetException: stream was reset: CANCEL`, it means the request timed out. Increase the timeout values further or check if the MCP server is responding slowly.

