# System Architecture Document

**Project:** Wissen AI ATS
**Team Titan Transformers:** Sudarshan Garg, Saurabh Kumar, Rupam Swain, Suryaprakash Rao

## Architecture Overview

Wissen AI ATS follows a **three-tier, service-oriented architecture** with clear separation of concerns and leverages the Model Context Protocol (MCP) for AI tool orchestration.

```
┌─────────────────────────────────────────────────────────────┐
│                      Frontend Layer                          │
│                   zoho-recruit-ui (React)                   │
│         (Port 5173 dev / served via client in prod)         │
└────────────────────────┬────────────────────────────────────┘
                         │ HTTP/WebSocket
                         │ JSON REST API
                         ▼
┌─────────────────────────────────────────────────────────────┐
│                   Application Layer                          │
│               mcp-client-demo (Spring Boot)                 │
│                      (Port 8081)                            │
│  ┌─────────────────────────────────────────────────────┐   │
│  │      REST Controllers & API Gateway                │   │
│  │  - /api/job-descriptions/*                         │   │
│  │  - /api/candidates/*                               │   │
│  │  - /api/interviews/*                               │   │
│  └──────────────────┬──────────────────────────────────┘   │
│                    │ MCP Client                             │
│  ┌────────────────▼──────────────────────────────────┐    │
│  │      Claude Sonnet via MCP                        │    │
│  │  - Tool orchestration                             │    │
│  │  - Prompt composition                             │    │
│  │  - Result formatting                              │    │
│  └──────────────────┬──────────────────────────────────┘   │
└─────────────────────┼──────────────────────────────────────┘
                      │ MCP Protocol (HTTP)
                      │ JSON-RPC 2.0
                      ▼
┌─────────────────────────────────────────────────────────────┐
│                   Service Layer                             │
│               mcp-server-demo (Spring Boot)                │
│                      (Port 8080)                           │
│  ┌─────────────────────────────────────────────────────┐  │
│  │           MCP Server & Tool Definitions             │  │
│  │  @Tool parseJobDescription                          │  │
│  │  @Tool searchCandidatesInZohoRecruit                │  │
│  │  @Tool findAndRankCandidatesForJD                   │  │
│  │  @Tool getDetailedCandidateFitAnalysis              │  │
│  │  @Tool generateCustomInterviewQuestions             │  │
│  └────────┬──────────────────────────────────────────┘  │
│           │                                              │
│  ┌────────▼──────────────────────────────────────────┐  │
│  │         Service Layer                             │  │
│  │  - AIEnhancedJobDescriptionService                │  │
│  │  - AIEnhancedCandidateRankingService              │  │
│  │  - ZohoRecruitService                             │  │
│  │  - ZohoCriteriaBuilder                            │  │
│  │  - ZohoRecruitOAuthService                        │  │
│  └────────┬──────────────────────────────────────────┘  │
└───────────┼──────────────────────────────────────────────┘
            │
            ├──────────────────┬──────────────────┐
            │                  │                  │
            ▼                  ▼                  ▼
    ┌──────────────┐  ┌──────────────┐  ┌──────────────┐
    │ Zoho Recruit │  │  Claude API  │  │  Database    │
    │   API        │  │  (Haiku)     │  │  (Internal)  │
    │  (OAuth)     │  │              │  │              │
    └──────────────┘  └──────────────┘  └──────────────┘
```

## Component Details

### 1. Frontend Layer (zoho-recruit-ui)

**Technology**: React 19 + TypeScript + Material-UI + Redux Toolkit

**Responsibilities**:
- User interface for recruitment workflows
- Real-time state management via Redux
- API communication with MCP Client
- Responsive, accessible UI (WCAG 2.1 AA)

**Key Components**:
- `JobListPage`: Display and manage job postings
- `CandidateSearch`: Search and filter candidates
- `CandidateRanking`: View ranked candidates with scores
- `FitAnalysis`: Detailed candidate-to-job fit details
- `InterviewQuestions`: Generated questions and feedback

**State Management**:
```typescript
store/
  ├── jobsSlice        // Job posting state
  ├── candidatesSlice  // Candidate data and rankings
  ├── analysisSlice    // AI analysis results
  └── uiSlice          // UI state (modals, notifications)
```

**API Client**:
```typescript
services/
  └── apiClient.ts     // Axios instance with base config
```

**Styling**:
- Material-UI theme customization
- CSS-in-JS with MUI System
- Responsive breakpoints

---

### 2. Application Layer (mcp-client-demo)

**Technology**: Spring Boot 4.0.5 + Spring AI + Java 21

**Responsibilities**:
- REST API gateway for frontend
- MCP client for server communication
- Claude Sonnet orchestration
- Request/response transformation
- Error handling and logging

**Architecture Pattern**: MVC with Service Layer

**Key Components**:

#### REST Controllers
```java
@RestController
@RequestMapping("/api")
├── JobDescriptionController
│   ├── POST /job-descriptions/parse
│   └── GET /job-descriptions/{id}
├── CandidateController
│   ├── POST /candidates/search
│   ├── POST /candidates/rank
│   ├── POST /candidates/{id}/fit-analysis
│   └── GET /candidates/{id}
└── InterviewController
    └── POST /interviews/generate-questions
```

#### MCP Client Configuration
```yaml
mcp:
  server-url: http://localhost:8080
  timeout: 30s
  
claude:
  model: claude-sonnet-4-20250514
  api-key: ${ANTHROPIC_API_KEY}
```

#### Service Classes
- `JobDescriptionService`: Orchestrates JD parsing via MCP
- `CandidateService`: Manages search and ranking logic
- `InterviewService`: Question generation coordination
- `MobileClientService`: MCP client communication

**Data Flow** (Example: Candidate Ranking)
```
POST /api/candidates/rank
  │
  ├─▶ CandidateController.rankCandidates()
  │     │
  │     ├─▶ CandidateService.rankCandidates()
  │     │     │
  │     │     ├─▶ MCP Client.callTool("findAndRankCandidatesForJD")
  │     │     │     │
  │     │     │     ├─▶ [MCP Server] AIEnhancedCandidateRankingService
  │     │     │     │
  │     │     │     └─▶ Return { candidates: [{id, score, explanation}] }
  │     │     │
  │     │     └─▶ Transform & return to client
  │     │
  │     └─▶ Return ResponseEntity with results
  │
  └─▶ Client receives JSON response
```

---

### 3. Service Layer (mcp-server-demo)

**Technology**: Spring Boot 4.0.5 + Spring AI + Java 21

**Responsibilities**:
- MCP tool definitions and implementation
- Business logic for AI-enhanced recruitment
- Zoho Recruit integration
- Database operations (if any)
- Health checks and monitoring

**Architecture Pattern**: Service-oriented with MCP tools

#### MCP Tools (Spring AI @Tool)

```java
@Component
public class RecruitmentTools {
  
  @Tool("Parse JD and extract requirements")
  public Map<String, Object> parseJobDescription(String jobDescription) { }
  
  @Tool("Search Zoho Recruit for matching candidates")
  public Map<String, Object> searchCandidatesInZohoRecruit(
    Map<String, Object> searchCriteria) { }
  
  @Tool("Find and rank candidates for JD")
  public Map<String, Object> findAndRankCandidatesForJD(
    String jobDescription) { }
  
  @Tool("Get detailed candidate fit analysis")
  public Map<String, Object> getDetailedCandidateFitAnalysis(
    String candidateInfo, String jobRequirements) { }
  
  @Tool("Generate interview questions")
  public Map<String, Object> generateCustomInterviewQuestions(
    String jobDescription, String candidateProfile) { }
}
```

#### Service Classes

**AIEnhancedJobDescriptionService**
- Parses JD using Claude Haiku
- Extracts: skills, experience, education, nice-to-haves
- Returns structured JSON
- Fallback: Regex-based parsing if AI fails

**AIEnhancedCandidateRankingService**
- Scores candidates using Claude Haiku
- Evaluates fit: skills match, experience level, education
- Returns ranking with explanation
- Fallback: Simple keyword matching if AI unavailable

**ZohoRecruitService**
- OAuth authentication management
- Candidate data retrieval
- Job posting retrieval
- Custom field access
- API error handling with retry logic

**ZohoCriteriaBuilder**
- Converts JD requirements to Zoho search criteria
- Validates criteria format
- Maps requirement fields to Zoho field names
- Handles custom field mapping

**ZohoRecruitOAuthService**
- OAuth 2.0 token management
- Token refresh logic
- Credential storage (environment variables)

#### Configuration
```yaml
zoho:
  recruit:
    client-id: ${ZOHO_RECRUIT_CLIENT_ID}
    client-secret: ${ZOHO_RECRUIT_CLIENT_SECRET}
    refresh-token: ${ZOHO_RECRUIT_REFRESH_TOKEN}
    org-id: ${ZOHO_RECRUIT_ORG_ID}

anthropic:
  api-key: ${ANTHROPIC_API_KEY}
  model: claude-haiku-4-5-20251001

mcp:
  server:
    port: 8080
```

---

## Data Flow Diagrams

### Flow 1: Complete Candidate Screening Pipeline

```
User uploads Job Description
  │
  └─▶ POST /api/candidates/search
      │
      ├─▶ Client: parseJobDescription(JD)
      │   └─▶ Server: Claude Haiku parses JD
      │       └─▶ Returns: { skills, experience, education }
      │
      ├─▶ Client: searchCandidatesInZohoRecruit(criteria)
      │   └─▶ Server: ZohoRecruitService searches Zoho
      │       └─▶ Returns: List of candidates
      │
      └─▶ Client: findAndRankCandidatesForJD(JD + candidates)
          └─▶ Server: Claude Haiku ranks candidates
              └─▶ Returns: Ranked list with scores

Frontend displays ranked candidates
  │
  └─▶ User clicks on candidate
      │
      └─▶ POST /api/candidates/{id}/fit-analysis
          │
          └─▶ Client: getDetailedCandidateFitAnalysis()
              │
              └─▶ Server: Claude generates fit analysis
                  └─▶ Returns: { score, strengths, gaps, recommendations }

Frontend displays detailed analysis
```

### Flow 2: Interview Question Generation

```
User selects candidate and job
  │
  └─▶ POST /api/interviews/generate-questions
      │
      └─▶ Client: Prepare job + candidate context
          │
          └─▶ Server: Claude Sonnet generates questions
              │
              └─▶ Returns: [{ question, category, evaluation_criteria }]

Frontend displays questions with editing capability
```

---

## Integration Points

### Zoho Recruit API

**Base URL**: `https://recruit.zoho.{region}/api/v2/`

**Authentication**: OAuth 2.0 Bearer Token

**Key Endpoints**:
```
GET  /Candidates          // List candidates
GET  /Candidates/{id}     // Get candidate details
POST /Candidates/search   // Search with criteria
GET  /Jobs                // List job postings
GET  /Jobs/{id}           // Get job details
POST /Activity            // Log activity
```

**Rate Limiting**: 600 requests/minute

**Error Handling**:
- Retry with exponential backoff (3 attempts)
- Circuit breaker pattern for failures
- Fallback to cached data if available

### Anthropic Claude API

**Model Selection**:
- **Claude Haiku** (throughout): Cost-optimized, efficient processing for all AI tasks
  - Job description parsing
  - Candidate ranking and scoring
  - Interview question generation
  - Fit analysis

**Key Features Used**:
- Prompt caching for repeated requests
- Structured output parsing
- Temperature: 0.7 (balanced creativity/consistency)

**Rate Limits**: 
- Requests per minute vary by plan
- Implement queue-based processing for bulk operations

---

## Database Schema (Optional Internal State)

```sql
-- If persisting results locally
CREATE TABLE candidates (
  id STRING PRIMARY KEY,
  zoho_id STRING UNIQUE,
  name STRING,
  email STRING,
  phone STRING,
  profile_summary TEXT,
  created_at TIMESTAMP
);

CREATE TABLE job_descriptions (
  id STRING PRIMARY KEY,
  title STRING,
  company STRING,
  parsed_requirements JSON,
  created_at TIMESTAMP
);

CREATE TABLE rankings (
  id STRING PRIMARY KEY,
  job_id STRING,
  candidate_id STRING,
  score DECIMAL,
  explanation TEXT,
  created_at TIMESTAMP,
  FOREIGN KEY (job_id) REFERENCES job_descriptions(id),
  FOREIGN KEY (candidate_id) REFERENCES candidates(id)
);
```

---

## Scalability Considerations

### Horizontal Scaling
- **Stateless Design**: Client and Server are horizontally scalable
- **Load Balancer**: Deploy multiple client/server instances behind load balancer
- **API Gateway**: Use Nginx/HAProxy for request distribution

### Performance Optimization
- **Prompt Caching**: Cache common JD parsing prompts
- **Pagination**: Limit search results (max 100 per page)
- **Async Processing**: Queue bulk ranking jobs
- **Database Indexing**: Index frequently searched fields in Zoho

### Monitoring & Observability
```
Metrics to track:
- API response times
- Tool execution times
- Claude API usage/costs
- Zoho API rate limits
- Error rates and types
- Cache hit rates
```

---

## Security Architecture

### Authentication & Authorization
- **Frontend**: Session-based (cookies with HttpOnly flag)
- **Backend APIs**: OAuth 2.0 with Spring Security
- **MCP Communication**: Localhost only (can extend with mTLS)

### Data Protection
- **API Keys**: Environment variables, never in code
- **Credentials**: Encrypted storage using Spring Cloud Config
- **HTTPS**: Enforced for all production traffic
- **CORS**: Configured for frontend origin only

### Input Validation
```
JD Parsing:
  - Max length: 50,000 characters
  - Content-type: text/plain, application/pdf
  - SQL injection prevention: Parameterized queries

Search Criteria:
  - Whitelist allowed fields
  - Validate field operators (=, contains, in, range)
  - Rate limit: 10 searches per minute per user

API Requests:
  - Request size limit: 1MB
  - Request timeout: 30 seconds
  - XSS protection: Output encoding
```

### Audit & Logging
```
Log all:
- User actions (search, ranking, questions)
- API calls to Zoho and Claude
- Authentication events
- Errors and exceptions
- Data access patterns

Retention: 90 days
```

---

## Deployment Architecture

### Development Environment
```
Local Docker Compose:
  - mcp-server-demo (port 8080)
  - mcp-client-demo (port 8081)
  - zoho-recruit-ui (port 5173)
  - Optional: Local PostgreSQL
```

### Production Environment
```
Cloud Deployment (AWS/GCP/Azure):
  - Spring Boot apps: Container (Docker)
  - Frontend: Static hosting (S3 + CloudFront)
  - Database: Managed RDS
  - Secrets: Cloud vault (Secrets Manager)
  - Monitoring: CloudWatch / Datadog
  - CI/CD: GitHub Actions
```

### Environment Variables Required
```bash
# Zoho Recruit
ZOHO_RECRUIT_CLIENT_ID=...
ZOHO_RECRUIT_CLIENT_SECRET=...
ZOHO_RECRUIT_REFRESH_TOKEN=...
ZOHO_RECRUIT_ORG_ID=...

# Anthropic
ANTHROPIC_API_KEY=...
ANTHROPIC_API_BASE_URL=https://api.anthropic.com/v1

# Server Config
MCP_SERVER_PORT=8080
MCP_CLIENT_PORT=8081
REACT_APP_API_URL=http://localhost:8081
```

---

## Technology Stack Summary

| Layer | Component | Technology | Version |
|-------|-----------|-----------|---------|
| Frontend | UI Framework | React | 19.2.4 |
| Frontend | Language | TypeScript | 6.0.2 |
| Frontend | UI Library | Material-UI | Latest |
| Frontend | State Mgmt | Redux Toolkit | Latest |
| Frontend | Build Tool | Vite | Latest |
| Backend | Language | Java | 21 |
| Backend | Framework | Spring Boot | 4.0.5 |
| Backend | Protocol | Spring AI (MCP) | 2.0.0-M4 |
| External | Recruitment | Zoho Recruit | Latest API |
| External | AI | Anthropic Claude | Haiku/Sonnet |

---

## Multi-Site MCP Server Support

The architecture is designed to support multiple MCP servers seamlessly:

```
Frontend Request
    ↓
MCP Client (Load Balancer)
    ├─→ Route to Zoho Recruit MCP Server (Port 8080)
    ├─→ Route to LinkedIn MCP Server (Port 8082)
    ├─→ Route to Indeed MCP Server (Port 8083)
    └─→ Aggregate and rank results
    ↓
Return unified candidate list
```

**Scalability Benefits**:
- Add new job site integrations without modifying existing code
- Each MCP server runs independently with Claude Haiku
- Client-side routing and result aggregation
- Multi-source candidate discovery

## Future Enhancement Areas

1. **Additional Job Sites**: LinkedIn, Indeed, Glassdoor MCP servers
2. **Advanced Analytics**: Hiring funnel, time-to-hire metrics
3. **Custom ML Models**: Train models on company hiring data
4. **Interview Scheduling**: Automated calendar integration
5. **Offer Management**: Offer generation and e-signature
6. **Mobile App**: Native iOS/Android applications
7. **Multi-tenant SaaS**: Support multiple organizations
