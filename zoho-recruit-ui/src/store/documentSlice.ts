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

export const { setText, setPdfFile, setLoading, setError, setSuccess, setCandidates, resetForm } = documentSlice.actions;
export default documentSlice.reducer;
