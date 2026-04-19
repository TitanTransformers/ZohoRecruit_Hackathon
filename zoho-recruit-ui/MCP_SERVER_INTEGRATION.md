# MCP Server Integration Guide

## Backend Controller Setup

To integrate the document processing API with your Spring Boot MCP server, follow these steps:

### Step 1: Add Controller Class

Create a new file in your MCP server project at:
```
src/main/java/com/mcp/mcp_server/controller/DocumentController.java
```

Copy the content from `DocumentController.java` in the root of this project.

### Step 2: Ensure Spring Web Dependency

Verify your `pom.xml` includes:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
```

### Step 3: Configure CORS

Add CORS configuration to your Spring Boot application. Create or update a configuration class:

```java
package com.mcp.mcp_server.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
            .allowedOrigins("*")
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
            .allowedHeaders("*")
            .maxAge(3600);
    }
}
```

### Step 4: Configure Application Properties

Update `application.yaml` to enable multipart file uploads:

```yaml
spring:
  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 10MB
  web:
    cors:
      allowed-origins: "*"
      allowed-methods: GET,POST,PUT,DELETE,OPTIONS
      max-age: 3600
```

### Step 5: Build and Run

```bash
mvn clean install
mvn spring-boot:run
```

The API will be available at `http://localhost:8080/api/documents/process`

## API Endpoints

### POST /api/documents/process

**Description**: Process a document by accepting text and/or PDF file.

**Request**:
- Content-Type: `multipart/form-data`
- Fields:
  - `text` (optional): String - Plain text content
  - `pdf` (optional): File - PDF file to upload

**Response** (Success - 200 OK):
```json
{
  "status": "success",
  "message": "Document processed successfully",
  "textProcessed": true,
  "textLength": 250,
  "textPreview": "The first 100 characters of the text...",
  "pdfProcessed": false,
  "timestamp": 1703123456789
}
```

**Response** (Error - 400 Bad Request):
```json
{
  "status": "error",
  "message": "Please provide either text or upload a PDF"
}
```

### GET /api/documents/health

**Description**: Health check endpoint for the document API.

**Response** (200 OK):
```json
{
  "status": "Document API is healthy"
}
```

## Testing the Integration

### Using cURL

```bash
# Test with text
curl -X POST http://localhost:8080/api/documents/process \
  -F "text=This is a test document"

# Test with PDF
curl -X POST http://localhost:8080/api/documents/process \
  -F "pdf=@/path/to/document.pdf"

# Test with both
curl -X POST http://localhost:8080/api/documents/process \
  -F "text=Some text" \
  -F "pdf=@/path/to/document.pdf"
```

### Using Postman

1. Create a new POST request to `http://localhost:8080/api/documents/process`
2. Set the body type to `form-data`
3. Add the fields:
   - `text` (text field) - optional
   - `pdf` (file field) - optional
4. Click Send

## Troubleshooting

### CORS Errors

If you see CORS errors in the browser console, ensure:
1. The CorsConfig class is present in the Spring Boot application
2. The controller has `@CrossOrigin` annotation
3. Check application.yaml has proper CORS configuration

### File Upload Errors

If file uploads fail:
1. Check the multipart configuration in application.yaml
2. Verify file size limits are appropriate
3. Ensure the PDF file is valid

### Connection Refused

If you get "Connection Refused" errors:
1. Ensure the Spring Boot application is running on port 8080
2. Check firewall settings
3. Update the API endpoint URL in the React application if needed

## Production Deployment

Before deploying to production:

1. **Update API Endpoint**: Change the hardcoded URL in `DocumentUploadPage.tsx` to your production server
2. **Restrict CORS Origins**: Update CorsConfig to only allow your frontend domain
3. **Add Authentication**: Consider adding JWT or OAuth2 authentication
4. **Add Validation**: Implement stricter file validation and scanning
5. **Add Logging**: Implement comprehensive logging for audit trails
6. **SSL/TLS**: Ensure HTTPS is enabled for production

## Enhanced Controller Example

For production, you may want to enhance the controller:

```java
@RestController
@RequestMapping("/api/documents")
@CrossOrigin(origins = "https://your-domain.com")
public class DocumentController {

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final Logger logger = LoggerFactory.getLogger(DocumentController.class);

    @PostMapping("/process")
    public ResponseEntity<Map<String, Object>> processDocument(
            @RequestParam(required = false) String text,
            @RequestParam(required = false) MultipartFile pdf) {
        
        logger.info("Processing document request");
        
        // Add authentication check
        // Add file scanning (antivirus)
        // Add database persistence
        // Add async processing
        
        // ... rest of implementation
    }
}
```

## Next Steps

1. Test the integration locally
2. Set up proper error handling and logging
3. Implement document storage (database or file system)
4. Add additional processing logic based on requirements
5. Set up CI/CD pipeline for deployment
