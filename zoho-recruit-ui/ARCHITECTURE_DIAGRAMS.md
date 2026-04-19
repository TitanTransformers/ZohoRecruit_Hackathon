# 📊 Zoho Recruit Sourcing Tool - Architecture & Flow Diagrams

## System Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                     USER BROWSER                                 │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │        React Frontend (http://localhost:5173)             │  │
│  │  ┌──────────────────────────────────────────────────────┐ │  │
│  │  │        DocumentUploadPage Component                  │ │  │
│  │  │  ┌─────────────────────────────────────────────────┐ │ │  │
│  │  │  │  Job Description Input (Text or PDF)            │ │ │  │
│  │  │  │  • Text area (10,000 char limit)                │ │ │  │
│  │  │  │  • PDF file upload button                       │ │ │  │
│  │  │  └─────────────────────────────────────────────────┘ │ │  │
│  │  │  ┌─────────────────────────────────────────────────┐ │ │  │
│  │  │  │  Redux State Management                         │ │ │  │
│  │  │  │  • text, pdfFile, results, resultColumns        │ │ │  │
│  │  │  └─────────────────────────────────────────────────┘ │ │  │
│  │  │  ┌─────────────────────────────────────────────────┐ │ │  │
│  │  │  │  Dynamic Results Table                          │ │ │  │
│  │  │  │  • Auto-generated columns from API response     │ │ │  │
│  │  │  │  • Styled with Material-UI                      │ │ │  │
│  │  │  └─────────────────────────────────────────────────┘ │ │  │
│  │  └──────────────────────────────────────────────────────┘ │  │
│  └───────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
                              │
                              │ HTTP POST
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                  BACKEND API SERVER                              │
│              (http://localhost:8081)                             │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │        Spring Boot DocumentController                      │  │
│  │  ┌──────────────────────────────────────────────────────┐ │  │
│  │  │  POST /api/documents/process                         │ │  │
│  │  │  • Accepts JSON or FormData                          │ │  │
│  │  │  • Extracts Job Description (text or PDF)            │ │  │
│  │  └──────────────────────────────────────────────────────┘ │  │
│  └───────────────────────────────────────────────────────────┘  │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │        ZohoRecruitService                                  │  │
│  │  ┌──────────────────────────────────────────────────────┐ │  │
│  │  │  • Parse Job Description                            │ │  │
│  │  │  • Extract keywords (skills, experience, etc)       │ │  │
│  │  │  • Search Zoho Recruit ATS                          │ │  │
│  │  │  • Return matching candidates                       │ │  │
│  │  └──────────────────────────────────────────────────────┘ │  │
│  └───────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
                              │
                              │ HTTP Response (JSON Array)
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                  ZOHO RECRUIT ATS                                │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │  Candidate Database                                        │  │
│  │  • Store all candidate profiles                           │  │
│  │  • Search by skills, experience, location, etc.           │  │
│  │  • Return matching candidates with scoring                │  │
│  └───────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

## Data Flow Diagram

```
USER ACTION                  STATE CHANGE              API CALL                RESPONSE               UI UPDATE
─────────────────────────────────────────────────────────────────────────────────────────────────────────────────

1. Enter Job Description
   ▼
User Types Text
   │
   └─► handleTextChange()
       │
       └─► dispatch(setText(value))
           │
           └─► Redux State Updated
               {text: "Senior Java Developer..."}

2. Upload PDF (Optional)
   ▼
User Selects File
   │
   └─► handleFileChange(event)
       │
       ├─► Validate (PDF only)
       │
       └─► dispatch(setPdfFile(file))
           │
           └─► Redux State Updated
               {pdfFile: File}

3. Click "Search Candidates"
   ▼
handleSubmit() Called
   │
   ├─► Validate Input
   │   {hasInput check}
   │
   ├─► dispatch(setLoading(true))
   │   │
   │   └─► UI Shows Loading State
   │
   └─► Format Payload
       │
       ├─► If text only:
       │   {message: text}
       │   Content-Type: application/json
       │
       └─► If PDF:
           FormData {message?, pdf}
           Content-Type: multipart/form-data
           │
           ▼
       POST http://localhost:8081/api/documents/process
           │
           ▼
       BACKEND PROCESSES
       ├─► Extract Job Description
       ├─► Search Zoho Recruit ATS
       ├─► Score Matching Candidates
       └─► Return Array of Candidates

4. API Response Received
   ▼
Parse Response
   │
   ├─► Extract candidates from:
   │   • Direct array: [...]
   │   • {results: [...]}
   │   • {candidates: [...]}
   │
   └─► Extract Column Names
       {candidateId, firstName, lastName, email, ...}
           │
           ▼
       dispatch(setResults(candidates))
           │
           ├─► results = candidates
           ├─► resultColumns = extracted keys
           └─► Redux State Updated

5. Display Results
   ▼
Component Re-renders
   │
   ├─► Success Alert: "Found 5 Matching Candidates"
   │
   ├─► Results Table Renders
   │   ├─► TableHead with formatted columns
   │   │   └─► candidateId → Candidate Id
   │   │   └─► firstName → First Name
   │   │   └─► email → Email
   │   │
   │   └─► TableBody with data rows
   │       └─► Each candidate = 1 row
   │       └─► Values truncated to 100 chars
   │
   └─► Action Buttons
       ├─► "New Search" ─► handleReset()
       └─► "Export Results" ─► Future Feature

6. User Actions
   ▼
   ├─► Click "New Search"
   │   └─► dispatch(resetForm())
   │       └─► All state cleared
   │       └─► Back to input form
   │
   └─► Click "Export Results"
       └─► Future implementation
```

## Component Hierarchy

```
App.tsx
│
├─► Redux Provider
│   └─► store configuration
│
└─► DocumentUploadPage.tsx
    │
    ├─► Header Section
    │   ├─► Title: "🔍 Zoho Recruit Sourcing Tool"
    │   └─► Subtitle: "Search Wissen's Zoho Recruit ATS..."
    │
    ├─► GradientCard
    │   │
    │   ├─► Text Input Section
    │   │   ├─► Label: "📋 Paste Job Description"
    │   │   ├─► Character Counter
    │   │   ├─► StyledTextField (multiline)
    │   │   └─► Remaining Characters Display
    │   │
    │   ├─► Divider with "OR" chip
    │   │
    │   ├─► PDF Upload Section
    │   │   ├─► Label: "📄 Upload Job Description (Optional)"
    │   │   ├─► File Input Button
    │   │   ├─► File Name Display (Chip)
    │   │   └─► Error Alert (conditional)
    │   │
    │   ├─► Alert Section
    │   │   ├─► Error Alert (red, if error)
    │   │   └─► Success Alert (green, if success)
    │   │
    │   └─► Action Buttons
    │       ├─► "Search Candidates" (Contained)
    │       └─► "Clear Form" (Outlined)
    │
    ├─► Info Cards (when no input)
    │   ├─► Card 1: "🔍 Advanced Search"
    │   ├─► Card 2: "🎯 Smart Matching"
    │   └─► Card 3: "⚡ Instant Results"
    │
    └─► Results Table (when data exists)
        │
        ├─► Header: "👥 Found {n} Matching Candidates"
        │   └─► Chip: "{n} profiles"
        │
        ├─► ResultsTableContainer
        │   │
        │   └─► Table
        │       │
        │       ├─► StyledTableHead
        │       │   └─► TableRow
        │       │       └─► TableCell (for each column)
        │       │           "Column Name"
        │       │
        │       └─► TableBody
        │           └─► StyledTableRow (for each candidate)
        │               └─► TableCell (for each column)
        │                   "Data Value (truncated)"
        │
        └─► Action Buttons
            ├─► "New Search"
            └─► "Export Results"
```

## State Management Flow

```
┌──────────────────────────────────────────────────────────────┐
│                    REDUX STORE                                │
│                                                                │
│  ┌────────────────────────────────────────────────────────┐  │
│  │              DocumentState Interface                    │  │
│  │                                                          │  │
│  │  text: string                 // Job description       │  │
│  │  pdfFile: File | null         // Uploaded PDF          │  │
│  │  loading: boolean              // API loading state    │  │
│  │  error: string | null          // Error message        │  │
│  │  success: boolean              // Success flag         │  │
│  │  results: Record[]             // Candidate results    │  │
│  │  resultColumns: string[]       // Table columns        │  │
│  │                                                          │  │
│  └────────────────────────────────────────────────────────┘  │
│                                                                │
│  ┌────────────────────────────────────────────────────────┐  │
│  │                   Actions (Reducers)                    │  │
│  │                                                          │  │
│  │  • setText(payload: string)                             │  │
│  │    └─► state.text = payload                             │  │
│  │                                                          │  │
│  │  • setPdfFile(payload: File | null)                     │  │
│  │    └─► state.pdfFile = payload                          │  │
│  │                                                          │  │
│  │  • setLoading(payload: boolean)                         │  │
│  │    └─► state.loading = payload                          │  │
│  │                                                          │  │
│  │  • setError(payload: string | null)                     │  │
│  │    └─► state.error = payload                            │  │
│  │                                                          │  │
│  │  • setSuccess(payload: boolean)                         │  │
│  │    └─► state.success = payload                          │  │
│  │                                                          │  │
│  │  • setResults(payload: Record[])                        │  │
│  │    ├─► state.results = payload                          │  │
│  │    └─► state.resultColumns = Object.keys(payload[0])   │  │
│  │                                                          │  │
│  │  • resetForm()                                           │  │
│  │    ├─► state.text = ''                                  │  │
│  │    ├─► state.pdfFile = null                             │  │
│  │    ├─► state.error = null                               │  │
│  │    ├─► state.success = false                            │  │
│  │    ├─► state.results = []                               │  │
│  │    └─► state.resultColumns = []                         │  │
│  │                                                          │  │
│  └────────────────────────────────────────────────────────┘  │
│                                                                │
└──────────────────────────────────────────────────────────────┘
```

## Request/Response Format

```
┌─────────────────────────────────────────────────────────────┐
│                  FRONTEND REQUEST                             │
│                                                               │
│  CASE 1: Text Only                                            │
│  ────────────────                                             │
│  POST /api/documents/process HTTP/1.1                        │
│  Content-Type: application/json                              │
│                                                               │
│  {                                                            │
│    "message": "Senior Java Developer with 5+ years..."       │
│  }                                                            │
│                                                               │
│  ───────────────────────────────────────────────────────────│
│                                                               │
│  CASE 2: PDF Only                                             │
│  ─────────────────                                            │
│  POST /api/documents/process HTTP/1.1                        │
│  Content-Type: multipart/form-data; boundary=...            │
│                                                               │
│  --boundary                                                   │
│  Content-Disposition: form-data; name="pdf"; filename="jd.pdf"
│                                                               │
│  [PDF binary data]                                            │
│  --boundary--                                                 │
│                                                               │
│  ───────────────────────────────────────────────────────────│
│                                                               │
│  CASE 3: Text + PDF                                           │
│  ────────────────────                                         │
│  POST /api/documents/process HTTP/1.1                        │
│  Content-Type: multipart/form-data; boundary=...            │
│                                                               │
│  --boundary                                                   │
│  Content-Disposition: form-data; name="message"              │
│                                                               │
│  Senior Java Developer needed for microservices project       │
│  --boundary                                                   │
│  Content-Disposition: form-data; name="pdf"; filename="jd.pdf"
│                                                               │
│  [PDF binary data]                                            │
│  --boundary--                                                 │
│                                                               │
└─────────────────────────────────────────────────────────────┘
                              │
                              │ BACKEND PROCESSING
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                  BACKEND RESPONSE                             │
│                                                               │
│  HTTP/1.1 200 OK                                              │
│  Content-Type: application/json                              │
│                                                               │
│  [                                                            │
│    {                                                          │
│      "candidateId": "C001",                                   │
│      "firstName": "John",                                     │
│      "lastName": "Doe",                                       │
│      "email": "john.doe@example.com",                         │
│      "phone": "+1-555-0123",                                  │
│      "skills": "Java, Spring Boot, Microservices",           │
│      "experience": "5 years",                                 │
│      "matchScore": "92%"                                      │
│    },                                                         │
│    {                                                          │
│      "candidateId": "C002",                                   │
│      "firstName": "Jane",                                     │
│      "lastName": "Smith",                                     │
│      "email": "jane.smith@example.com",                       │
│      "phone": "+1-555-0456",                                  │
│      "skills": "React, TypeScript, Node.js",                 │
│      "experience": "4 years",                                 │
│      "matchScore": "88%"                                      │
│    }                                                          │
│  ]                                                            │
│                                                               │
│  ───────────────────────────────────────────────────────────│
│                                                               │
│  ALTERNATIVE: Nested Response                                │
│  ──────────────────────────────                              │
│                                                               │
│  {                                                            │
│    "results": [                                               │
│      { /* candidate 1 */ },                                   │
│      { /* candidate 2 */ }                                    │
│    ]                                                          │
│  }                                                            │
│                                                               │
│  ───────────────────────────────────────────────────────────│
│                                                               │
│  OR: Nested in "candidates"                                  │
│  ─────────────────────────                                   │
│                                                               │
│  {                                                            │
│    "candidates": [                                            │
│      { /* candidate 1 */ },                                   │
│      { /* candidate 2 */ }                                    │
│    ]                                                          │
│  }                                                            │
│                                                               │
└─────────────────────────────────────────────────────────────┘
                              │
                              │ FRONTEND PARSING
                              ▼
┌─────────────────────────────────────────────────────────────┐
│              REDUX STATE UPDATE                               │
│                                                               │
│  dispatch(setResults([                                        │
│    {candidateId, firstName, lastName, email, phone, ...},   │
│    {candidateId, firstName, lastName, email, phone, ...}    │
│  ]))                                                          │
│                                                               │
│  resultColumns = [                                            │
│    "candidateId", "firstName", "lastName", "email",          │
│    "phone", "skills", "experience", "matchScore"             │
│  ]                                                            │
│                                                               │
│  UI Re-renders with table                                     │
│                                                               │
└─────────────────────────────────────────────────────────────┘
```

## Table Rendering Logic

```
Results Array: [{firstName: "John", email: "john@example.com"}, ...]
       │
       └─► Column Extraction
           ├─► Take first object
           ├─► Get keys: ["firstName", "email"]
           ├─► Format: ["First Name", "Email"]
           └─► Set as resultColumns
                │
                ▼
       ┌─────────────────────────┐
       │  Table Header Rendering │
       │                         │
       │  for each column:       │
       │  ├─► Extract key       │
       │  ├─► Format name       │
       │  │   snake_case → "Snake Case"
       │  │   camelCase → "Camel Case"
       │  └─► Render <TableCell>
       │                         │
       └─────────────────────────┘
                │
                ▼
       ┌─────────────────────────┐
       │  Table Body Rendering   │
       │                         │
       │  for each row:          │
       │  ├─► Get candidate      │
       │  └─► for each column:   │
       │      ├─► Get value      │
       │      ├─► Truncate to 100 chars
       │      ├─► Handle null    │
       │      └─► Render <TableCell>
       │                         │
       └─────────────────────────┘
```

## User Interaction Flowchart

```
                          START
                            │
                            ▼
                    ┌──────────────────┐
                    │  View Empty Form  │
                    │ (Info Cards Show) │
                    └──────────────────┘
                            │
         ┌──────────────────┼──────────────────┐
         │                  │                  │
         ▼                  ▼                  ▼
    Enter Text         Upload PDF         Do Both
         │                  │                  │
         │                  │                  │
         └──────────────────┼──────────────────┘
                            │
                            ▼
                ┌───────────────────────────┐
                │ Click "Search Candidates" │
                │ (Enable when has input)   │
                └───────────────────────────┘
                            │
                            ▼
                    ┌──────────────────┐
                    │  Loading State   │
                    │ "Searching       │
                    │  Profiles..."    │
                    │ [Progress Bar]   │
                    └──────────────────┘
                            │
         ┌──────────────────┴──────────────────┐
         │                                      │
         ▼                                      ▼
    ┌─────────────┐                  ┌──────────────────────┐
    │  No Results │                  │ Candidates Found ✅  │
    │             │                  │ ┌──────────────────┐ │
    │ Error Alert │                  │ │ Results Table    │ │
    │ "No         │                  │ │ with candidates  │ │
    │  candidate  │                  │ │ Data:            │ │
    │  profiles   │                  │ │ • Column headers │ │
    │  found..."  │                  │ │ • Row data       │ │
    │             │                  │ │ • Dynamic cols   │ │
    │ [New Search]│                  │ └──────────────────┘ │
    │             │                  │ [New Search]        │
    │             │                  │ [Export Results]    │
    └─────────────┘                  └──────────────────────┘
         │                                      │
         │         ┌─────────────────────────────┘
         │         │
         │         ▼
         │    ┌───────────┐
         │    │ New Search│
         │    │ Button    │
         │    └───────────┘
         │         │
         └─────────┴──────────────────┐
                                       │
                                       ▼
                            Form Resets
                            State Cleared
                            Back to Input
```

---

This comprehensive diagram set shows:
✅ System architecture (Frontend → Backend → Zoho ATS)
✅ Data flow from user action to UI update
✅ Component hierarchy and structure
✅ Redux state management flow
✅ Request/Response formats
✅ Table rendering logic
✅ Complete user interaction flow
