import React, { useState } from 'react';
import type { CandidateProfile } from '../types/candidate';

interface CandidateCardProps {
  candidate: CandidateProfile;
  rank: number;
}

const CandidateCard: React.FC<CandidateCardProps> = ({ candidate, rank }) => {
  const [isExpanded, setIsExpanded] = useState(false);
  const matchPercentage = candidate.matchPercentage ?? 0;

  const getMatchColor = (percentage: number) => {
    if (percentage >= 80) return 'from-color-success to-color-success';
    if (percentage >= 60) return 'from-color-warning to-color-warning';
    return 'from-color-error to-color-error';
  };

  const getMatchBadgeColor = (percentage: number) => {
    if (percentage >= 80) return 'bg-color-success/20 text-color-success border-color-success/50';
    if (percentage >= 60) return 'bg-color-warning/20 text-color-warning border-color-warning/50';
    return 'bg-color-error/20 text-color-error border-color-error/50';
  };

  return (
    <div
      className="group glass rounded-xl p-6 border border-glass-border hover:border-gradient-purple/50 transition-all duration-300 hover:shadow-lg cursor-pointer w-full"
      onClick={() => setIsExpanded(!isExpanded)}
    >
      {/* Header */}
      <div className="flex items-center justify-between gap-6 mb-4">
        {/* Rank Badge */}
        <div className="flex-shrink-0 text-2xl font-bold text-dark-text-secondary w-8 text-center">
          #{rank}
        </div>

        {/* Avatar */}
        <div className={`w-16 h-16 rounded-lg bg-gradient-to-br ${getMatchColor(matchPercentage)} flex items-center justify-center text-white font-bold text-lg shadow-lg flex-shrink-0`}>
          {candidate.name.split(' ').map(n => n[0]).join('').toUpperCase().slice(0, 2)}
        </div>

        {/* Info */}
        <div className="flex-1 min-w-0">
          <h3 className="text-lg font-bold text-dark-text truncate">{candidate.name}</h3>
          <p className="text-sm text-dark-text-secondary truncate">{candidate.email}</p>
        </div>

        {/* Match Percentage Circle */}
        <div className="relative w-16 h-16">
          <svg className="w-full h-full transform -rotate-90" viewBox="0 0 100 100">
            {/* Background circle */}
            <circle
              cx="50"
              cy="50"
              r="40"
              fill="none"
              stroke="rgba(255, 255, 255, 0.1)"
              strokeWidth="4"
            />
            {/* Progress circle */}
            <circle
              cx="50"
              cy="50"
              r="40"
              fill="none"
              stroke={matchPercentage >= 80 ? '#10b981' : matchPercentage >= 60 ? '#f59e0b' : '#ef4444'}
              strokeWidth="4"
              strokeDasharray={`${(matchPercentage / 100) * 2 * Math.PI * 40} ${2 * Math.PI * 40}`}
              strokeLinecap="round"
              className="transition-all duration-500"
            />
          </svg>
          <div className="absolute inset-0 flex items-center justify-center">
            <span className="text-base font-bold text-dark-text leading-none">{Math.floor(matchPercentage)}%</span>
          </div>
        </div>
      </div>

      {/* Match Badge */}
      <div className="flex items-center gap-2 mb-4">
        <span className={`px-0 py-1 rounded-full text-lg font-bold border ${getMatchBadgeColor(matchPercentage)}`}>
          {matchPercentage >= 80 ? '✓ Perfect Match' : matchPercentage >= 60 ? 'Good Match' : 'Partial Match'}
        </span>
      </div>

      {/* Skills - Matched and Missing Combined */}
      <div className="mb-4">
        {/* Matched Skills Header */}
        {candidate.matchedSkills && candidate.matchedSkills.length > 0 && (
          <div className="mb-3">
            <p className="text-xs font-bold underline text-color-warning mb-2 uppercase tracking-wide">✓ Matched Skills</p>
            <div className="flex flex-wrap gap-2">
              {candidate.matchedSkills.slice(0, 5).map((skill, idx) => (
                <span
                  key={idx}
                  className="px-3 py-1.5 rounded-lg bg-color-success/15 border border-color-success/40 text-xs font-semibold text-color-success hover:border-color-success/60 transition-all flex items-center gap-1"
                >
                  <span>✓</span>
                  {skill}
                </span>
              ))}
            </div>
          </div>
        )}

        {/* Missing Skills Header */}
        {candidate.missingSkills && candidate.missingSkills.length > 0 && (
          <div className="mt-4 pt-4 border-t border-dark-border/30">
            <p className="text-xs font-bold text-color-warning mb-2 uppercase tracking-wide">✗ Missing Skills</p>
            <div className="flex flex-wrap gap-2">
              {candidate.missingSkills.slice(0, 5).map((skill, idx) => (
                <span
                  key={idx}
                  className="px-3 py-1.5 rounded-lg bg-color-error/15 border border-color-error/40 text-xs font-semibold text-color-error hover:border-color-error/60 transition-all flex items-center gap-1"
                >
                  <span>✗</span>
                  {skill}
                </span>
              ))}
            </div>
          </div>
        )}

        {!candidate.matchedSkill || (candidate.matchedSkill.length === 0 && (!candidate.missingSkills || candidate.missingSkills.length === 0)) && (
          <span className="text-xs text-dark-text-secondary italic">No skill data available</span>
        )}
      </div>

      {/* Expandable Section */}
      {isExpanded && (
        <div className="mt-4 pt-4 border-t border-dark-border/50 animate-fade-in">
          {/* Analysis */}
          {candidate.analysis && (
            <div className="mb-4">
              <p className="text-xs font-semibold text-dark-text-secondary mb-2 uppercase tracking-wide">Analysis</p>
              <p className="text-sm text-dark-text-secondary leading-relaxed">{candidate.analysis}</p>
            </div>
          )}

          {/* Match Reasoning */}
          {candidate.matchReasoning && (
            <div className="mb-4">
              <p className="text-xs font-semibold text-dark-text-secondary mb-2 uppercase tracking-wide">Match Reasoning</p>
              <p className="text-sm text-dark-text-secondary leading-relaxed">{candidate.matchReasoning}</p>
            </div>
          )}

          {/* Fit Analysis */}
          {candidate.fitAnalysis && (
            <div className="mb-4">
              <p className="text-xs font-semibold text-dark-text-secondary mb-2 uppercase tracking-wide">Fit Analysis</p>
              <p className="text-sm text-dark-text-secondary leading-relaxed">{candidate.fitAnalysis}</p>
            </div>
          )}

          {/* Contact Button */}
          <div className="flex gap-2 mt-4">
            <button className="flex-1 px-4 py-2 rounded-lg glass border border-glass-border text-dark-text font-semibold text-sm hover:border-gradient-purple/50 transition-all">
              Contact
            </button>
          </div>
        </div>
      )}

      {/* Expand Hint */}
      {!isExpanded && (
        <p className="text-xs text-dark-text-secondary text-center group-hover:text-gradient-purple transition-colors">
          Click to expand
        </p>
      )}
    </div>
  );
};

interface ModernCandidateResultsProps {
  candidates: CandidateProfile[];
  loading?: boolean;
}

const ModernCandidateResults: React.FC<ModernCandidateResultsProps> = ({ candidates }) => {
  if (!candidates || candidates.length === 0) {
    return null;
  }

  return (
    <div className="w-full">
      {/* Results Header */}
      <div className="mb-6 flex items-center justify-between flex-wrap gap-2">
        <div>
          <h2 className="text-2xl font-bold text-dark-text">
            👥 {candidates.length} Candidate{candidates.length !== 1 ? 's' : ''}
          </h2>
          <p className="text-sm text-dark-text-secondary">Ranked by match percentage</p>
        </div>
        <div className="glass rounded-xl px-4 py-2 border border-glass-border">
          <span className="text-sm font-semibold text-gradient-purple">{candidates.length} profile{candidates.length !== 1 ? 's' : ''}</span>
        </div>
      </div>

      {/* Cards Grid */}
      <div className="space-y-4 animate-fade-in w-full">
        {candidates.map((candidate, index) => (
          <CandidateCard key={index} candidate={candidate} rank={index + 1} />
        ))}
      </div>
    </div>
  );
};

export default ModernCandidateResults;
