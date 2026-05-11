package com.mcp.mcp_client.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class LlmCacheConfig {

    @Bean
    public Cache<String, ChatClientResponse> llmCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(2)) // ✅ 2 min TTL
                .maximumSize(10_000)
                .recordStats()
                .build();
    }
}