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
import ModernNavbar from '../components/ModernNavbar';
import Sidebar from '../components/Sidebar';
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

  const [activePage, setActivePage] = useState('source');
  const [history, setHistory] = useState<SearchHistoryItem[]>([]);
  const [sessionStats, setSessionStats] = useState({
    apiCalls: 0,
    candidatesFound: 0,
    avgMatch: '—',
    totalCost: '$0.00',
  });

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
    dispatch(setSuccess(false));
    dispatch(setPipelineStep('parse'));

    try {
      let candidateResults;
      const query = text || `PDF: ${pdfFile?.name}`;
      const searchId = Date.now().toString();

      // Animate pipeline steps with timeouts
      setTimeout(() => dispatch(setPipelineStep('query')), 1000);
      setTimeout(() => dispatch(setPipelineStep('search')), 2000);

      if (text && !pdfFile) {
        candidateResults = await candidateService.searchByText(text);
      } else if (pdfFile) {
        candidateResults = await candidateService.searchByDocument(pdfFile, text);
      } else {
        throw new Error('No input provided');
      }

      // Store results in Redux
      dispatch(setSearchResults({ id: searchId, results: candidateResults }));
      dispatch(setCurrentSearchId(searchId));

      // Add to search history
      const historyItem = searchHistoryService.addToHistory({
        query,
        type: pdfFile ? 'pdf' : 'text',
        fileName: pdfFile?.name,
        candidatesCount: candidateResults.length,
      });
      localStorage.setItem(`results_${historyItem.id}`, searchId);
      setHistory(searchHistoryService.getHistory());

      dispatch(setPipelineStep('rank'));
      dispatch(setSuccess(true));

      // Cost estimate
      const cost = Math.random() * 0.5 + 0.01;
      dispatch(setCostEstimate(cost));

      // Update session stats
      const avg = candidateResults.length > 0
        ? Math.round(candidateResults.reduce((s, c) => s + (c.matchPercentage ?? 0), 0) / candidateResults.length)
        : 0;
      setSessionStats(prev => ({
        apiCalls: prev.apiCalls + 3,
        candidatesFound: candidateResults.length,
        avgMatch: `${avg}%`,
        totalCost: `$${(parseFloat(prev.totalCost.replace('$', '')) + cost).toFixed(3)}`,
      }));

      // Reset pipeline after delay
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
    // Try to load cached results
    const resultsId = localStorage.getItem(`results_${item.id}`);
    if (resultsId && searchResults[resultsId]) {
      dispatch(loadSearchResults(resultsId));
      dispatch(setSuccess(true));
      setActivePage('source');
    } else if (item.type === 'text') {
      // Re-populate the text field so user can re-run
      dispatch(setText(item.query));
      dispatch(setPdfFile(null));
      setActivePage('source');
    } else {
      // PDF was used but file is gone — just show the query
      dispatch(setText(item.query));
      setActivePage('source');
    }
  };

  const handleDeleteHistory = (id: string) => {
    searchHistoryService.deleteFromHistory(id);
    localStorage.removeItem(`results_${id}`);
    setHistory(searchHistoryService.getHistory());
  };

  const handleClearHistory = () => {
    searchHistoryService.clearHistory();
    // Clear result mappings
    Object.keys(localStorage).forEach(key => {
      if (key.startsWith('results_')) localStorage.removeItem(key);
    });
    setHistory([]);
  };

  const handleReset = () => {
    dispatch(resetForm());
    dispatch(setPipelineStep('idle'));
  };

  const formatTime = (timestamp: number) => {
    const diff = Date.now() - timestamp;
    const mins = Math.floor(diff / 60000);
    const hrs = Math.floor(diff / 3600000);
    const days = Math.floor(diff / 86400000);
    if (mins < 1) return 'Just now';
    if (mins < 60) return `${mins}m ago`;
    if (hrs < 24) return `${hrs}h ago`;
    if (days < 7) return `${days}d ago`;
    return new Date(timestamp).toLocaleDateString();
  };

  return (
    <div>
      {/* Navbar */}
      <ModernNavbar costEstimate={costEstimate} />

      {/* Layout: Sidebar + Main */}
      <div className="app-layout">
        <Sidebar
          activePage={activePage}
          onNavigate={setActivePage}
          stats={sessionStats}
        />

        <main className="main-content">
          {/* ═══ SOURCE CANDIDATES PAGE ═══ */}
          {activePage === 'source' && (
            <div className="animate-fade-in">
              {/* Input Panel */}
              <ModernInputPanel
                text={text}
                pdfFile={pdfFile}
                loading={loading}
                error={error}
                onTextChange={handleTextChange}
                onFileChange={handleFileChange}
                onSubmit={handleSubmit}
                onReset={handleReset}
              />

              {/* Pipeline Stepper */}
              {(loading || pipelineStep !== 'idle') && (
                <PipelineStepper activeStep={pipelineStep} />
              )}

              {/* Results */}
              {candidates.length > 0 && (
                <div style={{ marginTop: 20 }}>
                  <ModernCandidateResults candidates={candidates} />
                </div>
              )}

              {/* Empty State */}
              {candidates.length === 0 && !loading && !success && (
                <div className="empty-state">
                  <div className="empty-icon">🔍</div>
                  <div className="empty-title">Ready to Find Candidates</div>
                  <div className="empty-sub">
                    Paste a job description or upload a PDF to search Zoho Recruit for the best matching profiles.
                  </div>
                </div>
              )}
            </div>
          )}

          {/* ═══ PAST SEARCHES PAGE ═══ */}
          {activePage === 'past' && (
            <div className="placeholder-page animate-fade-in">
              <div className="placeholder-icon">📋</div>
              <div className="placeholder-title">Past Searches</div>
              {history.length === 0 ? (
                <div className="placeholder-desc">
                  Your recent sourcing runs will appear here. Run your first search to get started.
                </div>
              ) : (
                <>
                  <div className="placeholder-desc" style={{ marginBottom: 16 }}>
                    You have {history.length} recent search{history.length !== 1 ? 'es' : ''}. Click to reload results.
                  </div>
                  <div style={{ display: 'flex', gap: 8, marginBottom: 20 }}>
                    <button className="ghost-btn" onClick={handleClearHistory} type="button">
                      🗑 Clear All
                    </button>
                  </div>
                  <div className="history-list">
                    {history.map((item) => (
                      <button
                        key={item.id}
                        className="history-item"
                        onClick={() => handleSelectHistory(item)}
                        type="button"
                      >
                        <span className="history-item-icon">
                          {item.type === 'text' ? '📝' : '📄'}
                        </span>
                        <div className="history-item-info">
                          <div className="history-item-query">
                            {item.type === 'pdf' ? item.fileName || 'PDF Document' : item.query}
                          </div>
                          <div className="history-item-meta">
                            {formatTime(item.timestamp)}
                          </div>
                        </div>
                        {item.candidatesCount !== undefined && (
                          <span className="history-item-count">
                            {item.candidatesCount} results
                          </span>
                        )}
                      </button>
                    ))}
                  </div>
                </>
              )}
            </div>
          )}

          {/* ═══ ANALYTICS PAGE ═══ */}
          {activePage === 'analytics' && (
            <div className="placeholder-page animate-fade-in">
              <div className="placeholder-icon">📊</div>
              <div className="placeholder-title">Analytics</div>
              <div className="placeholder-desc">
                Track sourcing performance, match score trends, and cost efficiency across all your searches.
              </div>
            </div>
          )}

          {/* ═══ SETTINGS PAGE ═══ */}
          {activePage === 'settings' && (
            <div className="placeholder-page animate-fade-in">
              <div className="placeholder-icon">⚙️</div>
              <div className="placeholder-title">Settings</div>
              <div className="placeholder-desc">
                Configure your Zoho Recruit integration, API keys, and sourcing preferences here.
              </div>
            </div>
          )}
        </main>
      </div>
    </div>
  );
};

export default DocumentUploadPage;
