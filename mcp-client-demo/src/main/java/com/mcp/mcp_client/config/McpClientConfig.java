package com.mcp.mcp_client.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class McpClientConfig {

    private static final Logger logger = LoggerFactory.getLogger(McpClientConfig.class);

    /**
     * Configure ChatClient with MCP tool callbacks.
     * <p>
     * - ChatClient.Builder is auto-configured by Spring AI (uses the Anthropic ChatModel).
     * - ToolCallbackProvider is auto-configured by spring-ai-starter-mcp-client and
     *   exposes all tools discovered from connected MCP servers.
     * <p>
     * Spring AI handles the full tool-call loop automatically: when Claude requests a
     * tool, the framework invokes it via MCP and feeds the result back to Claude.
     */
    @Bean
    ChatClient chatClient(ChatClient.Builder chatClientBuilder, ToolCallbackProvider tools) {
        logger.info("Configuring ChatClient with MCP tool callbacks");
        return chatClientBuilder
                .defaultToolCallbacks(tools)
                .build();
    }
}
