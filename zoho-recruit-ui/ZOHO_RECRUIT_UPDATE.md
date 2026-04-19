# 🔍 Zoho Recruit Sourcing Tool - UI Update

## Overview
The Document Upload Page has been transformed into a **Zoho Recruit Sourcing Tool** that searches Wissen's Zoho Recruit ATS for the best candidate profiles given a Job Description.

## Key Updates

### 1. **Redux Store Enhancement** (`src/store/documentSlice.ts`)
- Added `results: Record<string, any>[]` - stores candidate search results
- Added `resultColumns: string[]` - dynamically determined from API response
- New action: `setResults(payload)` - automatically extracts column names from first result
- Updated `resetForm()` to clear results on new search

### 2. **Component Rebranding** (`src/pages/DocumentUploadPage.tsx`)

#### UI Title & Description
- **Before:** "Document Processing Hub"
- **After:** "🔍 Zoho Recruit Sourcing Tool"
- **Subtitle:** "Search Wissen's Zoho Recruit ATS for the best profiles given a Job Description"

#### Input Labels
- **Text Section:** "📋 Paste Job Description" instead of "📝 Paste Text"
- **PDF Section:** "📄 Upload Job Description (Optional)" instead of "📄 Upload PDF (Optional)"
- **Placeholder:** Updated to mention skills, experience, qualifications, and requirements

#### Action Button
- **Before:** "Process Document" with `SendIcon`
- **After:** "Search Candidates" with `SearchIcon`
- **Loading State:** "Searching Profiles..." instead of "Processing..."

#### Info Cards (When No Input)
Changed to reflect sourcing features:
- 🔍 Advanced Search - AI-powered candidate matching
- 🎯 Smart Matching - Find the best fits for your role
- ⚡ Instant Results - Get candidates in seconds

### 3. **Results Table Display**

#### Dynamic Column Generation
- Automatically extracts columns from the API response
- Column headers are formatted (e.g., `first_name` → `First Name`)
- Shows as many columns as the API returns

#### Table Features
- **Styled Header:** Gradient background with primary color
- **Hover Effects:** Rows highlight on hover for better UX
- **Responsive:** Adapts to different screen sizes
- **Data Truncation:** Long values are truncated to 100 characters
- **Success Indicator:** Shows count of matching candidates with chip

#### Table Components
```
Results Header:
👥 Found {count} Matching Candidate(s)
[{count} profiles] (chip)

Table:
- StyledTableHead with gradient background
- StyledTableRow with hover effects
- Automatic formatting of column names
- Data cells with text truncation
```

### 4. **API Integration Enhancement**

#### Smart Payload Handling
```
If ONLY text is provided:
  - Sends as JSON: { message: text }
  - Content-Type: application/json

If PDF is provided (with or optional text):
  - Sends as FormData with multipart/form-data
  - Appends: message (optional), pdf (file)
```

#### Response Handling
- Accepts both array responses and nested structures
  - Direct array: `[{candidate1}, {candidate2}]`
  - Nested in `results`: `{results: [{...}, {...}]}`
  - Nested in `candidates`: `{candidates: [{...}, {...}]}`
- Automatically extracts column names from first result
- Displays "No candidates found" if empty response

### 5. **Success Flow**
1. User provides Job Description (text or PDF)
2. Click "Search Candidates"
3. Loading state shows "Searching Profiles..."
4. API returns candidate results
5. Results table displays with all candidate data
6. User can:
   - View candidate profiles in the table
   - Click "New Search" to search again
   - Click "Export Results" (future feature)

### 6. **New Styled Components**

#### ResultsTableContainer
- Glassmorphic design with backdrop blur
- Responsive padding and styling
- Dark mode support

#### StyledTableHead
- Gradient background using primary color
- Bold headers with primary color text
- Clear visual separation with borders

#### StyledTableRow
- Smooth hover transitions
- Background color change on hover
- Proper cell padding

## File Changes

### Modified Files
1. `src/store/documentSlice.ts`
   - Enhanced DocumentState interface
   - Added results-related reducers
   - Updated exports

2. `src/pages/DocumentUploadPage.tsx`
   - Updated all imports (Table, TableBody, TableCell, etc.)
   - Added SearchIcon, removed SendIcon
   - New styled components for tables
   - Updated UI text for sourcing context
   - Added results table rendering logic
   - Enhanced handleSubmit for result capture
   - Conditional rendering for info cards vs. results

## API Endpoint
- **Base URL:** `http://localhost:8081/api/documents/process`
- **Method:** POST
- **Accepts:** 
  - JSON payload: `{ message: string }`
  - FormData: `message` (optional), `pdf` (file)

## Success Criteria
✅ UI rebranded for Zoho Recruit Sourcing
✅ Dynamic results table based on API response
✅ Automatic column extraction from candidate data
✅ Smart payload handling (JSON vs FormData)
✅ Responsive design maintained
✅ Dark mode support preserved
✅ TypeScript: 0 errors
✅ Build: Successful

## Next Steps
- Test with actual Zoho Recruit API
- Implement "Export Results" functionality
- Add pagination for large result sets
- Add column sorting/filtering
- Add candidate profile detail view modal
- Add search history/saved searches
