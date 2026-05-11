package com.mcp.mcp_client.controller;

import com.mcp.mcp_client.dto.ChatRequest;
import com.mcp.mcp_client.dto.ChatResponse;
import com.mcp.mcp_client.service.ChatService;
import com.mcp.mcp_client.service.McpToolService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/documents")
@CrossOrigin(origins = "*", maxAge = 3600)
public class DocumentController {

    private static final Logger logger = LoggerFactory.getLogger(DocumentController.class);

    private final ChatService chatService;
    private final McpToolService mcpToolService;

    public DocumentController(ChatService chatService, McpToolService mcpToolService) {
        this.chatService = chatService;
        this.mcpToolService = mcpToolService;
    }


    @PostMapping(value = "/process", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ChatResponse processDocument(
            @RequestParam(value = "pdf", required = false) MultipartFile pdfFile) {

        try {
            // Process PDF input
            if (pdfFile != null && !pdfFile.isEmpty()) {
                String fileName = pdfFile.getOriginalFilename();
                long fileSize = pdfFile.getSize();

                // Validate PDF
                if (!pdfFile.getContentType().equals("application/pdf")) {
                    return ChatResponse.builder().response("Error: Provided pdf is invalid").build();
                }
            }
            ChatRequest chatRequest = new ChatRequest(extractText(pdfFile.getInputStream()));
            Object response = chatService.chat(chatRequest);
            // If response is already a PaginatedResponse, wrap it
            if (response instanceof com.mcp.mcp_client.dto.PaginatedResponse) {
                return ChatResponse.builder()
                        .pagination((com.mcp.mcp_client.dto.PaginatedResponse) response)
                        .build();
            }
            return ChatResponse.builder().response(response).build();
        } catch (Exception e) {
            logger.error("Error processing document", e);
            return ChatResponse.builder().response("Error: " + e.getMessage()).build();
        }
    }
    public String extractText(InputStream inputStream) {
        try (PDDocument document = PDDocument.load(inputStream)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        } catch (Exception e) {
            throw new RuntimeException("Error reading PDF", e);
        }
    }
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "Document API is healthy");
        return ResponseEntity.ok(response);
    }
}