# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a hackathon project implementing a recruitment platform using the **Model Context Protocol (MCP)**. It consists of three main modules:

1. **mcp-server-demo**: Spring Boot MCP server (port 8080) exposing AI-enhanced recruitment tools
2. **mcp-client-demo**: Spring Boot MCP client (port 8081) that consumes the server's tools
3. **zoho-recruit-ui**: React + TypeScript + Vite frontend UI

### Architecture

```
┌─────────────────────────────────────────┐
│     React UI (zoho-recruit-ui)          │
│   Port 5173 (dev) / built to dist/      │
└────────────────┬────────────────────────┘
                 │ HTTP/WebSocket
                 │
┌────────────────▼────────────────────────┐
│  MCP Client (Spring Boot, Port 8081)    │
│  - Connects to MCP Server               │
│  - Serves REST API for frontend         │
│  - Uses Claude to orchestrate tools     │
└────────────────┬────────────────────────┘
                 │ MCP Protocol (HTTP)
                 │
┌────────────────▼────────────────────────┐
│  MCP Server (Spring Boot, Port 8080)    │
│  - Exposes recruitment MCP tools        │
│  - Integrates with Zoho Recruit API     │
│  - Uses Claude Haiku for AI analysis    │
└─────────────────────────────────────────┘
```

## Building & Running

### Prerequisites
- Java 21+
- Maven 3.9+
- Node.js 18+ (for React UI)
- Environment variables for Zoho Recruit and Anthropic API keys

### Build All Modules
```bash
# MCP Server
cd mcp-server-demo
mvn clean package

# MCP Client
cd ../mcp-client-demo
mvn clean package

# React UI
cd ../zoho-recruit-ui
npm install
npm run build
```

### Run Services
```bash
# Terminal 1: MCP Server (port 8080)
cd mcp-server-demo
export ZOHO_RECRUIT_CLIENT_ID="..."
export ZOHO_RECRUIT_CLIENT_SECRET="..."
export ZOHO_RECRUIT_REFRESH_TOKEN="..."
export ZOHO_RECRUIT_ORG_ID="..."
export ANTHROPIC_API_BASE_URL="https://api.anthropic.com/v1"
mvn spring-boot:run

# Terminal 2: MCP Client (port 8081)
cd mcp-client-demo
mvn spring-boot:run

# Terminal 3: React UI (port 5173)
cd zoho-recruit-ui
npm run dev
```

### Run Tests
```bash
# Server tests
cd mcp-server-demo
mvn test

# Run specific test
mvn test -Dtest=ZohoCriteriaBuilderTest
```

## Key Architecture Patterns

### MCP Tools
The server exposes recruitment tools via Spring AI's `@Tool` annotation:
- `parseJobDescription` - Extract structured data from job descriptions using Claude
- `searchCandidatesInZohoRecruit` - Query Zoho Recruit database
- `findAndRankCandidatesForJD` - End-to-end pipeline: search + rank + analyze
- `generateSearchFiltersFromJD` - Generate Zoho search criteria from JD
- `getDetailedCandidateFitAnalysis` - AI-powered fit scoring and explanation
- `generateCustomInterviewQuestions` - Generate tailored interview questions

### Service Layer
- **AIEnhancedCandidateRankingService**: Ranks candidates using Claude Haiku with fallback to regex-based ranking
- **AIEnhancedJobDescriptionService**: Parses JD using Claude with structured extraction fallback
- **ZohoRecruitService**: Unified interface for Zoho Recruit API interactions
- **ZohoRecruitOAuthService**: Handles OAuth token refresh for Zoho API
- **ZohoCriteriaBuilder**: Constructs Zoho search criteria with validation

### Configuration
- **mcp-server-demo**: `application.yaml` configures MCP server, Claude Haiku, and Zoho Recruit integration
- **mcp-client-demo**: `application.yaml` configures MCP client connection and Claude Sonnet
- API keys injected via environment variables or placeholders in YAML

## Common Development Tasks

### Adding a New MCP Tool
1. Add `@Tool` annotated method to `RecruitmentTools.java`
2. Use `ZohoRecruitService` or other services for implementation
3. Return `Map<String, Object>` with result structure
4. Test with `mvn test` to ensure tool is discoverable

### Modifying Zoho Integration
Edit `ZohoRecruitService.java` for API calls or `ZohoCriteriaBuilder.java` for search criteria construction.
Tests: `ZohoCriteriaBuilderTest.java`, `VerifyCriteriaFormatTest.java`

### Updating AI Prompts
Claude prompts are embedded in:
- `AIEnhancedJobDescriptionService.java` - JD parsing logic
- `AIEnhancedCandidateRankingService.java` - Candidate ranking prompts

## Important Notes

### Sensitive Data
- API keys are in `application.yaml` but should be overridden via environment variables in production
- Never commit real Anthropic or Zoho credentials to version control

### Spring AI Version
Both modules use Spring AI 2.0.0-M4 with MCP support. Ensure compatibility when updating dependencies.

### Port Configuration
- MCP Server: 8080 (configured in `application.yaml`)
- MCP Client: 8081 (configured in `application.yaml`)
- React UI: 5173 (dev), served via MCP Client in production

### Testing
Tests are located in `mcp-server-demo/src/test/`. Focus on:
- Criteria validation (`ZohoCriteriaBuilderTest.java`)
- API format verification (`VerifyCriteriaFormatTest.java`)
- Integration tests for service layer

### React UI Structure
- Uses Material-UI (MUI) for components
- Redux Toolkit for state management
- React Router for navigation
- Built with Vite for fast development

## File Organization

### mcp-server-demo
```
src/main/java/com/mcp/mcp_server/
├── config/          # Spring configuration, health checks
├── entity/          # Data models (Candidate, JobDescription, etc.)
├── service/         # Business logic (AI services, Zoho integration)
├── tools/           # MCP tool definitions
└── util/            # Utilities
```

### mcp-client-demo
```
src/main/java/com/mcp/mcp_client/
├── config/          # MCP client configuration
├── controller/      # REST endpoints
├── dto/             # Data transfer objects
└── service/         # Client-side service logic
```

### zoho-recruit-ui
```
src/
├── components/      # React components
├── pages/           # Page-level components
├── store/           # Redux store slices
├── services/        # API client services
└── types/           # TypeScript type definitions
```

## Development Notes

- The project uses Java 21 for both Spring Boot modules
- Spring Boot version 4.0.5 (latest stable)
- React 19.2.4 with TypeScript 6.0.2
- All three modules should be running for full functionality
- Health check endpoint: `GET http://localhost:8080/health` (server only)
