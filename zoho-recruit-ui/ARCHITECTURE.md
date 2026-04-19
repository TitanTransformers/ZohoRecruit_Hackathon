# Visual Architecture & Flow Guide

## 🏗️ Application Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                     BROWSER                                     │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │          React TypeScript Application                   │  │
│  │              (Vite Dev Server)                          │  │
│  │          http://localhost:5173                          │  │
│  ├──────────────────────────────────────────────────────────┤  │
│  │                                                          │  │
│  │  ┌────────────────────────────────────────────────────┐ │  │
│  │  │           App.tsx (Router)                         │ │  │
│  │  └──────────────────┬───────────────────────────────┘ │  │
│  │                     │                                  │  │
│  │  ┌──────────────────▼───────────────────────────────┐ │  │
│  │  │     DocumentUploadPage Component                 │ │  │
│  │  ├───────────────────────────────────────────────────┤ │  │
│  │  │                                                   │ │  │
│  │  │  ┌──────────────────────────────────────────┐    │ │  │
│  │  │  │  Text Input Area (Textarea)             │    │ │  │
│  │  │  │  - Multiline text editor                │    │ │  │
│  │  │  │  - Real-time validation                 │    │ │  │
│  │  │  │  - Disabled during submission           │    │ │  │
│  │  │  └──────────────────────────────────────────┘    │ │  │
│  │  │                                                   │ │  │
│  │  │  ┌──────────────────────────────────────────┐    │ │  │
│  │  │  │  PDF Upload Area (Paper with input)    │    │ │  │
│  │  │  │  - Drag and drop support               │    │ │  │
│  │  │  │  - Click to upload                     │    │ │  │
│  │  │  │  - File type validation                │    │ │  │
│  │  │  │  - Selected file preview               │    │ │  │
│  │  │  └──────────────────────────────────────────┘    │ │  │
│  │  │                                                   │ │  │
│  │  │  ┌──────────────────────────────────────────┐    │ │  │
│  │  │  │  Alert Messages                         │    │ │  │
│  │  │  │  - Error (red)                          │    │ │  │
│  │  │  │  - Success (green)                      │    │ │  │
│  │  │  │  - Loading (spinner)                    │    │ │  │
│  │  │  └──────────────────────────────────────────┘    │ │  │
│  │  │                                                   │ │  │
│  │  │  ┌──────────────────────────────────────────┐    │ │  │
│  │  │  │  Action Buttons                         │    │ │  │
│  │  │  │  - Submit (Blue) - Disabled if empty    │    │ │  │
│  │  │  │  - Reset (Outlined) - Clear form        │    │ │  │
│  │  │  └──────────────────────────────────────────┘    │ │  │
│  │  │                                                   │ │  │
│  │  └───────────────────┬───────────────────────────────┘ │  │
│  │                      │                                  │  │
│  │  ┌──────────────────▼───────────────────────────────┐ │  │
│  │  │       Redux Store (documentSlice)               │ │  │
│  │  ├───────────────────────────────────────────────────┤ │  │
│  │  │                                                   │ │  │
│  │  │  State:                                          │ │  │
│  │  │  - text: string                                 │ │  │
│  │  │  - pdfFile: File | null                         │ │  │
│  │  │  - loading: boolean                             │ │  │
│  │  │  - error: string | null                         │ │  │
│  │  │  - success: boolean                             │ │  │
│  │  │                                                   │ │  │
│  │  │  Actions:                                        │ │  │
│  │  │  - setText()                                    │ │  │
│  │  │  - setPdfFile()                                 │ │  │
│  │  │  - setLoading()                                 │ │  │
│  │  │  - setError()                                   │ │  │
│  │  │  - setSuccess()                                 │ │  │
│  │  │  - resetForm()                                  │ │  │
│  │  │                                                   │ │  │
│  │  └──────────────────┬───────────────────────────────┘ │  │
│  │                     │                                  │  │
│  └─────────────────────┼──────────────────────────────────┘  │
│                        │                                      │
└────────────────────────┼──────────────────────────────────────┘
                         │
                         │ HTTP POST (Fetch API)
                         │ Content-Type: multipart/form-data
                         │
         ┌───────────────▼──────────────────┐
         │  Network (Internet)              │
         │  http://localhost:8080           │
         └───────────────┬──────────────────┘
                         │
┌────────────────────────▼──────────────────────────────────────┐
│                    SPRING BOOT SERVER                         │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│  ┌──────────────────────────────────────────────────────┐   │
│  │    DocumentController                               │   │
│  │    POST /api/documents/process                      │   │
│  ├──────────────────────────────────────────────────────┤   │
│  │                                                      │   │
│  │  1. Receive multipart/form-data                    │   │
│  │  2. Validate input (text and/or pdf)              │   │
│  │  3. Process text (if provided)                     │   │
│  │  4. Validate PDF (if provided)                     │   │
│  │  5. Generate response JSON                         │   │
│  │  6. Return 200 OK with response                    │   │
│  │                                                      │   │
│  │  Response Structure:                                │   │
│  │  {                                                  │   │
│  │    "status": "success",                            │   │
│  │    "message": "...",                               │   │
│  │    "textProcessed": true/false,                    │   │
│  │    "pdfProcessed": true/false,                     │   │
│  │    "timestamp": 1234567890                         │   │
│  │  }                                                  │   │
│  │                                                      │   │
│  └──────────────────────┬───────────────────────────────┘   │
│                         │                                    │
│  ┌──────────────────────▼───────────────────────────────┐   │
│  │    CORS Configuration                               │   │
│  │    - Allows requests from any origin                │   │
│  │    - Supports multipart/form-data                   │   │
│  └──────────────────────────────────────────────────────┘   │
│                                                              │
└──────────────────────────────────────────────────────────────┘
```

---

## 📊 Data Flow Diagram

### User Interaction Flow

```
┌─────────────────────────────────────────────────────────────────┐
│                    USER INTERACTION                             │
└─────────────────────────────────────────────────────────────────┘

1. Load Application
   └─> Browser opens http://localhost:5173
       └─> React loads
           └─> Redux store initializes
               └─> DocumentUploadPage renders

2. User Inputs Text
   └─> onChange event
       └─> handleTextChange()
           └─> dispatch(setText())
               └─> Redux state updates
                   └─> Component re-renders

3. User Uploads PDF
   └─> onChange event (file input)
       └─> handleFileChange()
           └─> Validate file type
               └─> dispatch(setPdfFile())
                   └─> Redux state updates
                       └─> Component re-renders

4. User Clicks Submit
   └─> onClick event
       └─> handleSubmit()
           └─> Validate form (text or pdf)
               └─> dispatch(setLoading(true))
                   └─> Fetch POST request
                       ├─> FormData.append('text')
                       ├─> FormData.append('pdf')
                       └─> Send to /api/documents/process
                           │
                           ├─> Success Response
                           │   └─> dispatch(setSuccess(true))
                           │       └─> dispatch(resetForm())
                           │           └─> Show success message
                           │
                           └─> Error Response
                               └─> dispatch(setError())
                                   └─> Show error message
               └─> dispatch(setLoading(false))

5. User Clicks Reset
   └─> onClick event
       └─> handleReset()
           └─> dispatch(resetForm())
               └─> Clear all state
                   └─> Component re-renders
```

---

## 🔄 API Request/Response Flow

### Request Lifecycle

```
┌─────────────────────────────────────────────────────────────┐
│                   REQUEST PREPARATION                       │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  1. Validate Form State                                    │
│     - Check if text OR pdf exists                          │
│     - If empty, show error: "Please provide input"         │
│                                                             │
│  2. Create FormData Object                                 │
│     FormData {                                              │
│       text: "user input text...",                           │
│       pdf: File { name: "document.pdf", ... }              │
│     }                                                       │
│                                                             │
│  3. Set Loading State                                      │
│     - dispatch(setLoading(true))                           │
│     - UI shows spinner, buttons disabled                   │
│                                                             │
│  4. Clear Previous Errors                                  │
│     - dispatch(setError(null))                             │
│                                                             │
└────────────────┬────────────────────────────────────────────┘
                 │
         ┌───────▼─────────┐
         │  FETCH REQUEST  │
         ├─────────────────┤
         │ Method: POST    │
         │ URL:            │
         │ http://loc...   │
         │ :8080/api/...   │
         │ Body: FormData  │
         └───────┬─────────┘
                 │
       ┌─────────▼──────────┐
       │   NETWORK SEND     │
       │  Multipart Upload  │
       └─────────┬──────────┘
                 │
┌────────────────▼──────────────────────────────────────────┐
│              BACKEND PROCESSING                           │
├───────────────────────────────────────────────────────────┤
│                                                           │
│  Spring Boot receives request                            │
│  ├─> @PostMapping validates multipart                    │
│  ├─> Extract text and pdf parameters                     │
│  ├─> Validate inputs                                     │
│  ├─> Process text (if provided)                          │
│  ├─> Validate PDF (if provided)                          │
│  └─> Create JSON response                                │
│                                                           │
└────────────────┬──────────────────────────────────────────┘
                 │
    ┌────────────▼──────────────┐
    │   SEND RESPONSE           │
    │   Status: 200 OK          │
    │   Content-Type: JSON      │
    └────────────┬──────────────┘
                 │
┌────────────────▼──────────────────────────────────────────┐
│           RESPONSE HANDLING                               │
├───────────────────────────────────────────────────────────┤
│                                                           │
│  1. Check Response Status                                │
│     ├─> if (!response.ok) throw Error                    │
│     └─> Parse JSON                                       │
│                                                           │
│  2. Success Path                                         │
│     ├─> dispatch(setSuccess(true))                       │
│     ├─> Show "Document processed successfully!"          │
│     ├─> dispatch(resetForm())                            │
│     ├─> Clear all inputs                                 │
│     └─> setTimeout(() => hide success) 3000ms            │
│                                                           │
│  3. Error Path                                           │
│     ├─> Catch error                                      │
│     ├─> dispatch(setError(message))                      │
│     └─> Show error to user                               │
│                                                           │
│  4. Cleanup                                              │
│     └─> dispatch(setLoading(false))                      │
│         └─> Re-enable buttons                            │
│                                                           │
└───────────────────────────────────────────────────────────┘
```

---

## 🎨 UI Component Hierarchy

```
App
├── Provider (Redux)
├── CssBaseline (MUI Reset)
├── BrowserRouter
└── Box (Layout)
    └── Routes
        └── Route (/)
            └── DocumentUploadPage
                ├── Container
                └── Card
                    └── CardContent
                        └── Stack (vertical)
                            ├── Typography (h3)
                            │
                            ├── Box (Text Input)
                            │   ├── Typography (h6)
                            │   └── TextField (multiline)
                            │
                            ├── Divider ("OR")
                            │
                            ├── Box (PDF Upload)
                            │   ├── Typography (h6)
                            │   ├── Paper
                            │   │   ├── Input (file)
                            │   │   └── Label
                            │   │       ├── CloudUploadIcon
                            │   │       ├── Typography
                            │   │       └── Typography (caption)
                            │   ├── Typography (selected file)
                            │   └── Typography (error)
                            │
                            ├── Alert (error)
                            ├── Alert (success)
                            │
                            └── Stack (horizontal - buttons)
                                ├── Button (Submit)
                                │   └── CircularProgress (loading)
                                └── Button (Reset)
```

---

## 💾 Redux State Tree

```
Store
└── document (documentSlice)
    ├── text: "" | "user text..."
    ├── pdfFile: null | File
    ├── loading: false | true
    ├── error: null | "error message"
    └── success: false | true

Actions:
├── setText(string)
├── setPdfFile(File | null)
├── setLoading(boolean)
├── setError(string | null)
├── setSuccess(boolean)
└── resetForm()
```

---

## 🔗 Component Props Flow

```
DocumentUploadPage
├── useDispatch() → AppDispatch
├── useSelector(RootState) → document state
│   ├── text
│   ├── pdfFile
│   ├── loading
│   ├── error
│   └── success
│
└── Local State
    └── fileError: null | string

Event Handlers:
├── handleTextChange(event)
│   └── dispatch(setText())
│
├── handleFileChange(event)
│   ├── Validate file type
│   └── dispatch(setPdfFile())
│
├── handleSubmit()
│   ├── Validate form
│   ├── dispatch(setLoading(true))
│   ├── Fetch POST request
│   ├── Handle response
│   └── dispatch(setLoading(false))
│
└── handleReset()
    └── dispatch(resetForm())
```

---

## 📱 Responsive Breakpoints

```
MUI Breakpoints:
├── xs: 0px
├── sm: 600px
├── md: 960px
├── lg: 1280px
└── xl: 1920px

Container maxWidth="md"
└── Max width: 960px
    └── Centers content
    └── Responsive padding

TextField & Button
└── fullWidth
    └── Responsive to container

Stack spacing
└── Scales with viewport
```

---

## 🎯 Error Handling Flow

```
Error Sources:
├── Form Validation
│   ├── No text AND no PDF
│   ├── Invalid PDF type
│   └── File too large
│
├── Network Errors
│   ├── Connection timeout
│   ├── Server unreachable
│   └── CORS blocked
│
└── API Errors
    ├── 400 Bad Request
    ├── 500 Internal Server Error
    └── Custom validation errors

Error Handling:
1. Try-Catch Block
2. Dispatch setError(message)
3. Display Alert component
4. User can retry
5. Clear error on new attempt
```

---

## 🚀 Deployment Flow

```
Development
    ↓
npm run build
    ↓
Optimization (TypeScript, Minification)
    ↓
Build Output (dist/)
    ↓
Production Server
    ├── Serve dist/index.html
    ├── Serve dist/assets/
    ├── Configure routing
    └── Enable compression

Environment Config:
├── Development: http://localhost:5173
├── Staging: https://staging.zoho-recruit.com
└── Production: https://zoho-recruit.com
```

---

## 📊 File Size Breakdown

```
Production Build (dist/)
├── index.html (0.46 KB)
│   └── Entry point
│
├── assets/index-D64VDMd1.css (4.10 KB)
│   ├── MUI styles
│   ├── Emotion CSS-in-JS
│   └── App styles
│
└── assets/index-CjgKLZfs.js (462.92 KB)
    ├── React (core)
    ├── React Router
    ├── Redux
    ├── Material-UI
    ├── Application code
    └── Dependencies

Gzip Compression: ~147 KB (32% of original)
```

---

This visual guide helps understand how all components work together!
