# Quick Start: Refactored Code

## 5-Minute Setup

### 1. Install Dependencies
```bash
npm install
```

### 2. Create Environment File
```bash
# Copy the example template
cp .env.example .env

# Edit .env with your values (defaults work for local development)
```

### 3. Start Development Server
```bash
npm run dev
```

### 4. Open in Browser
```
http://localhost:5173
```

## Key Files to Know

| File | Purpose |
|------|---------|
| `.env` | Your API configuration |
| `src/config/environment.ts` | Load `.env` variables |
| `src/services/candidateService.ts` | Handle API calls |
| `src/components/CandidateResultsTable.tsx` | Display results |
| `src/pages/DocumentUploadPage.tsx` | Main search interface |

## How It Works

```
User Input
    ↓
DocumentUploadPage (component)
    ↓
candidateService (API call)
    ↓
API Backend
    ↓
Parse Response
    ↓
Store in Redux (candidates)
    ↓
CandidateResultsTable (display)
    ↓
User sees results
```

## Common Tasks

### Search by Job Description
```typescript
const candidates = await candidateService.searchByText(jobDescription);
dispatch(setCandidates(candidates));
```

### Search by PDF Document
```typescript
const candidates = await candidateService.searchByDocument(pdfFile, jobDescription);
dispatch(setCandidates(candidates));
```

### Display Results
```typescript
<CandidateResultsTable candidates={candidates} loading={false} />
```

## Environment Variables

**Required:**
```
VITE_API_BASE_URL=http://localhost:8081
```

**Optional:**
```
VITE_API_CHAT_ENDPOINT=/api/chat/send (default)
VITE_API_DOCUMENTS_ENDPOINT=/api/documents/process (default)
VITE_ENABLE_DEBUG_MODE=false (default)
```

## API Response Format

Your backend must return candidates in this format:

```json
{
  "data": [
    {
      "name": "John Doe",
      "email": "john@example.com",
      "matchedSkill": ["JavaScript", "React"],
      "missingSkills": ["Python"],
      "analysis": "Strong candidate...",
      "matchedPercentage": 85.5
    }
  ]
}
```

Or directly as array:
```json
[
  { "name": "...", "email": "...", ... }
]
```

## Build for Production

```bash
# Build
npm run build

# Preview production build
npm run preview
```

## Debugging

Enable debug logs in `.env`:
```
VITE_ENABLE_DEBUG_MODE=true
```

Then check browser console for detailed logs.

## Troubleshooting

**API calls not working?**
- Check `.env` has correct `VITE_API_BASE_URL`
- Verify backend is running
- Check browser Network tab for errors

**Results not showing?**
- Verify API response includes all required fields
- Check browser console for TypeScript errors
- Ensure candidates array is not empty

**Build errors?**
- Run `npm install` again
- Delete `node_modules` and reinstall
- Check Node.js version (v16+ required)

## Learn More

- `REFACTORING_GUIDE.md` - Full technical documentation
- `MIGRATION_GUIDE.md` - How to adapt existing code
- `BACKEND_INTEGRATION_GUIDE.md` - Backend integration details
