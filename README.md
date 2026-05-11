# Wissen AI ATS - Intelligent Recruitment Paradigm

![Wissen AI ATS](./docs/logo.png)

*An AI-powered Intelligent Applicant Tracking System built on Zoho Recruit and Anthropic Claude for the Wissen Hackathon 2026. Empowering recruiters with AI precision.*

**Team: Titan Transformers**
* Sudarshan Garg
* Saurabh Kumar
* Rupam Swain
* Suryaprakash Rao

---

## 🚀 Problem Statement

Modern talent acquisition pipelines are crippled by information overload and manual resume screening. Recruiters spend countless hours manually translating complex job descriptions into ATS search queries, retrieving hundreds of generic candidates, and subjectively scoring each one. This leads to:
- High time-to-hire metrics
- Recruiter burnout
- Suboptimal candidate matching
- Missed "hidden gem" candidates who lack perfect keyword density but have the right contextual experience.

**The Solution:** Wissen AI ATS automatically parses Job Descriptions (or uploaded PDFs) using LLMs, formulates precise Zoho Recruit queries, fetches candidate profiles, and algorithmically ranks them using Claude. It then provides recruiters with an actionable, transparent Fit Analysis.

---

## 🏗️ Architecture Diagram

Our solution utilizes a **three-tier Model Context Protocol (MCP)** architecture to safely separate frontend UX, client orchestration, and sensitive ATS tool execution.

```mermaid
graph TD
    UI[Frontend: React 19 UI] -->|REST API| Client[MCP Client: Spring Boot]
    Client -->|MCP Protocol via HTTP| Server[MCP Server: Spring Boot]
    
    subgraph "MCP Client Layer"
        Client <-->|Context Orchestration| ClaudeSonnet[Claude 3.5 Sonnet]
    end
    
    subgraph "MCP Server Layer"
        Server --> T1[@Tool: Parse JD]
        Server --> T2[@Tool: Search Zoho]
        Server --> T3[@Tool: Rank & Analyze Candidates]
    end
    
    Server <-->|OAuth 2.0| Zoho[Zoho Recruit API]
    Server <-->|API Calls| ClaudeHaiku[Claude 3.5 Haiku]
```

## 🏆 Why This Solution Wins

1. **First-Mover MCP Adoption**: By utilizing the new Model Context Protocol, we isolate sensitive APIs (Zoho, Anthropic) in the Server layer, while allowing the Client agent to orchestrate tools autonomously.
2. **"Luxury Minimalist" UI**: An investor-grade, stunning dark/light themed React interface with smooth micro-animations. It feels like a premium SaaS product, not a prototype.
3. **Session-Aware Caching**: Client-side storage drastically cuts API costs and load times. Repeating a recent query is instantaneous.
4. **Resilient Fallbacks**: If the AI encounters rate limits or errors, our backend gracefully degrades to standard regex-based parsing and keyword ranking.

---

## 🧠 Prompt Engineering Strategy

Our approach relies on **Chained Prompts & Model Tiering**:
- **Claude 3.5 Sonnet** (MCP Client side) dictates the overall orchestration. It acts as the intelligent controller, invoking tools as needed.
- **Claude 3.5 Haiku** (MCP Server side) acts as the workhorse for high-volume tasks. We built structured output instructions enforcing JSON responses for:
  - Extracting *Must-have* vs *Nice-to-have* skills from complex generic JDs.
  - Assessing candidate contextual experience rather than pure keyword matching.

Each prompt demands an `explanation` block *before* the `score`, enforcing Chain-of-Thought reasoning to reduce AI hallucination.

---

## 🔄 Zoho API Flow

1. **Authentication**: The Server boots and manages OAuth 2.0 refresh tokens securely using `ZohoRecruitOAuthService`.
2. **Criteria Building**: The AI's parsed JSON is handed to `ZohoCriteriaBuilder`, which translates semantic requirements into Zoho's strict query syntax (e.g., `(Skill_Set:equals:React) AND (Experience:equals:5)`).
3. **Data Retrieval**: `ZohoRecruitService` dynamically queries the `Candidates` endpoint, handles pagination, maps custom fields (Mobile vs Phone), and normalizes the data for the ranking engine.

---

## 📊 Ranking Methodology

Our LLM-powered ranking algorithm doesn't just match keywords:
1. **Skill Match Score**: Weighted parsing of direct technology overlaps.
2. **Experience Synergy**: Analyzing if the candidate's responsibilities align with the job level (Junior vs Staff).
3. **Fit Analysis**: A cohesive paragraph explaining *why* the candidate is a match and noting critical *Missing Skills*.

We dynamically reformat this into a composite `matchPercentage` and a precise `rankPosition` index for immediate UI display.

---

## 🛡️ Edge Cases Handled

- **Zoho API Rate Limiting**: Built-in retry logic with exponential backoff on HTTP 429 endpoints.
- **Invalid JDs / Empty Files**: Frontend validation restricts submission of empty queries or corrupted PDFs.
- **Malformed JSON AI Responses**: Regex fallback and structured format recovery mechanisms inside the Spring services.
- **Null Candidate Fields**: Unified data normalization converts null emails/phone fields into 'N/A' to prevent UI rendering crashes.

---

## 🚀 Deployment Steps

The platform is designed to be deployed via Docker in multi-stage builds (currently configured for Render).

**Prerequisites:** Docker, Node >= 18, Java >= 21.

1. **Clone the Repo:**
   ```bash
   git clone https://github.com/TitanTransformers/ZohoRecruit_Hackathon.git
   cd ZohoRecruit_Hackathon
   ```
2. **Configure Environment:**
   Set the following variables in deployment or `.env`:
   ```bash
   ZOHO_RECRUIT_CLIENT_ID="..."
   ZOHO_RECRUIT_CLIENT_SECRET="..."
   ZOHO_RECRUIT_REFRESH_TOKEN="..."
   ANTHROPIC_API_KEY="..."
   ```
3. **Build & Run (Docker Production):**
   ```bash
   docker build -t wissen-ai-ats .
   docker run -p 3000:3000 -e PORT=3000 wissen-ai-ats
   ```
4. **Local Development:**
   ```bash
   # Terminal 1: React UI
   cd zoho-recruit-ui && npm run dev
   
   # Terminal 2 & 3: Maven services
   cd mcp-server-demo && mvn spring-boot:run
   cd mcp-client-demo && mvn spring-boot:run
   ```

---

## 🎉 Demo Script for Judges

1. **Log In**: Open the UI. Show the beautiful animated login screen. Click the "Tony Stark" quick-login card to instantly access the dashboard.
2. **Sourcing Candidates**:
   - Navigate to "Source Candidates".
   - Paste a complex JD or upload a PDF.
   - Use the **"Fast" toggle** to demonstrate our dynamic payload orchestration (`Find candidates very fast` append).
   - Click **"Find Best Candidates"**.
3. **The Pipeline**: Point out the automated, animated pipeline stepper tracking the workflow: Parsing ➔ Querying ➔ AI Rank.
4. **The Reveal**: Show the resulting candidate cards. Expand a candidate to show the **Fit Analysis**, missing skills, and detailed sub-scores.
5. **History & Caching**: Click "Past Searches" in the sidebar. Select the last search—show how the results load *instantly* with zero latency and no new API cost thanks to localStorage caching.

---

## 🔮 Future Improvements

1. **LinkedIn / Indeed MCP Plugins**: Plugging in additional MCP tools to pull candidates cross-platform.
2. **Automated Outreach**: Enabling Claude to draft highly personalized email campaigns to the top-ranked candidates.
3. **Interview Scheduling**: Direct integration with Google Calendar / Outlook APIs to book interviews.
4. **Advanced Analytics**: Granular time-to-hire, diversity, and search efficiency dashboards.
