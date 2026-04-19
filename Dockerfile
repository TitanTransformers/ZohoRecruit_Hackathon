# ============================================================
# Stage 1: Build the React UI
# ============================================================
FROM node:20-alpine AS ui-builder

WORKDIR /ui

COPY zoho-recruit-ui/package.json zoho-recruit-ui/package-lock.json ./
RUN npm ci

COPY zoho-recruit-ui/ ./

# At build time the UI talks to the same origin via nginx proxy,
# so the base URL is simply "" (empty / relative)
ENV VITE_API_BASE_URL=""
RUN npm run build

# ============================================================
# Stage 2: Build the Java services
# ============================================================
FROM maven:3.9-eclipse-temurin-21 AS java-builder

WORKDIR /build
COPY . .

RUN cd mcp-server-demo && mvn clean package -DskipTests && \
    cd ../mcp-client-demo && mvn clean package -DskipTests

# ============================================================
# Stage 3: Runtime — nginx + JRE
# ============================================================
FROM eclipse-temurin:21-jre-alpine

# Install nginx and envsubst (from gettext)
RUN apk add --no-cache nginx gettext

WORKDIR /app

# Copy built UI into nginx's html directory
COPY --from=ui-builder /ui/dist /usr/share/nginx/html

# Copy nginx config template
COPY nginx.conf.template /etc/nginx/templates/default.conf.template

# Copy built JARs
COPY --from=java-builder /build/mcp-server-demo/target/*.jar app-server.jar
COPY --from=java-builder /build/mcp-client-demo/target/*.jar app-client.jar

# Create startup script
RUN cat > /app/start.sh << 'SCRIPT'
#!/bin/sh
set -e

# Render injects PORT; default to 10000 for Render, 3000 for local
PORT=${PORT:-3000}
export PORT

echo "==> Generating nginx config (public port: $PORT)..."
envsubst '${PORT}' < /etc/nginx/templates/default.conf.template > /etc/nginx/http.d/default.conf

echo "==> Starting MCP Server (internal port 8080)..."
java -jar /app/app-server.jar &
SERVER_PID=$!

# Wait for the server to be ready before starting the client
sleep 5

echo "==> Starting MCP Client (internal port 8081)..."
java -jar /app/app-client.jar --server.port=8081 &
CLIENT_PID=$!

echo "==> Starting nginx on port $PORT..."
nginx -g 'daemon off;' &
NGINX_PID=$!

# If any process exits, bring down the container
wait -n $SERVER_PID $CLIENT_PID $NGINX_PID
echo "A process exited — shutting down."
kill $SERVER_PID $CLIENT_PID $NGINX_PID 2>/dev/null
exit 1
SCRIPT

RUN chmod +x /app/start.sh

# Expose the single public port
EXPOSE ${PORT:-3000}

# Health check — nginx proxies /health to the MCP client
HEALTHCHECK --interval=30s --timeout=10s --start-period=90s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:${PORT:-3000}/health || exit 1

CMD ["/app/start.sh"]
