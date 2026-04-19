# Product Requirements Document (PRD)

## Executive Summary

Zoho Recruit AI is an AI-powered recruitment intelligence platform that enhances the Zoho Recruit ecosystem with intelligent candidate screening, ranking, and analysis capabilities. It reduces manual screening time by up to 70% while improving candidate quality assessments.

## Problem Statement

### Current Challenges
- **Manual Screening**: Recruiters spend excessive time manually reviewing candidate qualifications against job requirements
- **Inconsistent Evaluation**: Subjective assessment leads to inconsistent candidate evaluation
- **Time-to-Hire**: Extended screening process delays recruitment cycles
- **Candidate Data Integration**: Difficulty matching Zoho candidate data with job requirements

### Solution
An intelligent platform that:
1. Automatically analyzes job descriptions and extracts structured requirements
2. Intelligently searches Zoho Recruit database based on JD requirements
3. Ranks candidates using AI-powered fit analysis
4. Generates role-specific interview questions
5. Provides detailed fit analysis with actionable insights

## Target Users

### Primary Users
- **Recruiters**: Screen and rank candidates efficiently
- **Hiring Managers**: Review candidate recommendations and fit analysis
- **HR Teams**: Manage recruitment workflows and track candidates

### Use Cases
1. **Job Posting to Screening**: Upload JD, get ranked candidate list in minutes
2. **Candidate Fit Analysis**: Understand why a candidate is suitable for a role
3. **Interview Preparation**: Generate tailored interview questions
4. **Bulk Candidate Screening**: Process multiple candidates simultaneously

## Features & Requirements

### Core Features

#### 1. Job Description Analysis
**Requirement**: Automatically parse and extract structured requirements from job descriptions

**Acceptance Criteria**:
- Extract key requirements (skills, experience, education)
- Identify nice-to-have vs must-have qualifications
- Return structured JSON with clear requirement categories
- Support multiple document formats (text, PDF)
- Handle variations in JD format

**API Endpoint**: `POST /api/job-descriptions/parse`

---

#### 2. Candidate Search
**Requirement**: Generate Zoho search criteria from job requirements and find matching candidates

**Acceptance Criteria**:
- Convert extracted JD requirements to Zoho search criteria
- Search Zoho Recruit database for matching candidates
- Return paginated candidate results
- Support filters and sorting by match score
- Handle API rate limiting gracefully

**API Endpoint**: `POST /api/candidates/search`

---

#### 3. Candidate Ranking
**Requirement**: Score and rank candidates based on JD alignment

**Acceptance Criteria**:
- Score candidates on 0-100 scale
- Provide ranking explanation for transparency
- Identify strengths and gaps for each candidate
- Support bulk ranking of multiple candidates
- Fallback to regex-based ranking if AI unavailable

**API Endpoint**: `POST /api/candidates/rank`

---

#### 4. Fit Analysis
**Requirement**: Provide detailed candidate-to-job fit assessment

**Acceptance Criteria**:
- Generate comprehensive fit score (0-100)
- Identify top 3 strengths
- Identify top 3 gaps/risks
- Provide actionable recommendations
- Include experience level matching

**API Endpoint**: `POST /api/candidates/{id}/fit-analysis`

---

#### 5. Interview Question Generation
**Requirement**: Generate role-specific interview questions

**Acceptance Criteria**:
- Generate 5-10 behavioral and technical questions
- Tailor questions to job requirements and candidate experience
- Categorize questions (behavioral, technical, scenario-based)
- Provide suggested evaluation criteria for each question

**API Endpoint**: `POST /api/interviews/generate-questions`

---

#### 6. Real-time Integration with Multiple Job Sources
**Requirement**: Seamless synchronization with Zoho Recruit and support for additional job site integrations via MCP servers

**Acceptance Criteria**:
- OAuth-based authentication with primary job source (Zoho)
- Support for pluggable MCP servers for additional job sites
- Automatic token refresh and credential management
- Real-time candidate data access from multiple sources
- Support for custom fields and normalization across sources
- Error handling and retry logic per source
- Result aggregation and deduplication

---

### Frontend Features

#### Job Listing Page
- Display all job postings
- Search and filter functionality
- Create/edit job descriptions

#### Candidate Management
- View candidate list for a job
- See candidate rankings and fit scores
- View detailed candidate profiles
- Update candidate status

#### Analysis Dashboard
- Job performance metrics
- Top candidates for each role
- Hire success predictions
- Recruiting funnel analytics

#### Interview Questions
- View generated interview questions
- Customize questions before use
- Track interview feedback
- Rate question effectiveness

---

## Non-Functional Requirements

### Performance
- JD parsing: < 5 seconds
- Candidate search: < 3 seconds (up to 100 results)
- Batch ranking: < 10 seconds (up to 50 candidates)
- UI responsiveness: < 500ms for interactions

### Reliability
- 99.5% API uptime
- Graceful degradation when Zoho API is unavailable
- Comprehensive error handling and logging
- Automatic retry mechanisms with exponential backoff

### Scalability
- Support 1000+ concurrent users
- Handle 10,000+ candidates per search
- Process bulk operations (ranking 100+ candidates)
- Efficient database queries with pagination

### Security
- OAuth 2.0 authentication with Zoho
- API key management via environment variables
- HTTPS for all communication
- Input validation and XSS protection
- SQL injection prevention through parameterized queries

### Usability
- Intuitive UI for non-technical recruiters
- Clear explanations for AI scoring decisions
- Mobile-responsive design
- Keyboard accessibility (WCAG 2.1 AA)

---

## Integration Points

### Job Source APIs (Primary + Multi-Site)
- **Zoho Recruit API**: Candidate data retrieval, job posting management, custom fields
- **Additional MCP Servers**: Support for LinkedIn, Indeed, Glassdoor, etc. via MCP protocol
- Custom field mapping and normalization across sources
- Activity tracking and candidate history

### Anthropic Claude API
- **Claude Haiku**: Cost-optimized AI model for all processing tasks
  - Job description analysis and requirement extraction
  - Candidate fit analysis and scoring
  - Interview question generation
- Prompt caching for efficiency
- Structured output generation

### MCP Protocol
- Tool definition and discovery
- Stateless request-response pattern
- Standard tool output format
- Multi-server routing and aggregation

---

## Success Metrics

### User Adoption
- 80% recruiter adoption within 3 months
- 2+ features used per recruiter per week

### Business Impact
- 50% reduction in manual screening time
- 30% improvement in offer acceptance rate
- 20% reduction in time-to-hire

### Quality
- 95% user satisfaction (NPS > 40)
- < 5% false positive rankings (bad candidates ranked high)
- < 2% false negatives (good candidates ranked low)

---

## Timeline & Priorities

### Phase 1 (MVP) - Weeks 1-4
- ✅ Job description parsing
- ✅ Candidate search
- ✅ Basic ranking

### Phase 2 - Weeks 5-8
- ✅ Detailed fit analysis
- ✅ Interview question generation
- ✅ Frontend dashboard

### Phase 3 - Weeks 9+
- Analytics and reporting
- Advanced filtering options
- Bulk operations
- Mobile app

---

## Constraints & Assumptions

### Constraints
- Zoho Recruit OAuth availability required
- Claude API rate limits must be respected
- Search results limited to Zoho Recruit database
- Java 21+ and Node.js 18+ required for development

### Assumptions
- Users have valid Zoho Recruit accounts
- Anthropic API credentials are available
- Job descriptions are provided in English
- Candidate data in Zoho is reasonably accurate

---

## Out of Scope (Phase 1)

- Offer generation and e-signature (Phase 3)
- Payroll or onboarding features
- Performance management
- ATS migration tools
- (Note: Multi-site integration is enabled via MCP servers as future enhancement)
