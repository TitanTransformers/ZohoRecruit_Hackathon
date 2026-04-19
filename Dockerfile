# Build stage
FROM maven:3.9-eclipse-temurin-21 as builder

WORKDIR /build

# Copy all source code
COPY . .

# Build both modules
RUN mvn clean package -DskipTests \
    -pl mcp-server-demo,mcp-client-demo

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

echo "Starting MCP Server..."
java -jar app-server.jar &
SERVER_PID=$!

echo "Starting MCP Client..."
java -jar app-client.jar &
CLIENT_PID=$!

# Wait for both processes
wait $SERVER_PID $CLIENT_PID
EOF

RUN chmod +x /app/start.sh

# Expose both ports
EXPOSE 8080 8081

# Health check (optional, but recommended for Render)
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/health || exit 1

CMD ["/app/start.sh"]
