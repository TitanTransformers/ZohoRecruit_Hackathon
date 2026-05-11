package com.mcp.mcp_server.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
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
     * ObjectMapper for JSON processing in AI services.
     * Defined explicitly as the MCP server starter may not trigger Jackson auto-configuration.
     */
    @Bean
    @ConditionalOnMissingBean
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        // Be lenient with AI-generated JSON
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
        objectMapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
        objectMapper.configure(JsonParser.Feature.ALLOW_COMMENTS, true);
        objectMapper.configure(JsonParser.Feature.ALLOW_TRAILING_COMMA, true);
        return objectMapper;
    }

    /**
     * RestTemplate with extended connection and read timeouts for Zoho Recruit API calls
     * Zoho API can be slow when searching large candidate pools or during peak load.
     *
     * Timeouts:
     * - Connect: 5 seconds (establish TCP connection)
     * - Read: 60 seconds (wait for response from Zoho server)
     *
     * If timeouts still occur:
     * - Increase ZOHO_READ_TIMEOUT environment variable (in milliseconds)
     * - Reduce pageSize in search requests
     * - Consider pagination for large result sets
     */
    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5_000);    // 5 seconds to establish connection
        factory.setReadTimeout(60_000);      // 60 seconds to read response (increased from 15s)
        return new RestTemplate(factory);
    }
}

