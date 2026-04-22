package com.mcp.mcp_client.config;

import com.mcp.mcp_client.advisor.CachingAdvisor;
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
     * Spring AI auto-configures the Anthropic client and handles timeouts.
     */
    @Bean
    ChatClient chatClient(ChatClient.Builder chatClientBuilder, ToolCallbackProvider tools, CachingAdvisor cachingAdvisor) {
        logger.info("Configuring ChatClient with MCP tool callbacks");
        return chatClientBuilder
                .defaultAdvisors(cachingAdvisor)
                .defaultToolCallbacks(tools)
                .build();
    }
}
