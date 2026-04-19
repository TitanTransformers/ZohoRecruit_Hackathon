import React, { useState, useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import type { RootState, AppDispatch } from '../store/store';
import {
  setText,
  setPdfFile,
  setLoading,
  setError,
  setSuccess,
  setCurrentSearchId,
  setSearchResults,
  loadSearchResults,
  setPipelineStep,
  setCostEstimate,
  resetForm,
} from '../store/documentSlice';
import { candidateService } from '../services/candidateService';
import { searchHistoryService, type SearchHistoryItem } from '../services/searchHistoryService';
import { config } from '../config/environment';
import ModernNavbar from '../components/ModernNavbar';
import ModernInputPanel from '../components/ModernInputPanel';
import PipelineStepper from '../components/PipelineStepper';
import ModernCandidateResults from '../components/ModernCandidateResults';

const DocumentUploadPage: React.FC = () => {
  const dispatch = useDispatch<AppDispatch>();
  const {
    text,
    pdfFile,
    loading,
    error,
    success,
    candidates,
    pipelineStep,
    costEstimate,
    searchResults,
  } = useSelector((state: RootState) => state.document);

  const [history, setHistory] = useState<SearchHistoryItem[]>([]);

  // Load history on mount
  useEffect(() => {
    setHistory(searchHistoryService.getHistory());
  }, []);

  const handleTextChange = (value: string) => {
    dispatch(setText(value));
  };

  const handleFileChange = (file: File | null) => {
    dispatch(setPdfFile(file));
  };

  const handleSubmit = async () => {
    if (!text && !pdfFile) {
      dispatch(setError('Please provide either a Job Description or upload a PDF'));
      return;
    }

    dispatch(setLoading(true));
    dispatch(setError(null));
    dispatch(setPipelineStep('parse'));

    try {
      let candidateResults;
      const query = text || `PDF: ${pdfFile?.name}`;
      const searchId = Date.now().toString();

      // Simulate pipeline steps
      setTimeout(() => dispatch(setPipelineStep('query')), 1000);
      setTimeout(() => dispatch(setPipelineStep('search')), 2000);

      if (text && !pdfFile) {
        candidateResults = await candidateService.searchByText(text);
      } else if (pdfFile) {
        candidateResults = await candidateService.searchByDocument(pdfFile, text);
      } else {
        throw new Error('No input provided');
      }

      // Save results
      dispatch(setSearchResults({ id: searchId, results: candidateResults }));
      dispatch(setCurrentSearchId(searchId));

      // Add to history with results
      const historyItem = searchHistoryService.addToHistory({
        query,
        type: pdfFile ? 'pdf' : 'text',
        fileName: pdfFile?.name,
        candidatesCount: candidateResults.length,
      });

      // Store results associated with history item
      localStorage.setItem(`results_${historyItem.id}`, searchId);

      setHistory(searchHistoryService.getHistory());
      dispatch(setPipelineStep('rank'));
      dispatch(setSuccess(true));

      // Simulate cost estimate
      dispatch(setCostEstimate(Math.random() * 0.5 + 0.01));

      // Reset pipeline after brief delay
      setTimeout(() => dispatch(setPipelineStep('idle')), 3000);
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : 'An error occurred';
      dispatch(setError(errorMessage));
      dispatch(setPipelineStep('idle'));
    } finally {
      dispatch(setLoading(false));
    }
  };

  const handleSelectHistory = (item: SearchHistoryItem) => {
    // Try to load results for this history item
    const resultsId = localStorage.getItem(`results_${item.id}`);
    if (resultsId && searchResults[resultsId]) {
      dispatch(loadSearchResults(resultsId));
      dispatch(setSuccess(true));
    } else if (item.type === 'text') {
      dispatch(setText(item.query));
      dispatch(setPdfFile(null));
    } else {
      dispatch(setText(''));
    }
  };

  const handleDeleteHistory = (id: string) => {
    searchHistoryService.deleteFromHistory(id);
    setHistory(searchHistoryService.getHistory());
    localStorage.removeItem(`results_${id}`);
  };

  const handleClearHistory = () => {
    searchHistoryService.clearHistory();
    setHistory([]);
    // Clear all stored results
    const allKeys = Object.keys(localStorage);
    allKeys.forEach((key) => {
      if (key.startsWith('results_')) {
        localStorage.removeItem(key);
      }
    });
  };

  const handleReset = () => {
    dispatch(resetForm());
    dispatch(setPipelineStep('idle'));
  };

  return (
    <div className="min-h-screen bg-dark-bg">
      {/* Navbar */}
      <ModernNavbar
        costEstimate={costEstimate}
        historyItems={history}
        onHistorySelect={handleSelectHistory}
        onHistoryDelete={handleDeleteHistory}
        onHistoryClear={handleClearHistory}
      />

      {/* Main Content */}
      <main className="pt-24 pb-8 px-0 relative">
        <div className="w-full">
          {/* Header */}
          <div className="text-center mb-12 animate-fade-in px-4 md:px-6 lg:px-8">
            <h1 className="text-4xl md:text-5xl font-bold mb-4">
              <span className="text-3xl md:text-4xl mr-2">🔍</span>
              <span className="bg-gradient-to-r from-gradient-purple via-gradient-blue to-gradient-teal bg-clip-text text-transparent">
                {config.appName}
              </span>
            </h1>
            <p className="text-dark-text-secondary text-lg max-w-2xl mx-auto">
              Search Wissen's Zoho Recruit ATS for the best profiles given a Job Description
            </p>
          </div>

          {/* Input Panel */}
          <div className="mb-8 px-4 md:px-6 lg:px-8 max-w-6xl mx-auto w-full">
            <ModernInputPanel
              text={text}
              pdfFile={pdfFile}
              loading={loading}
              error={error}
              onTextChange={handleTextChange}
              onFileChange={handleFileChange}
              onSubmit={handleSubmit}
              onReset={handleReset}
              maxTextLength={config.maxTextLength}
            />
          </div>

          {/* Pipeline Stepper - Show during and after loading */}
          {(loading || pipelineStep !== 'idle') && (
            <div className="mb-8 animate-fade-in px-4 md:px-6 lg:px-8 max-w-6xl mx-auto w-full">
              <PipelineStepper activeStep={pipelineStep} />
            </div>
          )}

          {/* Success Message */}
          {success && !loading && (
            <div className="mb-8 animate-fade-in p-4 rounded-lg bg-color-success/10 border border-color-success/30 text-color-success flex items-center gap-3 m-auto md:mx-6 lg:m-auto max-w-6xl w-full" style={{margin: 'auto'}}>
              <span className="text-xl">✨</span>
              <span className="font-semibold">Found matching candidate profiles!</span>
            </div>
          )}

          {/* Results */}
          {candidates.length > 0 && (
            <div className="animate-fade-in px-4 md:px-6 lg:px-8 max-w-6xl mx-auto w-full">
              <ModernCandidateResults candidates={candidates} />
            </div>
          )}

          {/* Empty State */}
          {!candidates.length && !loading && !success && (
            <div className="text-center py-12 animate-fade-in px-4 md:px-6 lg:px-8">
              <div className="text-6xl mb-4">🎯</div>
              <h2 className="text-2xl font-bold text-dark-text mb-2">Ready to Find Candidates?</h2>
              <p className="text-dark-text-secondary max-w-md mx-auto">
                Enter a job description or upload a PDF to get started with AI-powered candidate matching
              </p>
            </div>
          )}
        </div>
      </main>
    </div>
  );
};

export default DocumentUploadPage;
