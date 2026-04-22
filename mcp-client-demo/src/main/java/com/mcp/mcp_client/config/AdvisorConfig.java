package com.mcp.mcp_client.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.mcp.mcp_client.advisor.CachingAdvisor;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AdvisorConfig {

    @Bean
    public CachingAdvisor cachingAdvisor(Cache<String, ChatClientResponse> cache) {
        return new CachingAdvisor(cache);
    }
}