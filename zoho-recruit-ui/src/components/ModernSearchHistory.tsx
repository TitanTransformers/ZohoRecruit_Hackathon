import React, { useState } from 'react';
import type { SearchHistoryItem } from '../services/searchHistoryService';

interface ModernSearchHistoryProps {
  items: SearchHistoryItem[];
  onSelect: (item: SearchHistoryItem) => void;
  onDelete: (id: string) => void;
  onClear: () => void;
}

const ModernSearchHistory: React.FC<ModernSearchHistoryProps> = ({ items, onSelect, onDelete, onClear }) => {
  const [isExpanded, setIsExpanded] = useState(false);

  if (items.length === 0) {
    return null;
  }

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

  return (
    <div className="fixed bottom-0 left-0 right-0 z-40">
      <div className="max-w-full mx-auto px-4 pb-4">
        <div className="glass rounded-t-xl border border-dark-border/50 border-b-0 overflow-hidden">
          {/* Header */}
          <button
            onClick={() => setIsExpanded(!isExpanded)}
            className="w-full px-6 py-4 flex items-center justify-between hover:bg-white/5 transition-colors"
          >
            <div className="flex items-center gap-3">
              <span className="text-xl">📋</span>
              <div className="text-left">
                <h3 className="font-bold text-dark-text">Search History</h3>
                <p className="text-xs text-dark-text-secondary">{items.length} search{items.length !== 1 ? 'es' : ''}</p>
              </div>
            </div>
            <div className="flex items-center gap-2">
              {items.length > 0 && (
                <button
                  onClick={(e) => {
                    e.stopPropagation();
                    onClear();
                  }}
                  className="px-3 py-1 text-xs rounded-lg glass border border-glass-border text-color-error hover:border-color-error/50 transition-all"
                >
                  Clear
                </button>
              )}
              <svg
                className={`w-5 h-5 text-dark-text-secondary transition-transform duration-300 ${isExpanded ? 'rotate-180' : ''}`}
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 14l-7 7m0 0l-7-7m7 7V3" />
              </svg>
            </div>
          </button>

          {/* Content */}
          {isExpanded && (
            <div className="border-t border-dark-border/50 max-h-64 overflow-y-auto animate-fade-in">
              <div className="divide-y divide-dark-border/30">
                {items.slice(0, 10).map((item) => (
                  <div
                    key={item.id}
                    onClick={() => onSelect(item)}
                    className="px-6 py-4 hover:bg-white/5 cursor-pointer transition-all group"
                  >
                    <div className="flex items-center justify-between">
                      <div className="flex-1">
                        <div className="flex items-center gap-2 mb-1">
                          <span className="text-lg">{item.type === 'text' ? '📝' : '📄'}</span>
                          <p className="text-sm font-medium text-dark-text truncate">
                            {item.type === 'pdf' ? item.fileName || 'PDF Document' : item.query}
                          </p>
                        </div>
                        <div className="flex items-center gap-2 text-xs text-dark-text-secondary">
                          <span>{formatTime(item.timestamp)}</span>
                          {item.candidatesCount && (
                            <>
                              <span>•</span>
                              <span>{item.candidatesCount} candidates</span>
                            </>
                          )}
                        </div>
                      </div>
                      <button
                        onClick={(e) => {
                          e.stopPropagation();
                          onDelete(item.id);
                        }}
                        className="p-2 rounded-lg opacity-0 group-hover:opacity-100 hover:bg-color-error/10 text-color-error transition-all ml-2"
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
      </div>
    </div>
  );
};

export default ModernSearchHistory;
