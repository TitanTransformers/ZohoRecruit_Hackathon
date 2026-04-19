package com.mcp.mcp_client.controller;

import com.mcp.mcp_client.dto.ChatRequest;
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
    public ResponseEntity<Map<String, Object>> processDocument(
            @RequestParam(value = "chatRequest", required = false) ChatRequest chatRequest,
            @RequestParam(value = "pdf", required = false) MultipartFile pdfFile) {

        Map<String, Object> response = new HashMap<>();

        try {
            // Validate input
            if ((chatRequest == null || chatRequest.getMessage() == null || chatRequest.getMessage().isEmpty()) &&
                    (pdfFile == null || pdfFile.isEmpty())) {
                response.put("status", "error");
                response.put("message", "Please provide either a chat request or upload a PDF");
                return ResponseEntity.badRequest().body(response);
            }

            // Process chat request input
            if (chatRequest != null && chatRequest.getMessage() != null && !chatRequest.getMessage().isEmpty()) {
                logger.info("Processing chat request: {}", chatRequest);
                Object chatResponse = chatService.chat(chatRequest);
                response.put("chatProcessed", true);
                response.put("chatMessage", chatRequest.getMessage());
                response.put("chatResponse", chatResponse);
            } else {
                response.put("chatProcessed", false);
            }

            // Process PDF input
            if (pdfFile != null && !pdfFile.isEmpty()) {
                String fileName = pdfFile.getOriginalFilename();
                long fileSize = pdfFile.getSize();

                // Validate PDF
                if (!pdfFile.getContentType().equals("application/pdf")) {
                    response.put("status", "error");
                    response.put("message", "File must be a PDF");
                    return ResponseEntity.badRequest().body(response);
                }

                response.put("pdfProcessed", true);
                response.put("pdfFileName", fileName);
                response.put("pdfFileSize", fileSize);
                response.put("pdfSizeMB", String.format("%.2f", fileSize / (1024.0 * 1024.0)));
            } else {
                response.put("pdfProcessed", false);
            }

            response.put("status", "success");
            response.put("message", "Document processed successfully");
            response.put("timestamp", System.currentTimeMillis());
            response.put("content", extractText(pdfFile.getInputStream()));
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error processing document", e);
            response.put("status", "error");
            response.put("message", "Error processing document: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
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