package com.mcp.mcp_client.controller;

import com.mcp.mcp_client.dto.ChatRequest;
import com.mcp.mcp_client.dto.ChatResponse;
import com.mcp.mcp_client.service.ChatService;
import com.mcp.mcp_client.service.McpToolService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    public ChatController(ChatService chatService, McpToolService mcpToolService) {
        this.chatService = chatService;
        this.mcpToolService = mcpToolService;
    }

    /**
     * Chat endpoint to send message to Claude and get response with MCP tool usage
     * Optimized for performance - minimal logging
     */
    @PostMapping({"/send", "/query"})
    public ChatResponse sendMessage(@RequestBody ChatRequest chatRequest) {
        // Fast path validation
        if (chatRequest == null || chatRequest.getMessage() == null || chatRequest.getMessage().isEmpty()) {
            return ChatResponse.builder().response("Error: Message cannot be empty").build();
        }

        try {
            Object response = chatService.chat(chatRequest);

            // If response is already a PaginatedResponse, wrap it
            if (response instanceof com.mcp.mcp_client.dto.PaginatedResponse) {
                return ChatResponse.builder()
                        .pagination((com.mcp.mcp_client.dto.PaginatedResponse) response)
                        .build();
            }

            return ChatResponse.builder().response(response).build();
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
}
