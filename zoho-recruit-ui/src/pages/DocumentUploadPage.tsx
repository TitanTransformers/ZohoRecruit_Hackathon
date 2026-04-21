import React, { useState, useEffect, useRef } from 'react';
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
import Footer from '../components/Footer';

const DUMMY_CANDIDATES = [
  {
    candidateId: "ZR_001",
    name: "Aarav Sharma",
    email: "aarav.sharma@example.com",
    mobile: "9876543210",
    matchPercentage: 92,
    skillMatchPercentage: 95,
    experienceMatchPercentage: 90,
    rankPosition: 1,
    matchedSkills: ["React", "TypeScript", "Node.js", "Redux", "CSS"],
    missingSkills: ["GraphQL"],
    fitAnalysis: "Excellent fit for the frontend role with strong React and TypeScript experience.",
    matchReasoning: "Strong background in React ecosystem. Missing GraphQL but has extensive REST API experience."
  },
  {
    candidateId: "ZR_002",
    name: "Priya Patel",
    email: "priya.p@example.com",
    mobile: "9876543211",
    matchPercentage: 85,
    skillMatchPercentage: 80,
    experienceMatchPercentage: 95,
    rankPosition: 2,
    matchedSkills: ["React", "JavaScript", "HTML", "CSS"],
    missingSkills: ["TypeScript", "Node.js", "Redux"],
    fitAnalysis: "Good experience but lacking some modern tools like TypeScript.",
    matchReasoning: "Solid core frontend skills. Will need training on TypeScript."
  },
  {
    candidateId: "ZR_003",
    name: "Rohan Gupta",
    email: "rohan.gupta@example.com",
    mobile: "9876543212",
    matchPercentage: 75,
    skillMatchPercentage: 70,
    experienceMatchPercentage: 80,
    rankPosition: 3,
    matchedSkills: ["Node.js", "Express", "MongoDB"],
    missingSkills: ["React", "TypeScript", "Frontend"],
    fitAnalysis: "More of a backend developer. Might struggle with complex UI tasks.",
    matchReasoning: "Primarily backend focused. Missing core frontend requirements."
  },
  {
    candidateId: "ZR_004",
    name: "Neha Singh",
    email: "neha.s@example.com",
    mobile: "9876543213",
    matchPercentage: 62,
    skillMatchPercentage: 60,
    experienceMatchPercentage: 65,
    rankPosition: 4,
    matchedSkills: ["HTML", "CSS", "JavaScript"],
    missingSkills: ["React", "TypeScript", "Node.js", "Redux", "Webpack"],
    fitAnalysis: "Junior candidate. Good for basic tasks but lacks advanced framework knowledge.",
    matchReasoning: "Entry level skills. Missing all advanced framework requirements."
  },
  {
    candidateId: "ZR_005",
    name: "Vikram Verma",
    email: "vikram.v@example.com",
    mobile: "9876543214",
    matchPercentage: 45,
    skillMatchPercentage: 40,
    experienceMatchPercentage: 50,
    rankPosition: 5,
    matchedSkills: ["Python", "Django"],
    missingSkills: ["React", "TypeScript", "Node.js", "JavaScript", "HTML", "CSS"],
    fitAnalysis: "Wrong tech stack. Primarily a Python developer.",
    matchReasoning: "Backend Python developer. Does not match frontend requirements."
  }
];

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
    searchResults,
  } = useSelector((state: RootState) => state.document);

  const [activePage, setActivePage] = useState('source');
  const [history, setHistory] = useState<SearchHistoryItem[]>([]);
  const [isFast, setIsFast] = useState(true);
  // In-memory cache: historyItem.id → CandidateProfile[]
  const candidateCache = useRef<Map<string, typeof candidates>>(new Map());
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
        candidateResults = await candidateService.searchByText(text, isFast);
      } else if (pdfFile) {
        candidateResults = await candidateService.searchByDocument(pdfFile, text, isFast);
      } else {
        throw new Error('No input provided');
      }

      // Store results in Redux
      dispatch(setSearchResults({ id: searchId, results: candidateResults }));
      dispatch(setCurrentSearchId(searchId));

      // Add to search history and cache candidates
      const historyItem = searchHistoryService.addToHistory({
        query,
        type: pdfFile ? 'pdf' : 'text',
        fileName: pdfFile?.name,
        candidatesCount: candidateResults.length,
      });
      candidateCache.current.set(historyItem.id, candidateResults);
      localStorage.setItem(`results_${historyItem.id}`, searchId);
      // Keep cache bounded to last 10
      if (candidateCache.current.size > 10) {
        const firstKey = candidateCache.current.keys().next().value;
        if (firstKey) candidateCache.current.delete(firstKey);
      }
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
    // 1. Check in-memory cache first (fastest — no API or localStorage lookup)
    const cached = candidateCache.current.get(item.id);
    if (cached && cached.length > 0) {
      const searchId = `hist_${item.id}`;
      dispatch(setSearchResults({ id: searchId, results: cached }));
      dispatch(setCurrentSearchId(searchId));
      dispatch(setSuccess(true));
      dispatch(setError(null));
      setActivePage('source');
      return;
    }

    // 2. Check Redux store via localStorage key
    const resultsId = localStorage.getItem(`results_${item.id}`);
    if (resultsId && searchResults[resultsId]) {
      dispatch(loadSearchResults(resultsId));
      dispatch(setSuccess(true));
      setActivePage('source');
      return;
    }

    // 3. Fallback: pre-fill the query so user can re-run
    dispatch(setText(item.type === 'text' ? item.query : (item.fileName ? `PDF: ${item.fileName}` : item.query)));
    dispatch(setPdfFile(null));
    dispatch(setError('Results expired — please re-run this search.'));
    setActivePage('source');
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

  const loadDummyData = () => {
    dispatch(setError(null));
    dispatch(setSuccess(false));
    dispatch(setPipelineStep('parse'));
    dispatch(setLoading(true));
    
    setTimeout(() => {
      dispatch(setPipelineStep('query'));
      setTimeout(() => {
         dispatch(setPipelineStep('search'));
         setTimeout(() => {
           const searchId = 'dummy-search-' + Date.now();
           dispatch(setSearchResults({ id: searchId, results: DUMMY_CANDIDATES as any }));
           dispatch(setCurrentSearchId(searchId));
           dispatch(setPipelineStep('rank'));
           dispatch(setSuccess(true));
           dispatch(setCostEstimate(0.15));
           setSessionStats(prev => ({
              ...prev,
              apiCalls: prev.apiCalls + 3,
              candidatesFound: prev.candidatesFound + 5,
              avgMatch: '71%',
              totalCost: `$${(parseFloat(prev.totalCost.replace('$', '')) + 0.15).toFixed(3)}`,
           }));
           setTimeout(() => {
             dispatch(setPipelineStep('idle'));
             dispatch(setLoading(false));
           }, 1500);
         }, 1000)
      }, 1000)
    }, 1000);
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
    <div className="app-wrapper">
      {/* Navbar */}
      <ModernNavbar />

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
                isFast={isFast}
                onTextChange={handleTextChange}
                onFileChange={handleFileChange}
                onSubmit={handleSubmit}
                onReset={handleReset}
                onToggleFast={setIsFast}
              />

              {/* Dummy Data Button */}
              <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: 10, marginBottom: 15, paddingRight: 20 }}>
                <button 
                  className="ghost-btn" 
                  onClick={loadDummyData} 
                  type="button"
                  style={{ fontSize: 12, color: 'var(--text-muted)' }}
                  disabled={loading}
                >
                  🧪 Load Dummy Data
                </button>
              </div>

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
          <Footer />
        </main>
      </div>
    </div>
  );
};

export default DocumentUploadPage;
