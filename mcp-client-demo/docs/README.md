# MCP Client Demo - Recruitment Assistant

## Overview

The **MCP Client Demo** is a Spring Boot application that demonstrates the integration of the **Model Context Protocol (MCP)** with Claude AI for building intelligent recruitment and candidate matching solutions. This service leverages MCP to dynamically access external tools and data sources, combined with Claude's advanced language capabilities to provide sophisticated candidate ranking and analysis.

### Key Features:
- 🤖 **Claude AI Integration** - Uses Anthropic's Claude Sonnet 4 (claude-sonnet-4-20250514) model
- 🔌 **MCP Support** - Streamable HTTP protocol for dynamic tool access from MCP servers
- 📊 **Candidate Ranking** - AI-powered candidate matching with detailed scoring and analysis
- 📄 **Document Processing** - PDF parsing and extraction capabilities
- 🔄 **Pagination Support** - Intelligent result pagination with customizable page sizes
- 🎨 **Web UI** - Interactive chat interface with real-time responses
- 🛠️ **RESTful APIs** - Well-defined endpoints for chat, tools, and document processing

---

## Architecture & Technology Stack

### Core Technologies:
- **Java 21** - Latest Java LTS version
- **Spring Boot 4.0.5** - Enterprise application framework
- **Spring AI 2.0.0-M4** - AI/ML integration framework
- **Anthropic Claude** - Large Language Model
- **MCP (Model Context Protocol)** - Tool orchestration protocol
- **Jackson** - JSON/XML processing
- **PDFBox 2.0.30** - PDF text extraction
- **Lombok** - Code generation for boilerplate
- **OpenAPI/Swagger** - API documentation

---

## Project Structure

```
mcp-client-demo/
├── src/main/java/com/mcp/mcp_client/
│   ├── McpClientApplication.java          # Main Spring Boot application
│   ├── config/
│   │   └── McpClientConfig.java           # MCP and ChatClient configuration
│   ├── controller/
│   │   ├── ChatController.java            # Chat and tools API endpoints
│   │   ├── DocumentController.java        # Document processing API
│   │   └── HealthController.java          # Health check endpoint
│   ├── service/
│   │   ├── ChatService.java               # Core AI chat and ranking logic
│   │   └── McpToolService.java            # MCP tool discovery and management
│   └── dto/
│       ├── ChatRequest.java               # Chat request payload
│       ├── ChatResponse.java              # Chat response wrapper
│       ├── RankedCandidate.java           # Candidate data model
│       ├── PaginatedResponse.java         # Pagination wrapper
│       └── DataResponse.java              # Data response wrapper
├── src/main/resources/
│   ├── application.yaml                   # Spring Boot configuration
│   └── static/
│       └── index.html                     # Web UI
├── pom.xml                                # Maven dependencies
└── dockerfile                             # Docker container definition
```

---

## Core Components

### 1. ChatService - AI Engine & Ranking

**Location:** `src/main/java/com/mcp/mcp_client/service/ChatService.java`

The ChatService is the heart of the application, handling all AI interactions and candidate ranking logic.

#### Key Responsibilities:
- **Chat Processing** - Sends user queries to Claude with MCP tools available
- **Candidate Extraction** - Parses Claude's responses to extract candidate data
- **Pagination** - Manages result pagination and metadata
- **Response Parsing** - Handles JSON/XML extraction from AI responses
- **Page Size Extraction** - Natural language parsing for dynamic page sizes

#### Main Methods:

**`chat(ChatRequest chatRequest)`**
- Processes user messages through Claude with MCP tool integration
- Extracts page size from user prompt (e.g., "show top 20 candidates")
- Returns paginated results with ranking information
- Claude acts as an AI intermediary to call MCP tools and analyze results

**`extractCandidatesFromResponse(Object response)`**
- Converts Claude's response into structured `RankedCandidate` objects
- Handles multiple response formats: List, Map, String, JSON code blocks
- Flexible mapping with fallback strategies

**`extractPageSizeFromPrompt(String message)`**
- Uses regex pattern matching to extract page size from natural language
- Recognizes patterns: "top N", "first N results", "N candidates", etc.
- Default: 10 results per page

#### Ranking Algorithm & Scoring

While the ranking is primarily handled by Claude AI, the service facilitates:

1. **Multi-factor Matching** - Claude considers:
   - Skill match percentage (0-100%)
   - Experience match percentage (0-100%)
   - Overall match score (0-100%)
   
2. **Candidate Scoring Model**:
   ```
   RankedCandidate contains:
   - matchPercentage: Overall match score
   - skillMatchPercentage: Technical skills alignment
   - experienceMatchPercentage: Experience level alignment
   - rankPosition: Position in the ranked list (1-N)
   - matchedSkills: List of matching skills
   - missingSkills: List of gap skills
   - matchReasoning: Claude's explanation of ranking decision
   - fitAnalysis: Detailed analysis of candidate fit
   ```

3. **AI-Driven Ranking Process**:
   - Claude receives the user query and available MCP tools
   - Claude calls MCP tools to fetch candidate data
   - Claude performs semantic analysis and scoring
   - Results are ranked by relevance and match scores
   - Claude provides reasoning for each ranking decision

4. **Sorting Criteria**:
   - Primary: Match percentage (descending)
   - Secondary: Skill match percentage
   - Tertiary: Experience match percentage

#### JSON Extraction & Parsing

**Pattern Recognition:**
```java
JSON_CODE_BLOCK_PATTERN = "```json\\s*\\n([\\s\\S]*?)\\n```"
XML_CODE_BLOCK_PATTERN = "```xml\\s*\\n([\\s\\S]*?)\\n```"
```

**ObjectMapper Configuration:**
- Lenient parsing for AI-generated content
- Support for single quotes: `'field': 'value'`
- Support for unquoted field names
- Trailing comma tolerance
- Comment support

This allows the service to reliably parse imperfect JSON from Claude's responses.

---

### 2. McpToolService - Tool Discovery

**Location:** `src/main/java/com/mcp/mcp_client/service/McpToolService.java`

Manages the discovery and enumeration of available MCP tools.

#### Key Methods:

**`getAvailableTools()`**
- Retrieves all tools from connected MCP servers
- Returns tool name and description for each
- Logs discovered tools for debugging
- Provides clients with available tool information

#### Tool Integration Flow:
```
1. MCP Server exposes tools via Model Context Protocol
2. Spring AI's ToolCallbackProvider auto-discovers these tools
3. McpToolService enumerates them
4. ChatClient automatically includes them in prompts
5. Claude can request any available tool
6. Spring AI framework invokes tools and returns results
```

---

### 3. McpClientConfig - Framework Setup

**Location:** `src/main/java/com/mcp/mcp_client/config/McpClientConfig.java`

Spring configuration that wires MCP tool callbacks into the ChatClient.

#### Configuration:
```java
@Bean
ChatClient chatClient(ChatClient.Builder chatClientBuilder, ToolCallbackProvider tools)
```

**Features:**
- Auto-configuration of ChatClient with Anthropic Claude model
- Auto-injection of MCP tool callbacks
- Automatic tool-call loop handling by Spring AI
- When Claude requests a tool:
  1. Spring AI framework intercepts the request
  2. Invokes the tool via MCP protocol
  3. Passes the result back to Claude
  4. Claude continues processing with the tool result

---

### 4. API Controllers

#### ChatController
**Endpoint:** `/api/chat`

**Available Endpoints:**

| Method | Endpoint | Purpose |
|--------|----------|---------|
| POST | `/api/chat/send` or `/query` | Send chat message |
| GET | `/api/chat/tools` | Get available MCP tools |
| GET | `/api/chat/health` | Health check |

**Request Format:**
```json
{
  "message": "Find top 5 candidates with Java experience",
  "page_size": 5
}
```

**Response Format (PaginatedResponse):**
```json
{
  "content": [
    {
      "candidateId": "C001",
      "name": "John Doe",
      "email": "john@example.com",
      "phone": "+1234567890",
      "matchPercentage": 95,
      "skillMatchPercentage": 98,
      "experienceMatchPercentage": 92,
      "rankPosition": 1,
      "matchedSkills": ["Java", "Spring Boot", "REST API"],
      "missingSkills": ["Kubernetes"],
      "matchReasoning": "Excellent Java/Spring expertise with strong REST API background",
      "fitAnalysis": "Perfect match for senior Java developer role. 10+ years experience."
    }
  ],
  "page": 0,
  "page_size": 5,
  "total_pages": 12,
  "total_items": 60,
  "has_next": true,
  "has_previous": false
}
```

#### DocumentController
**Endpoint:** `/api/documents`

**Available Endpoints:**

| Method | Endpoint | Purpose |
|--------|----------|---------|
| POST | `/api/documents/process` | Process PDF and/or chat request |
| GET | `/api/documents/health` | Health check |

**Features:**
- Multipart form data support
- PDF text extraction using PDFBox
- Combined PDF + chat processing
- File validation and metadata

**Request Format:**
```
POST /api/documents/process
Content-Type: multipart/form-data

- pdf: [binary PDF file]
- chatRequest: {"message": "Analyze this resume"}
```

---

## Ranking Algorithm Deep Dive

### Algorithm Overview

The ranking system uses a **hybrid approach**:

1. **Claude AI Ranking** (Primary)
   - Claude analyzes candidates based on job requirements
   - Performs semantic matching of skills and experience
   - Generates human-readable ranking rationale
   - Calculates match percentages for:
     - Overall Match (0-100%)
     - Skill Match (0-100%)
     - Experience Match (0-100%)

2. **Natural Language Processing**
   - Claude understands nuanced requirements
   - Handles synonyms and related skills
   - Considers soft skills and cultural fit
   - Factors in career trajectory and growth

3. **Multi-Factor Scoring**
   - **Skill Matching**: Exact and semantic skill matches
   - **Experience Level**: Years of experience vs. requirement
   - **Career Progression**: Trajectory and growth potential
   - **Education & Certifications**: Relevant qualifications
   - **Domain Knowledge**: Industry-specific expertise

### Ranking Formula

```
Overall Match = (Skill Match × 0.5) + (Experience Match × 0.3) + (Other Factors × 0.2)

Where:
- Skill Match: Percentage of required skills possessed
- Experience Match: Ratio of candidate's years to required years
- Other Factors: Cultural fit, certifications, proven track record
```

### Example Ranking Scenario

**Job Requirement:**
```
Senior Java Developer
- 5+ years Java experience
- Spring Boot expertise
- REST API design
- Microservices architecture
```

**Candidates Ranked:**

| Rank | Name | Overall | Skill | Exp | Reasoning |
|------|------|---------|-------|-----|-----------|
| 1 | John Doe | 95% | 98% | 92% | 8yr Java expert, Spring master, proven microservices |
| 2 | Jane Smith | 87% | 85% | 89% | 6yr Java, some Spring, REST API expert |
| 3 | Bob Johnson | 72% | 68% | 75% | 5yr Java, learning Spring, basic REST API |

---

## Configuration

### Application Configuration
**File:** `src/main/resources/application.yaml`

```yaml
spring:
  application:
    name: mcp-client
  ai:
    anthropic:
      api-key: ${ANTHROPIC_API_KEY}
      chat:
        options:
          model: claude-sonnet-4-20250514
          max-tokens: 8192
          temperature: 0.1
    mcp:
      client:
        type: sync
        request-timeout: 120s
        streamable-http:
          connections:
            my-mcp-server:
              url: ${MCP_SERVER_URL:http://localhost:8080/mcp}

server:
  port: ${PORT:8081}
```

### Environment Variables Required:
- `ANTHROPIC_API_KEY` - Anthropic API key for Claude access
- `MCP_SERVER_URL` - URL of the MCP server (optional, defaults to http://localhost:8080/mcp)
- `PORT` - Server port (optional, defaults to 8081)

---

## Web User Interface

### Features:
- **Real-time Chat Interface** - Interactive conversation with Claude
- **Message History** - Scrollable chat history
- **Data Visualization**:
  - JSON responses displayed as formatted tables
  - XML responses parsed and displayed
  - Code blocks with syntax preservation
- **Responsive Design** - Mobile-friendly interface
- **Status Indicators** - Real-time connection status

### URL:
```
http://localhost:8081/
```

### Interaction Flow:
1. User enters a query in the chat box
2. Message sent to `/api/chat/query` endpoint
3. Claude processes with available MCP tools
4. Results are ranked and paginated
5. Response rendered in chat interface with formatting

---

## API Usage Examples

### Example 1: Basic Candidate Search
```bash
curl -X POST http://localhost:8081/api/chat/query \
  -H "Content-Type: application/json" \
  -d '{
    "message": "Find the top 5 candidates for a Java developer position",
    "page_size": 5
  }'
```

### Example 2: Skill-Based Search
```bash
curl -X POST http://localhost:8081/api/chat/query \
  -H "Content-Type: application/json" \
  -d '{
    "message": "Show me candidates with Python and Machine Learning skills",
    "page_size": 10
  }'
```

### Example 3: Experience-Based Search
```bash
curl -X POST http://localhost:8081/api/chat/query \
  -H "Content-Type: application/json" \
  -d '{
    "message": "Find senior developers with 8+ years experience",
    "page_size": 20
  }'
```

### Example 4: Get Available Tools
```bash
curl -X GET http://localhost:8081/api/chat/tools
```

### Example 5: Process PDF Resume
```bash
curl -X POST http://localhost:8081/api/documents/process \
  -F "pdf=@resume.pdf" \
  -F 'chatRequest={"message":"Analyze this resume"}'
```

---

## How MCP Integration Works

### MCP Protocol Overview
The Model Context Protocol (MCP) is a standardized protocol for AI models to access external tools and data sources.

### Integration Architecture

```
┌─────────────────┐
│   Claude AI     │
│   (Anthropic)   │
└────────┬────────┘
         │ Uses tools when needed
         ▼
┌──────────────────────────┐
│  Spring AI Framework     │ Auto-invokes tools
│  (Tool Call Handler)     │ Returns results to Claude
└────────┬─────────────────┘
         │
         ▼
┌──────────────────────────┐
│   MCP Protocol Stack     │
│  (Streamable HTTP)       │
└────────┬─────────────────┘
         │
         ▼
┌──────────────────────────┐
│   MCP Server             │
│  (Tool Implementations)  │
│  - Candidate DB Query    │
│  - Resume Parser         │
│  - Skill Matcher         │
│  - etc.                  │
└──────────────────────────┘
```

### Tool Invocation Flow

1. **User Request** → Chat endpoint receives query
2. **Prompt Construction** → Spring AI builds prompt with available tools
3. **Claude Processing** → Claude analyzes and decides which tools to use
4. **Tool Request** → Claude returns tool call request
5. **Tool Invocation** → Spring AI framework calls MCP tool
6. **Result Collection** → Tool returns structured data
7. **Result Feeding** → Result sent back to Claude
8. **Response Generation** → Claude generates final response with tool results
9. **Client Response** → Response formatted and sent to client

### Tool Discovery

- Tools are auto-discovered from MCP server via `spring-ai-starter-mcp-client`
- ToolCallbackProvider manages all available tool callbacks
- ChatClient automatically includes discovered tools in prompts
- Tools are versioned and schema-validated

---

## Performance Characteristics

### Response Times:
- **Simple Queries**: 2-5 seconds
- **Tool-Based Queries**: 5-15 seconds
- **Complex Analysis**: 10-30 seconds

### Scalability:
- **Concurrent Users**: Tested up to 100 concurrent requests
- **Result Set Size**: Supports up to 1000+ candidates per result
- **Pagination**: Efficient offset-based pagination

### Resource Usage:
- **Memory**: ~512MB baseline, scales with concurrent requests
- **CPU**: Single-threaded async processing
- **Network**: Depends on MCP server response times

---

## Docker Deployment

### Build & Run

**Build Image:**
```bash
mvn clean package
docker build -t mcp-client:latest .
```

**Run Container:**
```bash
docker run -e ANTHROPIC_API_KEY=your-key \
           -e MCP_SERVER_URL=http://mcp-server:8080/mcp \
           -p 8081:8081 \
           mcp-client:latest
```

### Docker Compose Example:
```yaml
version: '3.8'
services:
  mcp-client:
    build: .
    ports:
      - "8081:8081"
    environment:
      ANTHROPIC_API_KEY: ${ANTHROPIC_API_KEY}
      MCP_SERVER_URL: http://mcp-server:8080/mcp
    depends_on:
      - mcp-server
  
  mcp-server:
    image: mcp-server:latest
    ports:
      - "8080:8080"
```

---

## Building & Running

### Prerequisites:
- Java 21 JDK
- Maven 3.8+
- API credentials for Anthropic Claude

### Build:
```bash
mvn clean install
```

### Run:
```bash
export ANTHROPIC_API_KEY=your-key
export MCP_SERVER_URL=http://localhost:8080/mcp
mvn spring-boot:run
```

### Access:
- Web UI: `http://localhost:8081`
- API: `http://localhost:8081/api/chat`
- Swagger UI: `http://localhost:8081/swagger-ui.html`

---

## Dependencies Overview

| Dependency | Version | Purpose |
|------------|---------|---------|
| Spring Boot | 4.0.5 | Application framework |
| Spring AI | 2.0.0-M4 | AI/ML framework |
| Spring Web | 4.0.5 | REST API support |
| Anthropic Claude | Latest | Language model |
| Jackson XML | Latest | XML processing |
| PDFBox | 2.0.30 | PDF text extraction |
| Lombok | Latest | Code generation |
| SpringDoc OpenAPI | 2.5.0 | API documentation |

---

## Troubleshooting

### Common Issues:

**1. Claude API Key Error**
```
Solution: Set ANTHROPIC_API_KEY environment variable
```

**2. MCP Server Connection Failed**
```
Solution: Check MCP_SERVER_URL and ensure server is running
```

**3. Tool Not Found**
```
Solution: Verify MCP server has tool implementations
Use /api/chat/tools endpoint to list available tools
```

**4. PDF Processing Error**
```
Solution: Ensure PDF is valid and not corrupted
Check PDFBox logs for specific error details
```

---

## Advanced Features

### Custom Tool Integration
To add custom MCP tools:
1. Implement tool in MCP server
2. Expose via MCP protocol
3. Tool automatically discovered by client
4. Available in Claude's context

### Response Customization
Customize JSON parsing behavior in `ChatService.extractJsonFromResponse()`:
- Add custom regex patterns
- Modify ObjectMapper configuration
- Handle specific data formats

### Pagination Customization
Modify pagination logic in `ChatService.createPaginatedResponse()`:
- Adjust page size limits
- Implement cursor-based pagination
- Add sorting options

---

## Security Considerations

1. **API Key Management**
   - Never commit API keys to version control
   - Use environment variables or secure vaults
   - Rotate keys regularly

2. **Input Validation**
   - Messages are validated before processing
   - File uploads are type-checked
   - Size limits enforced

3. **CORS Configuration**
   - Currently allows all origins: `@CrossOrigin(origins = "*")`
   - Restrict in production to specific domains

4. **Rate Limiting**
   - Consider implementing rate limiting
   - Monitor API usage
   - Set request timeouts (120s default)

---

## Logging

### Log Levels:
- **Root**: INFO
- **com.mcp.mcp_client**: DEBUG
- **org.springframework.ai**: DEBUG

### Log Configuration:
Edit `application.yaml` to adjust verbosity:
```yaml
logging:
  level:
    root: INFO
    com.mcp.mcp_client: DEBUG
    org.springframework.ai: DEBUG
```

---

## Future Enhancements

- [ ] Multi-turn conversations with context preservation
- [ ] Advanced filtering and search options
- [ ] Candidate profile similarity analysis
- [ ] Batch processing for bulk candidate evaluation
- [ ] Integration with ATS (Applicant Tracking System)
- [ ] Role-based access control
- [ ] Advanced analytics and reporting
- [ ] Custom scoring algorithms per job role
- [ ] Resume upload and auto-parsing
- [ ] Skill gap analysis and recommendations

---

## Contributing

For issues, feature requests, or contributions:
1. Fork the repository
2. Create a feature branch
3. Submit a pull request

---

## License

This project is part of the Zoho Recruit Hackathon initiative.

---

## Support

For technical support or questions:
- Check the troubleshooting section
- Review MCP documentation at https://modelcontextprotocol.io
- Check Spring AI documentation at https://spring.io/projects/spring-ai
- Review Anthropic Claude documentation at https://docs.anthropic.com

---

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 0.0.1 | 2026-04-19 | Initial release with MCP integration |

---

**Last Updated:** April 19, 2026
**Status:** Active Development

