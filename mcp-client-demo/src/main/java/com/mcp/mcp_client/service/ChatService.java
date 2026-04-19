package com.mcp.mcp_client.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mcp.mcp_client.dto.ChatRequest;
import com.mcp.mcp_client.dto.ChatResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ChatService {

    private static final Logger logger = LoggerFactory.getLogger(ChatService.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final Pattern JSON_CODE_BLOCK_PATTERN = Pattern.compile("```json\\s*\\n([\\s\\S]*?)\\n```");
    private static final Pattern XML_CODE_BLOCK_PATTERN = Pattern.compile("```xml\\s*\\n([\\s\\S]*?)\\n```");

    private final ChatClient chatClient;

    public ChatService(ChatClient chatClient) {
        this.chatClient = chatClient;
        logger.info("ChatService initialized with MCP-enabled ChatClient");
    }

    /**
     * Process a user message through Claude with MCP tools automatically available.
     * Spring AI handles tool calling internally — when Claude requests a tool,
     * the framework invokes it via MCP and returns the result back to Claude.
     */
    public Object chat(ChatRequest chatRequest) {
        logger.info("Processing chat message: {}", chatRequest.getMessage());

        try {
            String response = chatClient.prompt()
                    .system("You are a helpful AI assistant with access to MCP (Model Context Protocol) tools. " +
                            "Use the available tools when needed to answer the user's questions accurately.")
                    .user(chatRequest.getMessage())
                    .call()
                    .content();

            logger.info("Claude response received successfully");
            return response;

        } catch (Exception e) {
            logger.error("Error processing chat request", e);
            return ChatResponse.builder().response(
                    "Error processing your request: " + e.getMessage()
            ).build();
        }
    }

    /**
     * Extract JSON data from markdown code blocks in the response.
     * Returns the parsed JSON object if found, otherwise null.
     */
    private Object extractJsonFromResponse(String response) {
        try {
            Matcher matcher = JSON_CODE_BLOCK_PATTERN.matcher(response);
            
            if (matcher.find()) {
                String jsonStr = matcher.group(1).trim();
                logger.debug("Found JSON code block, parsing...");
                
                try {
                    // Try to parse as JSON
                    JsonNode jsonNode = objectMapper.readTree(jsonStr);
                    logger.debug("Successfully parsed JSON data");
                    
                    // If it's an array or object, return the parsed structure
                    if (jsonNode.isArray() || jsonNode.isObject()) {
                        return objectMapper.readValue(jsonStr, Object.class);
                    }
                } catch (Exception e) {
                    logger.warn("Failed to parse extracted JSON: {}", e.getMessage());
                }
            }
        } catch (Exception e) {
            logger.debug("Error extracting JSON from response: {}", e.getMessage());
        }
        
        return null;
    }

    /**
     * Extract XML data from markdown code blocks in the response.
     * Returns the XML as a string (NOT converted to JSON).
     * Returns null if no XML found.
     */
    private String extractXmlFromResponse(String response) {
        try {
            Matcher matcher = XML_CODE_BLOCK_PATTERN.matcher(response);
            
            if (matcher.find()) {
                String xmlStr = matcher.group(1).trim();
                logger.debug("Found XML code block");

                // Validate it's valid XML
                try {
                    javax.xml.parsers.DocumentBuilderFactory.newInstance()
                            .newDocumentBuilder()
                            .parse(new org.xml.sax.InputSource(new java.io.StringReader(xmlStr)));
                    logger.debug("Successfully validated XML data");
                    return xmlStr;
                } catch (Exception e) {
                    logger.warn("Failed to validate extracted XML: {}", e.getMessage());
                }
            }
        } catch (Exception e) {
            logger.debug("Error extracting XML from response: {}", e.getMessage());
        }
        
        return null;
    }
}
