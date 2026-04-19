# MCP Server Demo – AI-Powered Recruitment Pipeline

## Overview

This project is a **Model Context Protocol (MCP) Server** built with **Spring Boot** and **Spring AI** that provides an intelligent, AI-powered recruitment pipeline. It integrates with **Zoho Recruit ATS** for candidate data and uses **Claude Haiku (Anthropic)** as the LLM for semantic analysis, job description parsing, candidate ranking, and interview preparation.

The server exposes MCP tools over **Streamable HTTP** transport, allowing any MCP-compatible client to leverage the recruitment capabilities.

---

## Architecture

```
┌─────────────────┐      MCP (HTTP)      ┌──────────────────────┐
│   MCP Client    │ ◄──────────────────► │   MCP Server         │
│ (Claude Desktop,│                      │   (Spring Boot)      │
│  ai-recruitment │                      │                      │
│  -client.js)    │                      │  ┌────────────────┐  │
└─────────────────┘                      │  │ RecruitmentTools│  │
                                         │  └───────┬────────┘  │
                                         │          │           │
                              ┌──────────┼──────────┼───────────┤
                              │          ▼          ▼           │
                              │  ┌─────────┐  ┌──────────┐     │
                              │  │ AI JD    │  │ AI Rank  │     │
                              │  │ Service  │  │ Service  │     │
                              │  └────┬─────┘  └────┬─────┘     │
                              │       │             │           │
                              │       ▼             ▼           │
                              │  ┌─────────────────────┐       │
                              │  │  Claude Haiku LLM   │       │
                              │  │  (Anthropic API)    │       │
                              │  └─────────────────────┘       │
                              │                                 │
                              │  ┌─────────────────────┐       │
                              │  │  Zoho Recruit ATS   │       │
                              │  │  (OAuth2 + REST)    │       │
                              │  └─────────────────────┘       │
                              └─────────────────────────────────┘
```

---

## MCP Tools

The server exposes **6 MCP tools** via the `RecruitmentTools` class:

### 1. `parseJobDescription`
**Purpose:** Ingests raw job description text and extracts structured metadata using Claude Haiku AI.

**Extracted fields:**
- Job title, department, location
- Required & preferred skills
- Experience level and years required
- Qualifications and responsibilities

---

### 2. `searchCandidatesInZohoRecruit`
**Purpose:** Searches Zoho Recruit ATS for candidates matching a given job description.

**Pipeline:**
1. Parses the JD with Claude AI
2. Generates search criteria (skills, designation, experience, location)
3. Builds Zoho Recruit criteria expressions using `ZohoCriteriaBuilder`
4. Retrieves matching candidate profiles via Zoho Recruit REST API (OAuth2)

---

### 3. `findAndRankCandidatesForJD`
**Purpose:** End-to-end recruitment pipeline — parse JD → search candidates → rank with AI.

This is the primary tool. It returns a recruiter-ready ranked list with match percentages, matched/missing skills, and fit analysis for each candidate.

---

### 4. `generateSearchFiltersFromJD`
**Purpose:** Generates structured search filters and a Boolean search query from a job description. Useful for exporting filters to other talent databases.

---

### 5. `getDetailedCandidateFitAnalysis`
**Purpose:** Deep-dive AI analysis of a single candidate against a job description.

**Returns:** Strengths, weaknesses, development areas, growth potential, cultural fit assessment, interview focus areas, and estimated ramp-up time.

---

### 6. `generateCustomInterviewQuestions`
**Purpose:** Generates targeted interview questions tailored to a specific candidate and job.

**Each question includes:** Question text, category (technical/behavioral/domain/culture), and rationale.

---

## Candidate Ranking Algorithm

Ranking is performed by `AIEnhancedCandidateRankingService` using Claude Haiku as a semantic evaluator.

### Scoring Formula

```
matchPercentage = 0.60 × skillMatch + 0.25 × experienceMatch + 0.15 × softSkillMatch
```

| Weight | Component | Description |
|--------|-----------|-------------|
| 60% | **Skill Match** | Semantic matching of candidate skills against required & preferred skills |
| 25% | **Experience Match** | Years of experience and role-level alignment |
| 15% | **Soft Skills** | Communication, leadership, cultural indicators |

### How It Works

1. **Compact Prompt Construction** – Candidates are serialized to a minimal JSON format (id, title, skills) to reduce token usage.
2. **Single LLM Call** – All candidates are evaluated in one prompt for consistency in relative ranking.
3. **Structured JSON Output** – Claude returns a JSON array with per-candidate scoring fields:
   - `matchPercentage`, `skillMatchPercentage`, `experienceMatchPercentage`
   - `matchedSkills`, `missingSkills`
   - `matchReasoning`, `fitAnalysis`
4. **Robust JSON Parsing** – The response is parsed with bracket-matching extraction, markdown stripping, and truncated-JSON recovery (handles incomplete LLM responses by finding the last complete object and closing the array).
5. **Enrichment** – AI scores are merged with candidate profile data (name, email, phone) from Zoho Recruit.
6. **Sorting** – Results are sorted by `matchPercentage` descending.

### JSON Recovery Strategy

The service includes a multi-layer JSON recovery mechanism for handling truncated LLM responses:

- **Bracket matching** to extract JSON from surrounding text
- **Markdown code-block stripping** (`\`\`\`json ... \`\`\``)
- **Partial recovery** – if the response is cut off mid-object, it truncates to the last complete JSON object and closes the array

---

## Interview Question Generation

The `generateCustomInterviewQuestions` tool (powered by `AIEnhancedCandidateRankingService.generateInterviewQuestions()`) creates personalized interview questions by analyzing both the candidate profile and the job description together.

### How It Works

1. **Input** – Takes the candidate's name, current position, experience summary, skills, and the full job description.
2. **AI Prompt** – Claude Haiku receives the candidate profile alongside the job's required skills and top 3 responsibilities, and generates targeted questions.
3. **Configurable Count** – The number of questions is configurable (default: 8, max: 15).

### Output Per Question

| Field | Description |
|-------|-------------|
| `question` | The interview question text |
| `category` | One of: **technical**, **behavioral**, **domain**, **culture** |
| `rationale` | Why this question is important for evaluating this specific candidate |

### Categories Assessed

- **Technical** – Programming skills, system design, domain-specific knowledge
- **Behavioral** – Past experiences, conflict resolution, teamwork
- **Domain** – Industry knowledge, role-specific competencies
- **Culture** – Values alignment, work style, team fit

### Example Response Structure

```json
{
  "candidateId": "12345",
  "candidateName": "Jane Doe",
  "jobTitle": "Senior Java Developer",
  "totalQuestions": 8,
  "questions": [
    {
      "question": "Describe a time you optimized a Java application for performance...",
      "category": "technical",
      "rationale": "Candidate lists Java as a skill; this assesses depth of expertise"
    }
  ]
}
```

---

## Detailed Candidate Fit Analysis

The `getDetailedCandidateFitAnalysis` tool provides a deep-dive AI assessment of a single candidate against a job description.

### Output Fields

| Field | Description |
|-------|-------------|
| `overallFit` | Percentage score (0–100) |
| `strengths` | List of candidate strengths relative to the role |
| `weaknesses` | List of skill gaps or concerns |
| `developmentAreas` | Areas where the candidate could grow |
| `potentialToGrow` | Boolean – whether the candidate shows growth potential |
| `culturalFit` | Free-text assessment of cultural alignment |
| `recommendedInterviewFocus` | Suggested areas to probe during interviews |
| `estimatedRampUpTime` | How long until the candidate would be fully productive |

---

## Job Description Parsing

`AIEnhancedJobDescriptionService` uses Claude Haiku to intelligently extract structured metadata from free-text job descriptions and maps them to Zoho Recruit field names:

| Extracted Field | Zoho Recruit Field |
|---|---|
| jobTitle | Current_Job_Title |
| requiredSkills | Skill_Set |
| experienceLevel | Experience_in_Years |
| location | City |
| qualifications | Highest_Qualification_Held |

Experience levels are mapped to minimum years: Junior → 0, Mid → 3, Senior → 7, Lead/Executive → 10.

---

## Zoho Recruit Integration

- **OAuth2 client-credentials** flow for authentication (`ZohoRecruitOAuthService`)
- **REST API** calls for candidate search and details (`ZohoRecruitAPIService`)
- **`ZohoCriteriaBuilder`** generates valid Zoho criteria expressions with proper operators per field type (numeric fields use `greater_equal`/`less_equal`; string fields use `contains`/`equals`)
- Supports search by: skills, experience, location, designation, salary, qualification, employer, and more

---

## Configuration

### Environment Variables

| Variable | Required | Description |
|---|---|---|
| `ANTHROPIC_API_KEY` | ✅ | Anthropic API key for Claude Haiku |
| `ZOHO_RECRUIT_CLIENT_ID` | ✅ | Zoho OAuth client ID |
| `ZOHO_RECRUIT_CLIENT_SECRET` | ✅ | Zoho OAuth client secret |
| `ZOHO_RECRUIT_ORG_ID` | ✅ | Zoho organization ID |
| `LLM_MODEL` | ❌ | Override LLM model (default: `claude-3-5-haiku-20241022`) |

### Server

- **Port:** 8080
- **MCP Transport:** Streamable HTTP (async)
- **Max tokens:** 8192

---

## Running the Server

```bash
# Set required environment variables
export ANTHROPIC_API_KEY=your-key
export ZOHO_RECRUIT_CLIENT_ID=your-client-id
export ZOHO_RECRUIT_CLIENT_SECRET=your-client-secret
export ZOHO_RECRUIT_ORG_ID=your-org-id

# Build and run
./mvnw spring-boot:run
```

The MCP server will be available at `http://localhost:8080`.

---

## Project Structure

```
src/main/java/com/mcp/mcp_server/
├── McpServerApplication.java          # Spring Boot entry point
├── config/
│   ├── AIModelConfiguration.java      # Claude Haiku ChatClient bean
│   ├── McpServerToolProviderConfig.java # MCP tool registration
│   └── ZohoRecruitPortalConnectionConfig.java
├── entity/
│   ├── Candidate.java                 # Candidate profile entity
│   ├── JobDescription.java            # Parsed JD entity
│   └── RankedCandidate.java           # Ranked candidate with scores
├── service/
│   ├── AIEnhancedCandidateRankingService.java  # AI ranking engine
│   ├── AIEnhancedJobDescriptionService.java    # AI JD parser
│   ├── ZohoRecruitService.java                 # Zoho facade
│   ├── ZohoRecruitAPIService.java              # Zoho REST client
│   ├── ZohoRecruitOAuthService.java            # OAuth2 token mgmt
│   └── ZohoCriteriaBuilder.java                # Zoho query builder
├── tools/
│   └── RecruitmentTools.java          # MCP tool definitions
└── util/
    └── ZohoRecruitCandidateSearchField.java  # Valid search fields enum
```

