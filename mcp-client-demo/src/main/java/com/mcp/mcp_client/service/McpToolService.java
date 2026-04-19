package com.mcp.mcp_client.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class McpToolService {

    private static final Logger logger = LoggerFactory.getLogger(McpToolService.class);

    private final ToolCallbackProvider toolCallbackProvider;

    public McpToolService(ToolCallbackProvider toolCallbackProvider) {
        this.toolCallbackProvider = toolCallbackProvider;
        logger.info("McpToolService initialized with ToolCallbackProvider");
    }

    /**
     * Get all available tools from all connected MCP servers.
     */
    public List<Map<String, String>> getAvailableTools() {
        List<Map<String, String>> tools = new ArrayList<>();
        try {
            for (ToolCallback callback : toolCallbackProvider.getToolCallbacks()) {
                Map<String, String> toolInfo = new LinkedHashMap<>();
                toolInfo.put("name", callback.getToolDefinition().name());
                toolInfo.put("description", callback.getToolDefinition().description());
                tools.add(toolInfo);
                logger.debug("Discovered tool: {}", callback.getToolDefinition().name());
            }
            logger.info("Discovered {} tools from MCP servers", tools.size());
        } catch (Exception e) {
            logger.error("Error fetching tools from MCP servers", e);
        }
        return tools;
    }
}
