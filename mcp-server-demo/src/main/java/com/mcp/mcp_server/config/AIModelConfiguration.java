package com.mcp.mcp_server.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Spring AI Configuration for Claude Haiku integration
 *
 * This configuration sets up the ChatClient for use with Anthropic's Claude models
 * via Spring AI's Anthropic starter. Requires ANTHROPIC_API_KEY environment variable.
 */
@Slf4j
@Configuration
@EnableConfigurationProperties
public class AIModelConfiguration {

    /**
     * Configure ChatClient for Claude Haiku
     * Spring AI will auto-configure ChatModel based on spring.ai.anthropic properties
     *
     * @param builder ChatClient builder
     * @param chatModel Anthropic ChatModel (auto-configured by Spring AI)
     * @return configured ChatClient
     */
    @Bean
    public ChatClient chatClient(ChatClient.Builder builder, ChatModel chatModel) {
        log.info("Initializing ChatClient with Anthropic Claude model");

        // Validate that ChatModel is properly initialized
        if (chatModel == null) {
            log.error("ChatModel is null - Anthropic API key may not be configured properly");
            throw new IllegalStateException("ChatModel not initialized. Please ensure ANTHROPIC_API_KEY environment variable is set.");
        }

        log.debug("ChatModel class: {}", chatModel.getClass().getName());

        return builder
                .defaultSystem("You are a helpful AI assistant specialized in recruitment and job analysis.")
                .build();
    }

    /**
     * ObjectMapper for JSON processing in AI services
     */
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    /**
     * RestTemplate for HTTP calls
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}

