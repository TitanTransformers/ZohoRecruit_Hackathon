import React, { useState } from 'react';
import { config } from '../config/environment';
import type { SearchHistoryItem } from '../services/searchHistoryService';

interface ModernNavbarProps {
  costEstimate?: number;
  historyItems?: SearchHistoryItem[];
  onHistorySelect?: (item: SearchHistoryItem) => void;
  onHistoryDelete?: (id: string) => void;
  onHistoryClear?: () => void;
}

const ModernNavbar: React.FC<ModernNavbarProps> = ({
  costEstimate = 0,
  historyItems = [],
  onHistorySelect,
  onHistoryDelete,
  onHistoryClear,
}) => {
  const [isHistoryOpen, setIsHistoryOpen] = useState(false);

  const formatTime = (timestamp: number) => {
    const date = new Date(timestamp);
    const now = new Date();
    const diff = now.getTime() - date.getTime();
    const minutes = Math.floor(diff / 60000);
    const hours = Math.floor(diff / 3600000);
    const days = Math.floor(diff / 86400000);

    if (minutes < 1) return 'Just now';
    if (minutes < 60) return `${minutes}m ago`;
    if (hours < 24) return `${hours}h ago`;
    if (days < 7) return `${days}d ago`;
    return date.toLocaleDateString();
  };

  const handleHistorySelect = (item: SearchHistoryItem) => {
    onHistorySelect?.(item);
    setIsHistoryOpen(false);
  };

  return (
    <nav className="fixed top-0 left-0 right-0 z-50 glass border-b border-dark-border/50">
      <div className="max-w-full mx-auto px-6 py-4 flex items-center justify-between">
        {/* Logo */}
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-gradient-purple to-gradient-blue flex items-center justify-center">
            <span className="text-white font-bold text-lg">🔍</span>
          </div>
          <div className="hidden sm:block">
            <h1 className="text-lg font-bold bg-gradient-to-r from-gradient-purple via-gradient-blue to-gradient-teal bg-clip-text text-transparent">
              {config.appName}
            </h1>
            <p className="text-xs text-dark-text-secondary">AI-Powered Recruitment</p>
          </div>
        </div>

        {/* Right Section */}
        <div className="flex items-center gap-4">
         
          {/* History Dropdown */}
          {historyItems && historyItems.length > 0 && (
            <div className="relative">
              <button
                onClick={() => setIsHistoryOpen(!isHistoryOpen)}
                className="p-2 rounded-lg hover:glass transition-all relative group"
                title="Search History"
              >
                <svg
                  className="w-5 h-5 text-dark-text-secondary group-hover:text-dark-text transition-colors"
                  fill="none"
                  stroke="currentColor"
                  viewBox="0 0 24 24"
                >
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
                {historyItems.length > 0 && (
                  <span className="absolute top-0 right-0 w-5 h-5 rounded-full bg-gradient-to-r from-gradient-purple to-gradient-blue flex items-center justify-center text-white text-xs font-bold">
                    {historyItems.length > 9 ? '9+' : historyItems.length}
                  </span>
                )}
              </button>

              {/* Dropdown Menu */}
              {isHistoryOpen && (
                <div className="absolute right-0 mt-2 w-72 glass rounded-xl border border-dark-border/50 shadow-2xl z-50 overflow-hidden animate-fade-in">
                  {/* Header */}
                  <div className="px-4 py-3 border-b border-dark-border/30 flex items-center justify-between">
                    <div>
                      <h3 className="font-bold text-dark-text text-sm">📋 Search History</h3>
                      <p className="text-xs text-dark-text-secondary">{historyItems.length} search{historyItems.length !== 1 ? 'es' : ''}</p>
                    </div>
                    {historyItems.length > 0 && (
                      <button
                        onClick={() => {
                          onHistoryClear?.();
                          setIsHistoryOpen(false);
                        }}
                        className="px-2 py-1 text-xs rounded-lg glass border border-glass-border text-color-error hover:border-color-error/50 transition-all"
                      >
                        Clear
                      </button>
                    )}
                  </div>

                  {/* Items */}
                  <div className="max-h-80 overflow-y-auto divide-y divide-dark-border/30">
                    {historyItems.slice(0, 10).map((item) => (
                      <div
                        key={item.id}
                        className="px-4 py-3 hover:bg-white/5 cursor-pointer transition-all group"
                        onClick={() => handleHistorySelect(item)}
                      >
                        <div className="flex items-start justify-between gap-2">
                          <div className="flex-1 min-w-0">
                            <div className="flex items-center gap-2 mb-1">
                              <span className="text-lg flex-shrink-0">{item.type === 'text' ? '📝' : '📄'}</span>
                              <p className="text-xs font-medium text-dark-text truncate">
                                {item.type === 'pdf' ? item.fileName || 'PDF Document' : item.query}
                              </p>
                            </div>
                            <div className="flex items-center gap-1 text-xs text-dark-text-secondary">
                              <span>{formatTime(item.timestamp)}</span>
                              {item.candidatesCount && (
                                <>
                                  <span>•</span>
                                  <span>{item.candidatesCount} results</span>
                                </>
                              )}
                            </div>
                          </div>
                          <button
                            onClick={(e) => {
                              e.stopPropagation();
                              onHistoryDelete?.(item.id);
                            }}
                            className="p-1 rounded opacity-0 group-hover:opacity-100 hover:bg-color-error/10 text-color-error transition-all flex-shrink-0"
                          >
                            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                            </svg>
                          </button>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              )}
            </div>
          )}

          {/* Settings */}
          <button className="p-2 rounded-lg hover:glass transition-all">
            <svg
              className="w-5 h-5 text-dark-text-secondary hover:text-dark-text"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z"
              />
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
            </svg>
          </button>
        </div>
      </div>
    </nav>
  );
};

export default ModernNavbar;
