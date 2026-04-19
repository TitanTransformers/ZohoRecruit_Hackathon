# Zoho Recruit - AI-Powered Recruitment Platform

## Project Overview

Zoho Recruit AI is an intelligent recruitment platform that leverages the **Model Context Protocol (MCP)** and Claude AI to enhance recruitment workflows. The platform automates candidate screening, ranking, and job-fit analysis while seamlessly integrating with the Zoho Recruit ecosystem.

## Core Features

- **AI-Powered Job Description Analysis**: Automatically extract structured requirements from job descriptions
- **Intelligent Candidate Search**: Generate optimized search criteria and find matching candidates in Zoho Recruit
- **Automated Candidate Ranking**: Score and rank candidates based on JD alignment and fit analysis
- **Interview Question Generation**: Create tailored, role-specific interview questions
- **Detailed Fit Analysis**: Provide comprehensive candidate-to-job fit scoring with explanations
- **Real-time Integration**: Synchronize with Zoho Recruit API for candidate and job data

## Architecture Summary

### Three-Tier Architecture
```
┌──────────────────────────┐
│   React UI (Port 5173)   │
│  zoho-recruit-ui         │
└────────────┬─────────────┘
             │
┌────────────▼──────────────┐
│  MCP Client (Port 8081)   │
│  mcp-client-demo          │
│  - REST API Gateway       │
│  - Tool Orchestration     │
└────────────┬──────────────┘
             │
┌────────────▼──────────────┐
│  MCP Server (Port 8080)   │
│  mcp-server-demo          │
│  - Recruitment Tools      │
│  - Zoho Integration       │
└──────────────────────────┘
```

### Module Breakdown

| Module | Purpose | Tech Stack | Port |
|--------|---------|-----------|------|
| **mcp-server-demo** | MCP Server with recruitment tools | Java 21, Spring Boot 4.0.5, Spring AI | 8080 |
| **mcp-client-demo** | MCP Client & REST API Gateway | Java 21, Spring Boot 4.0.5 | 8081 |
| **zoho-recruit-ui** | Frontend UI | React 19, TypeScript, MUI, Redux Toolkit | 5173 |

## Key Technologies

- **Backend**: Java 21, Spring Boot, Spring AI (MCP Protocol)
- **Frontend**: React 19, TypeScript 6.0, Material-UI, Redux Toolkit, Vite
- **AI**: Claude Haiku (server), Claude Sonnet (client)
- **APIs**: Zoho Recruit REST API, MCP (Model Context Protocol)
- **Build**: Maven 3.9+, npm

## Getting Started

### Prerequisites
- Java 21+
- Maven 3.9+
- Node.js 18+
- Anthropic API Key
- Zoho Recruit API Credentials

### Quick Start

```bash
# Build all modules
./build.sh  # or manual Maven/npm builds

# Run services (each in separate terminal)
# Terminal 1: MCP Server
cd mcp-server-demo && mvn spring-boot:run

# Terminal 2: MCP Client
cd mcp-client-demo && mvn spring-boot:run

# Terminal 3: React UI
cd zoho-recruit-ui && npm run dev
```

Access the application at `http://localhost:5173`

## Development Workflow

1. **Backend Changes**: Edit Java code in mcp-server-demo or mcp-client-demo
2. **AI Prompts**: Update prompts in service classes (AIEnhancedJobDescriptionService, etc.)
3. **Frontend Changes**: Edit React components in zoho-recruit-ui
4. **API Integration**: Extend ZohoRecruitService for new Zoho endpoints

## Security Notes

- API keys managed via environment variables
- OAuth token refresh for Zoho API
- Spring Security configured for authentication
- All sensitive data excluded from version control

## Documentation

- **PRD.md**: Complete requirements and feature specifications
- **Architecture.md**: System design, data flow, and API documentation
- **CLAUDE.md**: Developer instructions and project structure

## Support & Contributions

For issues or questions, refer to the comprehensive documentation or review the service implementations in the codebase.
