package com.mcp.mcp_client.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mcp.mcp_client.dto.ChatRequest;
import com.mcp.mcp_client.dto.ChatResponse;
import com.mcp.mcp_client.dto.PaginatedResponse;
import com.mcp.mcp_client.dto.RankedCandidate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.ParameterizedTypeReference;
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
            List<RankedCandidate> response = chatClient.prompt()
                    .system("You are a helpful AI assistant with access to MCP (Model Context Protocol) tools. " +
                            "Use the available tools when needed to answer the user's questions accurately.")
                    .user(chatRequest.getMessage())
                    .call()
                    .entity(new ParameterizedTypeReference<>() {
                    });

            logger.info("Claude response received successfully");

            // Extract JSON data from response and convert to RankedCandidate list
            List<RankedCandidate> candidates = extractCandidatesFromResponse(response);

            // Set page size: use the smaller of request page size or total candidates count
            // This ensures we get meaningful pagination based on actual data
            int requestPageSize = chatRequest.getPageSize() > 0 ? chatRequest.getPageSize() : 10;
            int pageSize = Math.min(requestPageSize, Math.max(candidates.size() / 5, 5)); // Min 5 items per page
            if (candidates.size() <= requestPageSize) {
                pageSize = candidates.size(); // If total is less than requested, use total
            } else {
                pageSize = requestPageSize; // Use requested page size
            }

            logger.info("Setting page size to {} for {} total candidates", pageSize, candidates.size());

            // Create paginated response for the first page
            PaginatedResponse paginatedResponse = createPaginatedResponse(candidates, 0, pageSize);

            return paginatedResponse;

        } catch (Exception e) {
            logger.error("Error processing chat request", e);
            return ChatResponse.builder().response(
                    "Error processing your request: " + e.getMessage()
            ).build();
        }
    }

    /**
     * Extract candidates from the Claude response (from JSON code blocks or direct object)
     * @param response The response text from Claude or the response object
     * @return List of RankedCandidate objects
     */
    private List<RankedCandidate> extractCandidatesFromResponse(Object response) {
        List<RankedCandidate> candidates = new ArrayList<>();

        try {
            // Check if response is already a List of RankedCandidate
            if (response instanceof List) {
                List<?> dataList = (List<?>) response;
                for (Object item : dataList) {
                    if (item instanceof RankedCandidate) {
                        candidates.add((RankedCandidate) item);
                    } else {
                        // Try to convert from map/json
                        RankedCandidate candidate = objectMapper.convertValue(item, RankedCandidate.class);
                        candidates.add(candidate);
                    }
                }
                logger.info("Extracted {} candidates from response", candidates.size());
            } else if (response instanceof String) {
                // If it's a string, try to extract JSON from code blocks
                String responseStr = (String) response;
                Object extractedData = extractJsonFromResponse(responseStr);

                if (extractedData instanceof List) {
                    List<?> dataList = (List<?>) extractedData;
                    for (Object item : dataList) {
                        RankedCandidate candidate = objectMapper.convertValue(item, RankedCandidate.class);
                        candidates.add(candidate);
                    }
                    logger.info("Extracted {} candidates from JSON response", candidates.size());
                } else if (extractedData instanceof Map) {
                    // If it's a single object, wrap it in a list
                    RankedCandidate candidate = objectMapper.convertValue(extractedData, RankedCandidate.class);
                    candidates.add(candidate);
                    logger.info("Extracted 1 candidate from response");
                }
            } else if (response instanceof Map) {
                // Handle single candidate as Map
                RankedCandidate candidate = objectMapper.convertValue(response, RankedCandidate.class);
                candidates.add(candidate);
                logger.info("Extracted 1 candidate from response");
            }
        } catch (Exception e) {
            logger.warn("Failed to extract candidates from response: {}", e.getMessage());
        }

        return candidates;
    }

    /**
     * Create a paginated response from a list of candidates
     * @param allCandidates The complete list of candidates
     * @param pageNumber The page number (0-indexed)
     * @param pageSize The size of each page
     * @return PaginatedResponse object with pagination metadata
     */
    private PaginatedResponse createPaginatedResponse(List<RankedCandidate> allCandidates, int pageNumber, int pageSize) {
        logger.debug("Creating paginated response for page {} with page size {}", pageNumber, pageSize);

        int totalItems = allCandidates.size();
        int totalPages = (int) Math.ceil((double) totalItems / pageSize);

        int startIndex = pageNumber * pageSize;
        int endIndex = Math.min(startIndex + pageSize, totalItems);

        // Get candidates for this page
        List<RankedCandidate> pageCandidates = allCandidates.subList(startIndex, endIndex);

        boolean hasNext = pageNumber < totalPages - 1;
        boolean hasPrevious = pageNumber > 0;

        return PaginatedResponse.builder()
                .candidates(pageCandidates)
                .page(pageNumber)
                .pageSize(pageSize)
                .totalPages(totalPages)
                .totalItems(totalItems)
                .hasNext(hasNext)
                .hasPrevious(hasPrevious)
                .build();
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
