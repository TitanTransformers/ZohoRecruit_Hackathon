# Backend Integration Guide for Zoho Recruit Sourcing Tool

## Overview
This guide explains how to update the Spring Boot backend to work with the new Zoho Recruit Sourcing Tool UI.

## Expected Behavior

### Input Processing
The backend should:
1. Accept either JSON or FormData on the same endpoint
2. Extract the Job Description (text or PDF)
3. Search the Zoho Recruit ATS database
4. Return matching candidate profiles

### Output Requirements
The backend should return:
- **Array of candidates** with dynamic properties based on your candidate model
- Each candidate object becomes a row in the results table
- Each property of the candidate object becomes a column

## Backend Implementation Example

### Current Endpoint Structure
```java
@RestController
@RequestMapping("/api/documents")
@CrossOrigin(origins = "*")
public class DocumentController {
    
    @PostMapping("/process")
    public ResponseEntity<?> processDocument(
            @RequestParam(required = false) String message,
            @RequestParam(required = false) MultipartFile pdf) {
        // Implementation here
    }
}
```

### Updated Implementation

```java
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import java.io.IOException;

@RestController
@RequestMapping("/api/documents")
@CrossOrigin(origins = "*")
public class DocumentController {

    // Inject your Zoho Recruit ATS service
    // @Autowired
    // private ZohoRecruitService zohoRecruitService;

    @PostMapping("/process")
    public ResponseEntity<?> processDocument(
            @RequestParam(required = false) String message,
            @RequestParam(required = false) MultipartFile pdf) {
        
        try {
            // Extract Job Description
            String jobDescription = extractJobDescription(message, pdf);
            
            if (jobDescription == null || jobDescription.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(new ErrorResponse("Please provide a job description"));
            }

            // Search Zoho Recruit ATS
            // Replace this with actual Zoho API call
            List<CandidateProfile> candidates = searchZohoATS(jobDescription);

            // Return as array directly
            return ResponseEntity.ok(candidates);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Error processing job description: " + e.getMessage()));
        }
    }

    /**
     * Extract job description from text or PDF
     */
    private String extractJobDescription(String message, MultipartFile pdf) 
            throws IOException {
        if (message != null && !message.trim().isEmpty()) {
            return message;
        }
        
        if (pdf != null && !pdf.isEmpty()) {
            // Implement PDF extraction
            return extractTextFromPDF(pdf);
        }
        
        return null;
    }

    /**
     * Extract text from PDF (requires Apache PDFBox or similar)
     */
    private String extractTextFromPDF(MultipartFile pdf) throws IOException {
        // Implementation using PDFBox or similar library
        // Example:
        // PDDocument document = PDDocument.load(pdf.getInputStream());
        // PDFTextStripper stripper = new PDFTextStripper();
        // String text = stripper.getText(document);
        // document.close();
        // return text;
        
        // Placeholder for now
        return "PDF content extracted";
    }

    /**
     * Search Zoho Recruit ATS for matching candidates
     */
    private List<CandidateProfile> searchZohoATS(String jobDescription) {
        // TODO: Implement actual Zoho API integration
        // This is a mock implementation for testing
        
        List<CandidateProfile> candidates = new ArrayList<>();
        
        // Example: Return mock candidates (replace with actual API call)
        candidates.add(new CandidateProfile(
            "C001",
            "John",
            "Doe",
            "john.doe@example.com",
            "+1-555-0123",
            "Java, Spring Boot, Microservices",
            "5 years",
            "92%"
        ));
        
        candidates.add(new CandidateProfile(
            "C002",
            "Jane",
            "Smith",
            "jane.smith@example.com",
            "+1-555-0456",
            "React, TypeScript, Node.js",
            "4 years",
            "88%"
        ));
        
        // TODO: Call actual Zoho Recruit API
        // List<CandidateProfile> candidates = zohoRecruitService.searchCandidates(jobDescription);
        
        return candidates;
    }

    /**
     * Candidate Profile Model
     * Customize fields based on your Zoho Recruit ATS schema
     */
    public static class CandidateProfile {
        public String candidateId;
        public String firstName;
        public String lastName;
        public String email;
        public String phone;
        public String skills;
        public String experience;
        public String matchScore;

        // Constructor
        public CandidateProfile(String candidateId, String firstName, String lastName,
                              String email, String phone, String skills,
                              String experience, String matchScore) {
            this.candidateId = candidateId;
            this.firstName = firstName;
            this.lastName = lastName;
            this.email = email;
            this.phone = phone;
            this.skills = skills;
            this.experience = experience;
            this.matchScore = matchScore;
        }

        // Getters (required for JSON serialization)
        public String getCandidateId() { return candidateId; }
        public String getFirstName() { return firstName; }
        public String getLastName() { return lastName; }
        public String getEmail() { return email; }
        public String getPhone() { return phone; }
        public String getSkills() { return skills; }
        public String getExperience() { return experience; }
        public String getMatchScore() { return matchScore; }
    }

    /**
     * Error Response Model
     */
    public static class ErrorResponse {
        public String error;

        public ErrorResponse(String error) {
            this.error = error;
        }

        public String getError() { return error; }
    }
}
```

## Zoho Recruit API Integration

### Example: Using Zoho Recruit SDK

```java
import com.zoho.crm.api.v2.*;
import com.zoho.crm.api.v2.users.User;
import com.zoho.crm.api.v2.search.*;

@Service
public class ZohoRecruitService {

    /**
     * Search candidates in Zoho Recruit based on job description
     */
    public List<CandidateProfile> searchCandidates(String jobDescription) 
            throws Exception {
        
        // Initialize Zoho CRM API
        // Configure: auth token, domain, module name
        
        List<CandidateProfile> results = new ArrayList<>();
        
        // Example: Search using Zoho's search API
        // String query = buildSearchQuery(jobDescription);
        // SearchRecords searchRequest = new SearchRecords();
        // searchRequest.setModuleAPIName("Candidates");
        // searchRequest.setCriteria(query);
        
        // BulkRead bulkRead = new BulkRead();
        // List<SearchResult> records = bulkRead.search(searchRequest);
        
        // // Map results to CandidateProfile
        // for (SearchResult record : records) {
        //     CandidateProfile candidate = mapZohoCandidateToCandidateProfile(record);
        //     results.add(candidate);
        // }
        
        return results;
    }

    /**
     * Build search query from job description
     * Extract keywords and search Zoho Recruit
     */
    private String buildSearchQuery(String jobDescription) {
        // Extract keywords like skills, experience, location, etc.
        // Build Zoho search criteria
        // Example: ((Skills contains "Java") OR (Skills contains "Spring Boot"))
        
        return jobDescription; // Simplified for now
    }
}
```

## Database Model (Example)

```java
import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "candidates")
public class Candidate {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "zoho_candidate_id")
    private String zohoCandidateId;
    
    @Column(name = "first_name")
    private String firstName;
    
    @Column(name = "last_name")
    private String lastName;
    
    @Column(name = "email")
    private String email;
    
    @Column(name = "phone")
    private String phone;
    
    @Column(name = "skills")
    private String skills;
    
    @Column(name = "experience")
    private String experience;
    
    @Column(name = "current_location")
    private String currentLocation;
    
    @Column(name = "current_employer")
    private String currentEmployer;
    
    @Column(name = "current_job_title")
    private String currentJobTitle;
    
    @Column(name = "match_percentage")
    private Double matchPercentage;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    // Getters and Setters
}
```

## Response Format

### Recommended Response (Array)
```json
[
  {
    "candidateId": "C001",
    "firstName": "John",
    "lastName": "Doe",
    "email": "john.doe@example.com",
    "phone": "+1-555-0123",
    "skills": "Java, Spring Boot, Microservices",
    "experience": "5 years",
    "matchScore": "92%"
  },
  {
    "candidateId": "C002",
    "firstName": "Jane",
    "lastName": "Smith",
    "email": "jane.smith@example.com",
    "phone": "+1-555-0456",
    "skills": "React, TypeScript, Node.js",
    "experience": "4 years",
    "matchScore": "88%"
  }
]
```

### Alternative Response (Nested)
```json
{
  "results": [
    { /* candidate 1 */ },
    { /* candidate 2 */ }
  ]
}
```

## Testing the Endpoint

### Using Postman

1. **Create a new POST request**
   - URL: `http://localhost:8081/api/documents/process`

2. **Test with JSON (Text Only)**
   - Headers: `Content-Type: application/json`
   - Body (raw):
   ```json
   {
     "message": "Senior Java Developer needed. 5+ years experience, Spring Boot, Microservices, AWS."
   }
   ```

3. **Test with FormData (PDF)**
   - Headers: `Content-Type: multipart/form-data`
   - Body:
     - `message` (text): Optional job description
     - `pdf` (file): Upload job_description.pdf

### Using cURL

```bash
# Test JSON
curl -X POST http://localhost:8081/api/documents/process \
  -H "Content-Type: application/json" \
  -d '{"message":"Senior Java Developer needed"}'

# Test with PDF
curl -X POST http://localhost:8081/api/documents/process \
  -F "pdf=@job_description.pdf"

# Test with both
curl -X POST http://localhost:8081/api/documents/process \
  -F "message=Senior Java Developer" \
  -F "pdf=@job_description.pdf"
```

## Dependencies to Add

### pom.xml
```xml
<!-- PDF Processing (if needed) -->
<dependency>
    <groupId>org.apache.pdfbox</groupId>
    <artifactId>pdfbox</artifactId>
    <version>2.0.28</version>
</dependency>

<!-- Jackson for JSON (usually included in Spring Boot) -->
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
</dependency>

<!-- Zoho CRM SDK (if using official SDK) -->
<dependency>
    <groupId>com.zoho.crm</groupId>
    <artifactId>zohocrmsdk</artifactId>
    <version>3.0.0</version>
</dependency>
```

## Configuration (application.properties)

```properties
# Zoho Recruit Configuration
zoho.recruit.client.id=YOUR_CLIENT_ID
zoho.recruit.client.secret=YOUR_CLIENT_SECRET
zoho.recruit.refresh.token=YOUR_REFRESH_TOKEN
zoho.recruit.api.domain=https://www.zohoapis.com

# CORS Configuration
server.servlet.context-path=/api
```

## Debugging Tips

1. **Enable Request Logging**
   ```java
   @Configuration
   public class WebConfig implements WebMvcConfigurer {
       @Override
       public void addInterceptors(InterceptorRegistry registry) {
           registry.addInterceptor(new HttpLoggingInterceptor());
       }
   }
   ```

2. **Check API Response Format**
   - Ensure your API returns proper JSON
   - Use `curl` to test before using UI

3. **Monitor Network Traffic**
   - Open browser DevTools
   - Check Network tab for request/response
   - Verify response headers and body

## Security Considerations

1. **CORS**: Already configured with `@CrossOrigin(origins = "*")`
2. **Input Validation**: Validate job description length and format
3. **File Upload**: Validate PDF file size and type
4. **Rate Limiting**: Consider adding rate limits for API calls
5. **Authentication**: Add JWT/OAuth if using real Zoho API

## Next Steps

1. Implement actual Zoho Recruit API integration
2. Add error handling and validation
3. Implement PDF text extraction
4. Add caching for search results
5. Add pagination for large result sets
6. Implement result filtering and sorting
7. Add audit logging
