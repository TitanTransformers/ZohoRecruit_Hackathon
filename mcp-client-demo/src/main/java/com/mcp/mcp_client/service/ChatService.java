package com.mcp.mcp_client.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mcp.mcp_client.dto.ChatRequest;
import com.mcp.mcp_client.dto.RankedCandidate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ChatService {

    private static final Logger logger = LoggerFactory.getLogger(ChatService.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final Pattern PAGE_SIZE_PATTERN = Pattern.compile(
            "(?:top|first|show|display|list|get|fetch|return|give me|find)\\s+(\\d+)|" +
                    "(\\d+)\\s+(?:candidates|results|records|entries|people|profiles|resumes)",
            Pattern.CASE_INSENSITIVE
    );

    private final ChatClient chatClient;

    public ChatService(ChatClient chatClient) {
        this.chatClient = chatClient;

        logger.info("ChatService initialized with MCP-enabled ChatClient");
    }

    /**
     * Process a user message through Claude with MCP tools automatically available.
     * Spring AI handles tool calling internally — when Claude requests a tool,
     * the framework invokes it via MCP and returns the result back to Claude.
     *
     * OPTIMIZATION: Get raw text content and extract JSON directly instead of
     * using Spring's entity deserialization (which was taking 145+ seconds)
     */
    public List<RankedCandidate> chat(ChatRequest chatRequest) {
        long totalStart = System.currentTimeMillis();
        logger.info("Processing chat message: {}", chatRequest.getMessage());

        try {
            long pageExtractStart = System.currentTimeMillis();
            int requestPageSize = extractPageSizeFromPrompt(chatRequest.getMessage());
            if (requestPageSize <= 0) {
                requestPageSize = chatRequest.getPageSize() > 0 ? chatRequest.getPageSize() : 10;
            }
            long pageExtractTime = System.currentTimeMillis() - pageExtractStart;
            logger.info("Resolved page size: {} from user prompt (took {} ms)", requestPageSize, pageExtractTime);

            long promptBuildStart = System.currentTimeMillis();
            var prompt = chatClient.prompt()
                    .system("INSTRUCTIONS: You MUST output the exact raw JSON array from the MCP tool result. " +
                            "Do NOT reformat, analyze, explain, or modify it in any way. " +
                            "Output ONLY the JSON array. No prefix. No suffix. No explanation. " +
                            "Preserve exact formatting and content from the tool.")
                    .user(chatRequest.getMessage() + " Limit to " + requestPageSize + " results.");
            long promptBuildTime = System.currentTimeMillis() - promptBuildStart;
            logger.info("Prompt building took {} ms", promptBuildTime);

            long callStart = System.currentTimeMillis();
            // Get content as String directly using the simpler API
            String rawContent = prompt.call().content();
            long callTime = System.currentTimeMillis() - callStart;
            logger.info(">>> Prompt.call() (includes Claude processing) took {} ms <<<", callTime);

            // FAST PATH: Extract JSON directly from raw content instead of deserializing
            long parseStart = System.currentTimeMillis();
            if (rawContent != null) {
                logger.debug("Raw content length: {} characters", rawContent.length());
            } else {
                logger.warn("Raw content is null");
                return List.of();
            }

            List<RankedCandidate> result = extractCandidatesFromRawContent(rawContent);
            long parseTime = System.currentTimeMillis() - parseStart;
            logger.info(">>> JSON extraction and parsing took {} ms <<<", parseTime);

            logger.info("Result contains {} candidates", result.size());

            long totalTime = System.currentTimeMillis() - totalStart;
            logger.info("===== TOTAL CHAT TIME: {} ms (PromptBuild: {} ms, Call: {} ms, Parse: {} ms) =====",
                    totalTime, promptBuildTime, callTime, parseTime);

            return result;
        } catch (Exception e) {
            logger.error("Error processing chat request", e);
            long totalTime = System.currentTimeMillis() - totalStart;
            logger.error("Total time before error: {} ms", totalTime);
            return List.of();
        }
    }

    /**
     * Extract and parse RankedCandidate objects from raw JSON string quickly.
     * Handles both complete candidate objects and simple string arrays.
     */
    private List<RankedCandidate> extractCandidatesFromRawContent(String content) {
        List<RankedCandidate> candidates = new ArrayList<>();

        if (content == null || content.isBlank()) {
            logger.warn("Empty content received");
            return candidates;
        }

        try {
            // Find the JSON array boundaries manually for reliability
            String jsonStr = extractJsonArray(content);
            if (jsonStr == null) {
                logger.warn("No JSON array found in content");
                logger.debug("Raw content: {}", content.substring(0, Math.min(500, content.length())));
                return candidates;
            }

            logger.debug("Found JSON array: {} characters", jsonStr.length());
            long deserializeStart = System.currentTimeMillis();
            @SuppressWarnings("unchecked")
            List<Object> parsedList = objectMapper.readValue(jsonStr, List.class);
            long deserializeTime = System.currentTimeMillis() - deserializeStart;
            logger.debug("Array deserialization took {} ms for {} items", deserializeTime, parsedList.size());

            for (int i = 0; i < parsedList.size(); i++) {
                Object item = parsedList.get(i);
                try {
                    RankedCandidate candidate;

                    // Handle different response formats
                    if (item instanceof String) {
                        // Simple string: create a basic RankedCandidate with the name
                        candidate = RankedCandidate.builder()
                                .name((String) item)
                                .candidateId(String.valueOf(i + 1))
                                .rankPosition(i + 1)
                                .build();
                        logger.debug("Created candidate from string: {}", item);
                    } else if (item instanceof Map) {
                        // Object with properties: deserialize as RankedCandidate
                        candidate = objectMapper.convertValue(item, RankedCandidate.class);
                    } else {
                        logger.warn("Unexpected item type: {} (value: {})", item.getClass().getSimpleName(), item);
                        continue;
                    }

                    candidates.add(candidate);
                } catch (Exception e) {
                    logger.warn("Failed to convert item to RankedCandidate: {}", e.getMessage());
                }
            }
        } catch (Exception e) {
            logger.error("Error extracting candidates from raw content: {}", e.getMessage());
        }

        return candidates;
    }

    /**
     * Extract JSON array string by finding matching [ ] brackets.
     * More reliable than regex for large/nested JSON.
     */
    private String extractJsonArray(String content) {
        int start = content.indexOf('[');
        if (start == -1) return null;

        int depth = 0;
        for (int i = start; i < content.length(); i++) {
            char c = content.charAt(i);
            if (c == '[') depth++;
            else if (c == ']') {
                depth--;
                if (depth == 0) {
                    return content.substring(start, i + 1);
                }
            }
        }
        // If brackets don't close (truncated response), return null
        logger.warn("JSON array appears truncated (unclosed brackets)");
        return null;
    }



    /**
     * Extract page size from the user's natural language prompt.
     * Matches patterns like "top 20", "show 30 candidates", "first 15 results", etc.
     * @param message The user's prompt message
     * @return The extracted page size, or -1 if not found
     */
    private int extractPageSizeFromPrompt(String message) {
        if (message == null || message.isBlank()) return -1;
        Matcher matcher = PAGE_SIZE_PATTERN.matcher(message);
        if (matcher.find()) {
            String num = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
            try {
                int size = Integer.parseInt(num);
                logger.info("Extracted page size {} from user prompt: '{}'", size, message);
                return size;
            } catch (NumberFormatException e) {
                // ignore
            }
        }
        return -1;
    }
}
