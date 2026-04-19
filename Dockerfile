# Build stage
FROM maven:3.9-eclipse-temurin-21 as builder

WORKDIR /build

# Copy all source code
COPY . .

# Build both modules
RUN cd mcp-server-demo && mvn clean package -DskipTests && \
    cd ../mcp-client-demo && mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy built JARs from builder
COPY --from=builder /build/mcp-server-demo/target/*.jar app-server.jar
COPY --from=builder /build/mcp-client-demo/target/*.jar app-client.jar

# Create startup script
RUN cat > /app/start.sh << 'EOF'
#!/bin/sh
set -e

# Default PORT for local dev; Render injects this at runtime
PORT=${PORT:-8081}

echo "Starting MCP Server on port 8080..."
java -jar app-server.jar &
SERVER_PID=$!

# Give the server a moment to start before the client connects
sleep 5

echo "Starting MCP Client on port $PORT..."
java -jar app-client.jar --server.port=$PORT &
CLIENT_PID=$!

# Wait for both processes
wait $SERVER_PID $CLIENT_PID
EOF

RUN chmod +x /app/start.sh

# Expose both ports
EXPOSE 8080 8081

# Health check — targets the Client (public-facing service)
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:${PORT:-8081}/health || exit 1

CMD ["/app/start.sh"]
