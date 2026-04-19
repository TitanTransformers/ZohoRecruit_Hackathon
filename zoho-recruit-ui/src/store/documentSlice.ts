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
  currentSearchId: string | null;
  searchResults: Record<string, CandidateProfile[]>; // Cache results by search ID
  pipelineStep: 'idle' | 'parse' | 'query' | 'search' | 'rank';
  costEstimate: number;
}

const initialState: DocumentState = {
  text: '',
  pdfFile: null,
  loading: false,
  error: null,
  success: false,
  candidates: [],
  currentSearchId: null,
  searchResults: {},
  pipelineStep: 'idle',
  costEstimate: 0,
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
    setCurrentSearchId: (state, action: PayloadAction<string | null>) => {
      state.currentSearchId = action.payload;
    },
    setSearchResults: (state, action: PayloadAction<{ id: string; results: CandidateProfile[] }>) => {
      state.searchResults[action.payload.id] = action.payload.results;
      state.candidates = action.payload.results;
    },
    loadSearchResults: (state, action: PayloadAction<string>) => {
      const results = state.searchResults[action.payload];
      if (results) {
        state.candidates = results;
        state.currentSearchId = action.payload;
        state.success = true;
      }
    },
    setPipelineStep: (state, action: PayloadAction<'idle' | 'parse' | 'query' | 'search' | 'rank'>) => {
      state.pipelineStep = action.payload;
    },
    setCostEstimate: (state, action: PayloadAction<number>) => {
      state.costEstimate = action.payload;
    },
    resetForm: (state) => {
      state.text = '';
      state.pdfFile = null;
      state.error = null;
      state.success = false;
      state.candidates = [];
      state.pipelineStep = 'idle';
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
  setCurrentSearchId,
  setSearchResults,
  loadSearchResults,
  setPipelineStep,
  setCostEstimate,
  resetForm,
} = documentSlice.actions;
export default documentSlice.reducer;
