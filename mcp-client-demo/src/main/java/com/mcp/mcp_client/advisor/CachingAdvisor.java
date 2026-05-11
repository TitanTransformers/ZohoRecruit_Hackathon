package com.mcp.mcp_client.advisor;

import com.github.benmanes.caffeine.cache.Cache;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

public class CachingAdvisor implements CallAdvisor {

    private final Cache<String, ChatClientResponse> cache;

    public CachingAdvisor(Cache<String, ChatClientResponse> cache) {
        this.cache = cache;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request,
                                         CallAdvisorChain chain) {

        if (shouldSkipCache(request)) {
            return chain.nextCall(request);
        }

        String key = buildCacheKey(request);

        // ✅ CACHE HIT
        ChatClientResponse cached = cache.getIfPresent(key);
        if (cached != null) {
            System.out.println("✅ CACHE HIT");
            return cached;
        }

        System.out.println("❌ CACHE MISS");

        // 🚀 CALL LLM
        ChatClientResponse response = chain.nextCall(request);

        // ✅ STORE RESPONSE
        if (response != null) {
            cache.put(key, response);
        }

        return response;
    }

    @Override
    public String getName() {
        return "caching-advisor";
    }

    @Override
    public int getOrder() {
        return 0; // run early
    }

    // ------------------------
    // Helper Methods
    // ------------------------

    private String buildCacheKey(ChatClientRequest request) {
        String prompt = extractPrompt(request);
        String normalized = normalize(prompt);

        // Optional: include model manually if fixed
        String model = "claude"; // adjust if needed

        return hash(normalized + "|" + model);
    }

    private String extractPrompt(ChatClientRequest request) {
        try {
            return request.prompt() != null
                    ? request.prompt().toString()
                    : "";
        } catch (Exception e) {
            return "";
        }
    }

    private String normalize(String input) {
        if (input == null) return "";
        return input.trim()
                .toLowerCase()
                .replaceAll("\\s+", " ");
    }

    private boolean shouldSkipCache(ChatClientRequest request) {
        String prompt = extractPrompt(request);

        return prompt == null
                || prompt.contains("userid")
                || prompt.contains("timestamp")
                || prompt.length() > 5000;
    }

    private String hash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to hash cache key", e);
        }
    }
}