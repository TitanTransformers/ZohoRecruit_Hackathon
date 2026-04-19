# Code Reference - Refactored Implementation

## Complete Code Examples

This document provides complete, working code examples for the refactored implementation.

## 1. Environment Configuration

### `.env.example` / `.env`
```bash
# API Configuration
VITE_API_BASE_URL=http://localhost:8081
VITE_API_CHAT_ENDPOINT=/api/chat/send
VITE_API_DOCUMENTS_ENDPOINT=/api/documents/process

# Feature Flags
VITE_ENABLE_DEBUG_MODE=false

# Application Settings
VITE_APP_NAME=Zoho Recruit Sourcing Tool
VITE_MAX_FILE_SIZE=10485760
VITE_MAX_TEXT_LENGTH=10000
```

### `src/config/environment.ts` - Full Implementation
```typescript
/**
 * Environment Configuration
 * Centralized configuration management for the application
 */

interface EnvironmentConfig {
  apiBaseUrl: string;
  apiChatEndpoint: string;
  apiDocumentsEndpoint: string;
  enableDebugMode: boolean;
  appName: string;
  maxFileSize: number;
  maxTextLength: number;
}

const getEnvironmentVariable = (key: string, defaultValue: string = ''): string => {
  return import.meta.env[`VITE_${key}`] || defaultValue;
};

const getEnvironmentNumber = (key: string, defaultValue: number = 0): number => {
  const value = import.meta.env[`VITE_${key}`];
  return value ? parseInt(value, 10) : defaultValue;
};

const getEnvironmentBoolean = (key: string, defaultValue: boolean = false): boolean => {
  const value = import.meta.env[`VITE_${key}`];
  return value ? value === 'true' : defaultValue;
};

const envConfig: EnvironmentConfig = {
  apiBaseUrl: getEnvironmentVariable('API_BASE_URL', 'http://localhost:8081'),
  apiChatEndpoint: getEnvironmentVariable('API_CHAT_ENDPOINT', '/api/chat/send'),
  apiDocumentsEndpoint: getEnvironmentVariable('API_DOCUMENTS_ENDPOINT', '/api/documents/process'),
  enableDebugMode: getEnvironmentBoolean('ENABLE_DEBUG_MODE', false),
  appName: getEnvironmentVariable('APP_NAME', 'Zoho Recruit Sourcing Tool'),
  maxFileSize: getEnvironmentNumber('MAX_FILE_SIZE', 10485760),
  maxTextLength: getEnvironmentNumber('MAX_TEXT_LENGTH', 10000),
};

export const config = Object.freeze(envConfig);

export const getApiUrl = (endpoint: string): string => {
  return `${envConfig.apiBaseUrl}${endpoint}`;
};

export const debugLog = (message: string, data?: unknown): void => {
  if (envConfig.enableDebugMode) {
    console.log(`[${envConfig.appName}]`, message, data);
  }
};
```

## 2. Type Definitions

### `src/types/candidate.ts` - Full Implementation
```typescript
/**
 * Candidate Response Type
 * Represents the API response structure for a candidate profile
 */
export interface CandidateProfile {
  name: string;
  email: string;
  matchedSkill: string[];
  missingSkills: string[];
  analysis: string;
  matchedPercentage: number;
}

/**
 * Search Request Type
 * Represents the payload sent to the API
 */
export interface SearchRequest {
  message?: string;
  pdf?: File;
}

/**
 * API Response Type
 * Generic wrapper for API responses
 */
export interface ApiResponse<T> {
  data?: T;
  results?: T;
  candidates?: T;
  error?: string;
  message?: string;
  status?: number;
}
```

## 3. API Service Layer

### `src/services/candidateService.ts` - Full Implementation
```typescript
/**
 * Candidate Service
 * Handles all API calls related to candidate search and profile retrieval
 */

import { CandidateProfile, ApiResponse } from '../types/candidate';
import { config, getApiUrl, debugLog } from '../config/environment';

class CandidateService {
  /**
   * Search candidates using text (Job Description)
   * @param message - Job description text
   * @returns Promise with array of candidate profiles
   */
  async searchByText(message: string): Promise<CandidateProfile[]> {
    if (!message.trim()) {
      throw new Error('Job description cannot be empty');
    }

    debugLog('Searching candidates by text', { messageLength: message.length });

    try {
      const url = getApiUrl(config.apiChatEndpoint);
      const response = await fetch(url, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ message }),
      });

      return this.handleResponse(response);
    } catch (error) {
      this.handleError(error);
    }
  }

  /**
   * Search candidates using document (PDF) with optional text
   * @param file - PDF file
   * @param message - Optional job description text
   * @returns Promise with array of candidate profiles
   */
  async searchByDocument(file: File, message?: string): Promise<CandidateProfile[]> {
    if (!file) {
      throw new Error('PDF file is required');
    }

    if (file.type !== 'application/pdf') {
      throw new Error('Only PDF files are supported');
    }

    debugLog('Searching candidates by document', { fileName: file.name, fileSize: file.size });

    try {
      const formData = new FormData();

      if (message?.trim()) {
        formData.append('message', message);
      }

      formData.append('pdf', file);

      const url = getApiUrl(config.apiDocumentsEndpoint);
      const response = await fetch(url, {
        method: 'POST',
        body: formData,
      });

      return this.handleResponse(response);
    } catch (error) {
      this.handleError(error);
    }
  }

  /**
   * Handle API response
   * @param response - Fetch response object
   * @returns Promise with parsed candidate profiles
   */
  private async handleResponse(response: Response): Promise<CandidateProfile[]> {
    if (!response.ok) {
      throw new Error(`API Error: ${response.status} ${response.statusText}`);
    }

    const data: ApiResponse<CandidateProfile[]> = await response.json();
    debugLog('API Response received', data);

    // Support multiple response formats
    const candidates = this.extractCandidates(data);

    if (!candidates || candidates.length === 0) {
      throw new Error('No candidate profiles found matching the job description');
    }

    return this.validateAndNormalizeCandidates(candidates);
  }

  /**
   * Extract candidates from various API response formats
   * @param data - API response data
   * @returns Array of candidates or null
   */
  private extractCandidates(data: ApiResponse<CandidateProfile[]>): CandidateProfile[] | null {
    // Check for direct array response
    if (Array.isArray(data)) {
      return data as CandidateProfile[];
    }

    // Check for nested array in various properties
    if (data.results && Array.isArray(data.results)) {
      return data.results;
    }

    if (data.candidates && Array.isArray(data.candidates)) {
      return data.candidates;
    }

    if (data.data && Array.isArray(data.data)) {
      return data.data;
    }

    return null;
  }

  /**
   * Validate and normalize candidate data
   * @param candidates - Array of candidate profiles
   * @returns Validated candidate profiles
   */
  private validateAndNormalizeCandidates(candidates: CandidateProfile[]): CandidateProfile[] {
    return candidates.map((candidate) => {
      return {
        name: candidate.name || 'N/A',
        email: candidate.email || 'N/A',
        matchedSkill: Array.isArray(candidate.matchedSkill) ? candidate.matchedSkill : [],
        missingSkills: Array.isArray(candidate.missingSkills) ? candidate.missingSkills : [],
        analysis: candidate.analysis || 'No analysis available',
        matchedPercentage: typeof candidate.matchedPercentage === 'number' 
          ? Math.min(100, Math.max(0, candidate.matchedPercentage))
          : 0,
      };
    });
  }

  /**
   * Handle API errors
   * @param error - Error object
   */
  private handleError(error: unknown): never {
    const errorMessage = error instanceof Error ? error.message : 'An unknown error occurred';
    debugLog('API Error', errorMessage);
    throw new Error(errorMessage);
  }
}

export const candidateService = new CandidateService();
```

## 4. Results Display Component

### `src/components/CandidateResultsTable.tsx` - Full Implementation
See the component file created in the workspace - it includes:
- Professional table styling
- Skills displayed as color-coded tags
- Match percentage with progress bars
- Email as clickable mailto link
- Analysis text with tooltips
- Responsive design

## 5. Redux State Management

### `src/store/documentSlice.ts` - Updated Implementation
```typescript
import { createSlice } from '@reduxjs/toolkit';
import type { PayloadAction } from '@reduxjs/toolkit';
import type { CandidateProfile } from '../types/candidate';

interface DocumentState {
  text: string;
  pdfFile: File | null;
  loading: boolean;
  error: string | null;
  success: boolean;
  candidates: CandidateProfile[];
}

const initialState: DocumentState = {
  text: '',
  pdfFile: null,
  loading: false,
  error: null,
  success: false,
  candidates: [],
};

export const documentSlice = createSlice({
  name: 'document',
  initialState,
  reducers: {
    setText: (state, action: PayloadAction<string>) => {
      state.text = action.payload;
    },
    setPdfFile: (state, action: PayloadAction<File | null>) => {
      state.pdfFile = action.payload;
    },
    setLoading: (state, action: PayloadAction<boolean>) => {
      state.loading = action.payload;
    },
    setError: (state, action: PayloadAction<string | null>) => {
      state.error = action.payload;
    },
    setSuccess: (state, action: PayloadAction<boolean>) => {
      state.success = action.payload;
    },
    setCandidates: (state, action: PayloadAction<CandidateProfile[]>) => {
      state.candidates = action.payload;
    },
    resetForm: (state) => {
      state.text = '';
      state.pdfFile = null;
      state.error = null;
      state.success = false;
      state.candidates = [];
    },
  },
});

export const { 
  setText, 
  setPdfFile, 
  setLoading, 
  setError, 
  setSuccess, 
  setCandidates, 
  resetForm 
} = documentSlice.actions;

export default documentSlice.reducer;
```

## 6. Using the Service in Component

### Example Usage in DocumentUploadPage.tsx
```typescript
import { candidateService } from '../services/candidateService';
import { config } from '../config/environment';
import { setCandidates } from '../store/documentSlice';

// Handling search
const handleSubmit = async () => {
  if (!text && !pdfFile) {
    dispatch(setError('Please provide either a Job Description or upload a PDF'));
    return;
  }

  dispatch(setLoading(true));
  dispatch(setError(null));

  try {
    let candidateResults;

    if (text && !pdfFile) {
      // Only text provided
      candidateResults = await candidateService.searchByText(text);
    } else if (pdfFile) {
      // PDF provided (with or without text)
      candidateResults = await candidateService.searchByDocument(pdfFile, text);
    } else {
      throw new Error('No input provided');
    }

    dispatch(setCandidates(candidateResults));
    dispatch(setSuccess(true));
  } catch (err) {
    const errorMessage = err instanceof Error ? err.message : 'An error occurred';
    dispatch(setError(errorMessage));
  } finally {
    dispatch(setLoading(false));
  }
};
```

## 7. Using Environment Config in Component

```typescript
import { config } from '../config/environment';

// In component render:
<Typography>
  🔍 {config.appName}
</Typography>

// For text field max length:
<StyledTextField
  slotProps={{
    htmlInput: {
      maxLength: config.maxTextLength,
    },
  }}
/>

// For character counter:
<Typography>
  {config.maxTextLength - textCharCount} characters remaining
</Typography>
```

## 8. Using Results Component

```typescript
import CandidateResultsTable from '../components/CandidateResultsTable';
import { useSelector } from 'react-redux';

const DocumentUploadPage = () => {
  const { candidates, loading } = useSelector((state) => state.document);

  return (
    <>
      {candidates.length > 0 && (
        <CandidateResultsTable 
          candidates={candidates} 
          loading={loading} 
        />
      )}
    </>
  );
};
```

## 9. API Response Parsing Examples

The service handles these response formats:

### Format 1: Direct Array
```json
[
  {
    "name": "John Doe",
    "email": "john@example.com",
    "matchedSkill": ["JavaScript", "React"],
    "missingSkills": ["Python"],
    "analysis": "Strong candidate",
    "matchedPercentage": 85.5
  }
]
```

### Format 2: Nested in data
```json
{
  "data": [
    {
      "name": "John Doe",
      ...
    }
  ]
}
```

### Format 3: Nested in results
```json
{
  "results": [
    {
      "name": "John Doe",
      ...
    }
  ]
}
```

### Format 4: Nested in candidates
```json
{
  "candidates": [
    {
      "name": "John Doe",
      ...
    }
  ]
}
```

## 10. Error Handling Examples

```typescript
// The service automatically handles:

// 1. Invalid input
try {
  await candidateService.searchByText('');
} catch (err) {
  // Error: "Job description cannot be empty"
}

// 2. Invalid file type
try {
  await candidateService.searchByDocument(txtFile);
} catch (err) {
  // Error: "Only PDF files are supported"
}

// 3. API errors
try {
  await candidateService.searchByText(description);
} catch (err) {
  // Error: "API Error: 500 Internal Server Error"
}

// 4. No results found
try {
  await candidateService.searchByText(description);
} catch (err) {
  // Error: "No candidate profiles found matching the job description"
}
```

## 11. Environment Configuration Examples

### Development Environment
```bash
# .env for local development
VITE_API_BASE_URL=http://localhost:8081
VITE_ENABLE_DEBUG_MODE=true
```

### Staging Environment
```bash
# .env for staging
VITE_API_BASE_URL=https://staging-api.example.com
VITE_ENABLE_DEBUG_MODE=false
```

### Production Environment
```bash
# .env for production (via build process)
VITE_API_BASE_URL=https://api.example.com
VITE_ENABLE_DEBUG_MODE=false
```

## 12. Testing Examples

### Test candidateService
```typescript
// Unit test example
describe('candidateService', () => {
  it('should search candidates by text', async () => {
    const mockCandidates = [{
      name: 'John Doe',
      email: 'john@example.com',
      matchedSkill: ['JavaScript'],
      missingSkills: ['Python'],
      analysis: 'Good fit',
      matchedPercentage: 85
    }];

    global.fetch = jest.fn(() =>
      Promise.resolve({
        ok: true,
        json: () => Promise.resolve({ data: mockCandidates })
      })
    );

    const result = await candidateService.searchByText('JavaScript Developer');
    expect(result).toEqual(mockCandidates);
  });
});
```

## 13. Extending the Service

### Add a new search method
```typescript
// In candidateService.ts
async searchBySkills(skills: string[]): Promise<CandidateProfile[]> {
  debugLog('Searching candidates by skills', { skills });

  try {
    const url = getApiUrl('/api/candidates/search');
    const response = await fetch(url, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ skills }),
    });

    return this.handleResponse(response);
  } catch (error) {
    this.handleError(error);
  }
}
```

### Add a new environment variable
```bash
# In .env
VITE_API_SKILLS_ENDPOINT=/api/candidates/search
```

```typescript
// In environment.ts
apiSkillsEndpoint: getEnvironmentVariable('API_SKILLS_ENDPOINT', '/api/candidates/search'),
```

## 14. Performance Optimization

### Memoize the component
```typescript
import React, { memo } from 'react';

const CandidateResultsTable = memo(({ candidates, loading }) => {
  // Component implementation
});

export default CandidateResultsTable;
```

### Cache service results
```typescript
class CandidateService {
  private cache = new Map<string, CandidateProfile[]>();

  async searchByText(message: string): Promise<CandidateProfile[]> {
    const cacheKey = `text:${message}`;
    
    if (this.cache.has(cacheKey)) {
      return this.cache.get(cacheKey)!;
    }

    const results = await /* ... API call ... */;
    this.cache.set(cacheKey, results);
    return results;
  }
}
```

## Complete Integration Example

```typescript
// src/pages/DocumentUploadPage.tsx - Complete flow
import { useDispatch, useSelector } from 'react-redux';
import { candidateService } from '../services/candidateService';
import { config } from '../config/environment';
import CandidateResultsTable from '../components/CandidateResultsTable';
import { 
  setCandidates, 
  setLoading, 
  setError, 
  setSuccess 
} from '../store/documentSlice';

const DocumentUploadPage = () => {
  const dispatch = useDispatch();
  const { text, pdfFile, loading, candidates, error, success } = 
    useSelector(state => state.document);

  const handleSearch = async () => {
    dispatch(setLoading(true));
    dispatch(setError(null));

    try {
      const results = text && !pdfFile
        ? await candidateService.searchByText(text)
        : await candidateService.searchByDocument(pdfFile!, text);

      dispatch(setCandidates(results));
      dispatch(setSuccess(true));
    } catch (err) {
      dispatch(setError(err instanceof Error ? err.message : 'Error'));
    } finally {
      dispatch(setLoading(false));
    }
  };

  return (
    <div>
      <h1>{config.appName}</h1>
      {/* Form inputs */}
      <button onClick={handleSearch}>Search</button>
      
      {/* Results */}
      {candidates.length > 0 && (
        <CandidateResultsTable 
          candidates={candidates} 
          loading={loading} 
        />
      )}

      {/* Error */}
      {error && <Alert>{error}</Alert>}
    </div>
  );
};

export default DocumentUploadPage;
```

## Conclusion

This refactored implementation provides:
- ✅ Clean, modular architecture
- ✅ Type-safe code with TypeScript
- ✅ Proper separation of concerns
- ✅ Reusable components and services
- ✅ Comprehensive error handling
- ✅ Easy to extend and maintain
- ✅ Production-ready code

All code follows best practices and industry standards.
