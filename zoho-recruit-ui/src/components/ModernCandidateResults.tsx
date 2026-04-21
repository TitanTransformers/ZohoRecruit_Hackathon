import React, { useState, useEffect, useRef } from 'react';
import type { CandidateProfile } from '../types/candidate';

const AVATAR_GRADIENTS = [
  'linear-gradient(135deg,#7C3AED,#4F46E5)',
  'linear-gradient(135deg,#0D9488,#0891B2)',
  'linear-gradient(135deg,#2563EB,#7C3AED)',
  'linear-gradient(135deg,#D97706,#DC2626)',
  'linear-gradient(135deg,#059669,#0D9488)',
];

/* ─── Single Candidate Card ──────────────────── */
interface CandidateCardProps {
  candidate: CandidateProfile;
  index: number;
}

const CandidateCard: React.FC<CandidateCardProps> = ({ candidate, index }) => {
  const [isExpanded, setIsExpanded] = useState(false);
  const [copiedField, setCopiedField] = useState<string | null>(null);
  const ringRef = useRef<SVGCircleElement>(null);

  const matchPct = candidate.matchPercentage ?? 0;
  const initials = candidate.name.split(' ').map(n => n[0]).join('').substring(0, 2);
  const grad = AVATAR_GRADIENTS[index % AVATAR_GRADIENTS.length];
  const ringColor = matchPct >= 80 ? '#10B981' : matchPct >= 60 ? '#F59E0B' : '#EF4444';

  const fitType = matchPct >= 80 ? 'strong' : matchPct >= 60 ? 'possible' : 'weak';
  const fitLabel = fitType === 'strong' ? 'Strong Fit' : fitType === 'possible' ? 'Possible Fit' : 'Weak Fit';
  const fitClass = `fit-badge fit-${fitType}`;

  // SVG ring math
  const r = 27;
  const circ = 2 * Math.PI * r;
  const targetOffset = circ - (matchPct / 100) * circ;

  // Animate ring on mount
  useEffect(() => {
    const el = ringRef.current;
    if (el) {
      const timer = setTimeout(() => {
        el.style.strokeDashoffset = String(targetOffset);
      }, 100 + index * 120);
      return () => clearTimeout(timer);
    }
  }, [targetOffset, index]);

  // Skills display
  const maxMatch = 4;
  const maxMiss = 2;
  const matchedSkills = (candidate.matchedSkills || []).slice(0, maxMatch);
  const missingSkills = (candidate.missingSkills || []).slice(0, maxMiss);
  const overflow = Math.max(0, (candidate.matchedSkills?.length || 0) - maxMatch) +
                   Math.max(0, (candidate.missingSkills?.length || 0) - maxMiss);

  const copyText = async (text: string, field: string) => {
    try {
      await navigator.clipboard.writeText(text);
      setCopiedField(field);
      setTimeout(() => setCopiedField(null), 2000);
    } catch { /* ignore */ }
  };

  // Derive role from matchReasoning or use a placeholder
  const role = candidate.matchReasoning
    ? candidate.matchReasoning.split('.')[0].substring(0, 40)
    : 'Software Engineer';

  return (
    <div
      className="candidate-card"
      style={{ animationDelay: `${index * 60}ms` }}
    >
      {/* Top section: avatar + info + ring */}
      <div className="card-top">
        <div className="avatar" style={{ background: grad }}>{initials}</div>
        <div className="card-info">
          <div className="card-name">{candidate.name}</div>
          <div className="card-role">{role}</div>
          {candidate.candidateId && (
            <div className="card-company">{candidate.candidateId}</div>
          )}
        </div>
        <div className="match-ring-wrap">
          <div className="match-ring">
            <svg width="64" height="64" viewBox="0 0 64 64">
              <circle cx="32" cy="32" r={r} fill="none" stroke="rgba(255,255,255,0.08)" strokeWidth="5" />
              <circle
                ref={ringRef}
                cx="32" cy="32" r={r}
                fill="none"
                stroke={ringColor}
                strokeWidth="5"
                strokeLinecap="round"
                strokeDasharray={String(circ)}
                strokeDashoffset={String(circ)}
                style={{ transition: `stroke-dashoffset 1s ease ${index * 120}ms` }}
              />
            </svg>
            <div className="match-ring-text" style={{ color: ringColor }}>{matchPct}%</div>
          </div>
          <span className={fitClass}>{fitLabel}</span>
        </div>
      </div>

      {/* Skills */}
      <div className="skills-row">
        {matchedSkills.map((s, i) => (
          <span key={`m-${i}`} className="skill-tag skill-match">{s}</span>
        ))}
        {missingSkills.map((s, i) => (
          <span key={`x-${i}`} className="skill-tag skill-miss">✕ {s}</span>
        ))}
        {overflow > 0 && (
          <span className="skill-tag" style={{
            background: 'var(--bg-elevated)',
            border: '1px solid var(--border)',
            color: 'var(--text-muted)',
          }}>+{overflow} more</span>
        )}
      </div>

      {/* Contact */}
      <div className="contact-row">
        <button
          className="contact-item"
          onClick={() => copyText(candidate.email, 'email')}
          type="button"
        >
          <span>📧</span>
          <span>{candidate.email}</span>
          <span className={`tooltip-copied ${copiedField === 'email' ? 'show' : ''}`}>Copied!</span>
        </button>
        {candidate.phone && (
          <button
            className="contact-item"
            onClick={() => copyText(candidate.phone!, 'phone')}
            type="button"
          >
            <span>📞</span>
            <span>{candidate.phone}</span>
            <span className={`tooltip-copied ${copiedField === 'phone' ? 'show' : ''}`}>Copied!</span>
          </button>
        )}
      </div>

      {/* Expand toggle */}
      <button
        className="expand-toggle"
        onClick={() => setIsExpanded(!isExpanded)}
        type="button"
      >
        <span>{isExpanded ? '▴' : '▾'}</span>
        {isExpanded ? ' Hide fit summary' : ' View fit summary'}
      </button>

      {/* Expanded content */}
      {isExpanded && (
        <div className="expand-content animate-fade-in-up">
          {candidate.fitAnalysis && (
            <p className="fit-summary">"{candidate.fitAnalysis}"</p>
          )}
          <div className="expand-meta">
            {candidate.skillMatchPercentage !== undefined && (
              <div className="expand-meta-item">
                Skill Match: <span>{candidate.skillMatchPercentage}%</span>
              </div>
            )}
            {candidate.experienceMatchPercentage !== undefined && (
              <div className="expand-meta-item">
                Experience Match: <span>{candidate.experienceMatchPercentage}%</span>
              </div>
            )}
            {candidate.rankPosition !== undefined && (
              <div className="expand-meta-item">
                Rank: <span>#{candidate.rankPosition}</span>
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
};

/* ─── Results Container ──────────────────────── */
interface ModernCandidateResultsProps {
  candidates: CandidateProfile[];
}

const ModernCandidateResults: React.FC<ModernCandidateResultsProps> = ({ candidates }) => {
  const [filter, setFilter] = useState('all');

  if (!candidates || candidates.length === 0) return null;

  const filtered = filter === 'all'
    ? candidates
    : candidates.filter(c => {
        const pct = c.matchPercentage ?? 0;
        if (filter === 'strong') return pct >= 80;
        if (filter === 'possible') return pct >= 60 && pct < 80;
        if (filter === 'weak') return pct < 60;
        return true;
      });

  const exportJSON = () => {
    const blob = new Blob([JSON.stringify(candidates, null, 2)], { type: 'application/json' });
    const a = document.createElement('a');
    a.href = URL.createObjectURL(blob);
    a.download = 'candidates.json';
    a.click();
  };

  const exportCSV = () => {
    const headers = ['Name', 'Email', 'Phone', 'Match%', 'Fit', 'Matched Skills', 'Missing Skills'];
    const rows = candidates.map(c => [
      c.name, c.email, c.phone || '',
      `${c.matchPercentage ?? 0}%`,
      (c.matchPercentage ?? 0) >= 80 ? 'Strong' : (c.matchPercentage ?? 0) >= 60 ? 'Possible' : 'Weak',
      (c.matchedSkills || []).join(';'),
      (c.missingSkills || []).join(';'),
    ]);
    const csv = [headers, ...rows].map(r => r.join(',')).join('\n');
    const blob = new Blob([csv], { type: 'text/csv' });
    const a = document.createElement('a');
    a.href = URL.createObjectURL(blob);
    a.download = 'candidates.csv';
    a.click();
  };

  return (
    <div>
      {/* Header */}
      <div className="results-header">
        <div>
          <h2 className="results-title">{candidates.length} Candidates Found</h2>
          <p className="results-subtitle">Ranked by AI match score</p>
        </div>
        <div className="results-actions">
          <button className="ghost-btn" onClick={exportJSON} type="button">↓ Export JSON</button>
          <button className="ghost-btn" onClick={exportCSV} type="button">↓ Export CSV</button>
          <select
            className="filter-select"
            value={filter}
            onChange={(e) => setFilter(e.target.value)}
          >
            <option value="all">All Candidates</option>
            <option value="strong">Strong Fit</option>
            <option value="possible">Possible Fit</option>
            <option value="weak">Weak Fit</option>
          </select>
        </div>
      </div>

      {/* Grid */}
      <div className="results-grid">
        {filtered.map((candidate, index) => (
          <CandidateCard
            key={candidate.candidateId || index}
            candidate={candidate}
            index={index}
          />
        ))}
      </div>

      {filtered.length === 0 && (
        <div className="empty-state">
          <div className="empty-icon">🔍</div>
          <div className="empty-title">No candidates match this filter</div>
          <div className="empty-sub">Try selecting a different filter option</div>
        </div>
      )}
    </div>
  );
};

export default ModernCandidateResults;
