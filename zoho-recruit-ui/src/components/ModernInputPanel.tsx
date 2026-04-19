import React, { useRef, useState } from 'react';

interface ModernInputPanelProps {
  text: string;
  pdfFile: File | null;
  loading: boolean;
  error: string | null;
  onTextChange: (text: string) => void;
  onFileChange: (file: File | null) => void;
  onSubmit: () => void;
  onReset: () => void;
  maxTextLength?: number;
}

const ModernInputPanel: React.FC<ModernInputPanelProps> = ({
  text,
  pdfFile,
  loading,
  error,
  onTextChange,
  onFileChange,
  onSubmit,
  onReset,
  maxTextLength = 5000,
}) => {
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [isDragging, setIsDragging] = useState(false);
  const [fileError, setFileError] = useState<string | null>(null);
  const [activeTab, setActiveTab] = useState<'text' | 'pdf'>('text');

  const textCharCount = text.length;
  const hasInput = text.trim().length > 0 || pdfFile !== null;

  const handleTextChange = (e: React.ChangeEvent<HTMLTextAreaElement>) => {
    const newText = e.target.value;
    if (newText.length <= maxTextLength) {
      onTextChange(newText);
    }
  };

  const handleFileSelect = (file: File | null) => {
    if (!file) {
      onFileChange(null);
      setFileError(null);
      return;
    }

    if (file.type === 'application/pdf') {
      onFileChange(file);
      setFileError(null);
    } else {
      setFileError('Only PDF files are supported');
      onFileChange(null);
    }
  };

  const handleDragOver = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragging(true);
  };

  const handleDragLeave = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragging(false);
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragging(false);
    const file = e.dataTransfer.files?.[0];
    if (file) {
      handleFileSelect(file);
    }
  };

  return (
    <div className="space-y-6">
      {/* Tab Navigation */}
      <div className="flex gap-2 border-b border-dark-border/50">
        <button
          onClick={() => setActiveTab('text')}
          className={`px-6 py-3 font-semibold transition-all ${
            activeTab === 'text'
              ? 'text-gradient-purple border-b-2 border-gradient-purple'
              : 'text-dark-text-secondary hover:text-dark-text'
          }`}
        >
          <span className="mr-2">📋</span>Job Description
        </button>
        <button
          onClick={() => setActiveTab('pdf')}
          className={`px-6 py-3 font-semibold transition-all ${
            activeTab === 'pdf'
              ? 'text-gradient-purple border-b-2 border-gradient-purple'
              : 'text-dark-text-secondary hover:text-dark-text'
          }`}
        >
          <span className="mr-2">📄</span>Upload PDF
        </button>
      </div>

      {/* Text Input Tab */}
      {activeTab === 'text' && (
        <div className="glass rounded-xl p-6 border border-glass-border hover:border-gradient-purple/30 transition-all duration-300 animate-fade-in">
          <div className="flex items-center justify-between mb-4">
            <div>
              <h3 className="text-lg font-bold text-dark-text flex items-center gap-2">
                📋 Job Description
              </h3>
              <p className="text-xs text-dark-text-secondary mt-1">Paste or type the job requirements</p>
            </div>
            {textCharCount > 0 && (
              <div className="px-3 py-1 rounded-lg bg-gradient-to-r from-gradient-purple/20 to-gradient-blue/20 border border-gradient-purple/30">
                <span className="text-xs font-semibold text-gradient-purple">{textCharCount} chars</span>
              </div>
            )}
          </div>

          <textarea
            value={text}
            onChange={handleTextChange}
            disabled={loading}
            placeholder="Paste the job description here... Include skills, experience, qualifications, and requirements"
            className="w-full h-40 px-4 py-3 rounded-lg bg-dark-bg-secondary border border-dark-border text-dark-text placeholder-dark-text-secondary focus:outline-none focus:border-gradient-purple focus:ring-2 focus:ring-gradient-purple/30 resize-none transition-all duration-300 disabled:opacity-50 disabled:cursor-not-allowed"
          />

          {/* Character Count Progress Bar */}
          <div className="mt-4 space-y-2">
            <div className="flex items-center justify-between text-xs">
              <span className="text-dark-text-secondary">Character limit</span>
              <span className={`font-semibold ${textCharCount > maxTextLength * 0.9 ? 'text-color-warning' : 'text-dark-text-secondary'}`}>
                {maxTextLength - textCharCount} remaining
              </span>
            </div>
            <div className="w-full h-1 rounded-full bg-dark-bg-secondary overflow-hidden">
              <div
                className={`h-full rounded-full transition-all duration-300 ${
                  textCharCount > maxTextLength * 0.9
                    ? 'bg-gradient-to-r from-color-warning to-color-error'
                    : 'bg-gradient-to-r from-gradient-purple to-gradient-blue'
                }`}
                style={{ width: `${Math.min(100, (textCharCount / maxTextLength) * 100)}%` }}
              />
            </div>
          </div>
        </div>
      )}

      {/* PDF Upload Tab */}
      {activeTab === 'pdf' && (
        <div
          className="glass rounded-xl p-6 border-2 border-dashed border-dark-border transition-all duration-300 group animate-fade-in"
          onDragOver={handleDragOver}
          onDragLeave={handleDragLeave}
          onDrop={handleDrop}
          style={isDragging ? {
            borderColor: '#7C3AED',
            backgroundColor: 'rgba(124, 58, 237, 0.05)',
          } : {}}
        >
          <div className="text-center">
            <div className="mb-4">
              <span className="inline-block text-4xl">{pdfFile ? '✓' : '📄'}</span>
            </div>

            <h3 className="text-lg font-bold text-dark-text mb-1">
              {pdfFile ? 'PDF Selected' : 'Upload PDF'}
            </h3>
            <p className="text-sm text-dark-text-secondary mb-4">
              {pdfFile
                ? `File: ${pdfFile.name} (${(pdfFile.size / 1024).toFixed(1)} KB)`
                : 'Drag and drop your file here, or click to browse'}
            </p>

            {!pdfFile && (
              <>
                <button
                  onClick={() => fileInputRef.current?.click()}
                  disabled={loading}
                  className="px-6 py-2 rounded-lg bg-gradient-to-r from-gradient-purple to-gradient-blue text-white font-semibold hover:shadow-lg hover:scale-105 transition-all disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  Choose File
                </button>
                <input
                  ref={fileInputRef}
                  type="file"
                  accept=".pdf"
                  onChange={(e) => handleFileSelect(e.target.files?.[0] || null)}
                  disabled={loading}
                  style={{ display: 'none' }}
                />
              </>
            )}

            {pdfFile && (
              <button
                onClick={() => handleFileSelect(null)}
                disabled={loading}
                className="px-6 py-2 rounded-lg glass border border-glass-border text-color-error hover:bg-color-error/10 font-semibold transition-all disabled:opacity-50"
              >
                Remove File
              </button>
            )}
          </div>

          {fileError && (
            <div className="mt-4 p-3 rounded-lg bg-color-error/10 border border-color-error/30 text-color-error text-sm">
              {fileError}
            </div>
          )}
        </div>
      )}

      {/* Error Alert */}
      {error && (
        <div className="animate-fade-in p-4 rounded-lg bg-color-error/10 border border-color-error/30 text-color-error text-sm">
          {error}
        </div>
      )}

      {/* Action Buttons */}
      <div className="flex gap-3 mt-6">
        <button
          onClick={onSubmit}
          disabled={loading || !hasInput}
          className={`flex-1 px-6 py-3 rounded-lg font-bold text-white transition-all duration-300 ${
            loading || !hasInput
              ? 'opacity-50 cursor-not-allowed bg-dark-bg-secondary'
              : 'bg-gradient-to-r from-gradient-purple via-gradient-blue to-gradient-teal hover:shadow-lg hover:scale-105 active:scale-95'
          }`}
        >
          <div className="flex items-center justify-center gap-2">
            {loading ? (
              <>
                <div className="w-5 h-5 rounded-full border-2 border-white border-t-transparent animate-spin"></div>
                <span>Searching...</span>
              </>
            ) : (
              <>
                <span>🔍</span>
                <span>Find Candidates</span>
              </>
            )}
          </div>
        </button>

        <button
          onClick={onReset}
          disabled={loading || !hasInput}
          className="px-6 py-3 rounded-lg glass border border-glass-border text-dark-text font-bold hover:border-gradient-purple/50 transition-all disabled:opacity-50 disabled:cursor-not-allowed"
        >
          Clear
        </button>
      </div>
    </div>
  );
};

export default ModernInputPanel;
