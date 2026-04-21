package com.mcp.mcp_server.config;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

/**
 * Simple health-check endpoint to verify the MCP server is running.
 *
 * Usage:
 *   curl http://localhost:8080/health
 */
@RestController
public class HealthController {

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
                "status", "UP",
                "service", "recruitment-mcp-server",
                "timestamp", Instant.now().toString(),
                "endpoints", Map.of(
                        "mcp", "/mcp",
                        "health", "/health"
                )
        );
    }
}

