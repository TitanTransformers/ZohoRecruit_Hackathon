package com.mcp.mcp_client.controller;

import com.mcp.mcp_client.dto.ChatRequest;
import com.mcp.mcp_client.dto.ChatResponse;
import com.mcp.mcp_client.dto.RankedCandidate;
import com.mcp.mcp_client.service.ChatService;
import com.mcp.mcp_client.service.McpToolService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*", maxAge = 3600)
public class ChatController {

    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);

    private final ChatService chatService;
    private final McpToolService mcpToolService;
    private final ChatClient chatClient;

    public ChatController(ChatService chatService, McpToolService mcpToolService, ChatClient chatClient) {
        this.chatService = chatService;
        this.mcpToolService = mcpToolService;
        this.chatClient = chatClient;
    }

    /**
     * Chat endpoint to send message to Claude and get response with MCP tool usage
     * Optimized for performance - minimal logging
     */
    @PostMapping({"/send", "/query"})
    public ChatResponse sendMessage(@RequestBody ChatRequest chatRequest) {
        long requestStart = System.currentTimeMillis();
        
        // Fast path validation
        if (chatRequest == null || chatRequest.getMessage() == null || chatRequest.getMessage().isEmpty()) {
            return ChatResponse.builder().response("Error: Message cannot be empty").build();
        }

        try {
            long validationEnd = System.currentTimeMillis();
            logger.info("Request validation took {} ms", validationEnd - requestStart);

            long serviceCallStart = System.currentTimeMillis();
            List<RankedCandidate> response = chatService.chat(chatRequest);
            long serviceCallEnd = System.currentTimeMillis();
            logger.info("ChatService.chat() took {} ms", serviceCallEnd - serviceCallStart);

            long responseBuildStart = System.currentTimeMillis();
            ChatResponse chatResponse = ChatResponse.builder().response(response).build();
            long responseBuildEnd = System.currentTimeMillis();
            logger.info("ChatResponse building took {} ms", responseBuildEnd - responseBuildStart);

            long totalTime = System.currentTimeMillis() - requestStart;
            logger.info("===== TOTAL CONTROLLER TIME: {} ms =====", totalTime);

            return chatResponse;
        } catch (Exception e) {
            logger.error("Chat error: {}", e.getMessage());
            return ChatResponse.builder().response("Error: " + e.getMessage()).build();
        }
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }

    /**
     * Get available MCP tools
     */
    @GetMapping("/tools")
    public ResponseEntity<List<Map<String, String>>> getAvailableTools() {
        return ResponseEntity.ok(mcpToolService.getAvailableTools());
    }

    /**
     * Direct MCP endpoint - bypasses Claude entirely
     * Calls MCP tools directly and returns raw response
     * Much faster than going through Claude processing
     * Uses same request payload as /send endpoint
     *
     * @param chatRequest ChatRequest with message and optional pageSize
     * @return Raw MCP tool response as string
     */
    @PostMapping("/direct")
    public ResponseEntity<String> directMcp(@RequestBody ChatRequest chatRequest) {
        long startTime = System.currentTimeMillis();

        if (chatRequest == null || chatRequest.getMessage() == null || chatRequest.getMessage().isEmpty()) {
            return ResponseEntity.badRequest().body("Error: Message cannot be empty");
        }

        logger.info("Direct MCP call - Query: {}, PageSize: {}", chatRequest.getMessage(), chatRequest.getPageSize());

        try {
            int pageSize = chatRequest.getPageSize() > 0 ? chatRequest.getPageSize() : 10;

            // Call MCP tools directly via ChatClient (without Claude processing)
            String result = chatClient.prompt()
                    .system("You are a data passthrough. Call the MCP tool with the user query. " +
                            "Return ONLY the raw JSON response from the tool, nothing else.")
                    .user(chatRequest.getMessage() + ". Limit to " + pageSize + " results.")
                    .call()
                    .content();

            // Remove markdown code block formatting if present
            if (result != null) {
                result = result.trim();
                // Remove ```json prefix if present
                if (result.startsWith("```json")) {
                    result = result.substring(7).trim();
                } else if (result.startsWith("```")) {
                    result = result.substring(3).trim();
                }
                // Remove ``` suffix if present
                if (result.endsWith("```")) {
                    result = result.substring(0, result.length() - 3).trim();
                }
            }

            long totalTime = System.currentTimeMillis() - startTime;
            logger.info("Direct MCP call completed in {} ms", totalTime);

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Direct MCP call failed", e);
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }
}
