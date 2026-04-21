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

  // Phone: prefer mobile (new API), fallback to phone (legacy)
  const phoneNumber = candidate.mobile || candidate.phone;

  const copyText = async (text: string, field: string) => {
    try {
      await navigator.clipboard.writeText(text);
      setCopiedField(field);
      setTimeout(() => setCopiedField(null), 2000);
    } catch { /* ignore */ }
  };

  // All matched/missing skills (no truncation — shown in expanded)
  const allMatchedSkills = candidate.matchedSkills || [];
  const allMissingSkills = candidate.missingSkills || [];

  // In collapsed view show limited skills
  const previewMatched = allMatchedSkills.slice(0, 4);
  const previewMissing = allMissingSkills.slice(0, 2);
  const hiddenCount = Math.max(0, allMatchedSkills.length - 4) + Math.max(0, allMissingSkills.length - 2);

  return (
    <div className="candidate-card" style={{ animationDelay: `${index * 60}ms` }}>

      {/* ── Rank badge ── */}
      {candidate.rankPosition != null && (
        <div style={{
          display: 'flex', alignItems: 'center', justifyContent: 'space-between',
          marginBottom: 12,
        }}>
          <span style={{
            fontSize: 11, fontWeight: 700, color: 'var(--accent-purple)',
            background: 'rgba(124,58,237,0.1)', border: '1px solid rgba(124,58,237,0.25)',
            borderRadius: 6, padding: '2px 8px', letterSpacing: '.3px',
          }}>
            #{candidate.rankPosition}
          </span>
          {candidate.candidateId && candidate.candidateId !== 'N/A' && (
            <span style={{ fontSize: 11, color: 'var(--text-muted)', fontFamily: 'monospace' }}>
              {candidate.candidateId}
            </span>
          )}
        </div>
      )}

      {/* ── Top: avatar + info + ring ── */}
      <div className="card-top">
        <div className="avatar" style={{ background: grad }}>{initials}</div>
        <div className="card-info">
          <div className="card-name">{candidate.name}</div>
          {/* matchReasoning as subtitle */}
          {candidate.matchReasoning && (
            <div className="card-role" title={candidate.matchReasoning}>
              {candidate.matchReasoning.substring(0, 48)}{candidate.matchReasoning.length > 48 ? '…' : ''}
            </div>
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

      {/* ── Skill/Experience sub-scores ── */}
      <div style={{ display: 'flex', gap: 8, marginBottom: 12, flexWrap: 'wrap' as const }}>
        {candidate.skillMatchPercentage != null && (
          <span style={{
            fontSize: 11, color: 'var(--text-secondary)',
            background: 'var(--bg-elevated)', borderRadius: 6,
            padding: '3px 8px', border: '1px solid var(--border)',
          }}>
            🎯 Skill <strong style={{ color: '#10B981' }}>{candidate.skillMatchPercentage}%</strong>
          </span>
        )}
        {candidate.experienceMatchPercentage != null && (
          <span style={{
            fontSize: 11, color: 'var(--text-secondary)',
            background: 'var(--bg-elevated)', borderRadius: 6,
            padding: '3px 8px', border: '1px solid var(--border)',
          }}>
            📅 Exp <strong style={{ color: '#3B82F6' }}>{candidate.experienceMatchPercentage}%</strong>
          </span>
        )}
      </div>

      {/* ── Collapsed skills preview ── */}
      <div className="skills-row">
        {previewMatched.map((s, i) => (
          <span key={`m-${i}`} className="skill-tag skill-match">✓ {s}</span>
        ))}
        {previewMissing.map((s, i) => (
          <span key={`x-${i}`} className="skill-tag skill-miss">✕ {s}</span>
        ))}
        {hiddenCount > 0 && (
          <span className="skill-tag" style={{
            background: 'var(--bg-elevated)',
            border: '1px solid var(--border)',
            color: 'var(--text-muted)',
          }}>+{hiddenCount} more</span>
        )}
      </div>

      {/* ── Contact ── */}
      <div className="contact-row">
        {candidate.email && candidate.email !== 'N/A' && (
          <button
            className="contact-item"
            onClick={() => copyText(candidate.email, 'email')}
            type="button"
          >
            <span>📧</span>
            <span>{candidate.email}</span>
            <span className={`tooltip-copied ${copiedField === 'email' ? 'show' : ''}`}>Copied!</span>
          </button>
        )}
        {phoneNumber && phoneNumber !== 'N/A' && (
          <button
            className="contact-item"
            onClick={() => copyText(phoneNumber, 'phone')}
            type="button"
          >
            <span>📞</span>
            <span>{phoneNumber}</span>
            <span className={`tooltip-copied ${copiedField === 'phone' ? 'show' : ''}`}>Copied!</span>
          </button>
        )}
      </div>

      {/* ── Expand toggle ── */}
      <button
        className="expand-toggle"
        onClick={() => setIsExpanded(!isExpanded)}
        type="button"
      >
        <span>{isExpanded ? '▴' : '▾'}</span>
        {isExpanded ? ' Hide details' : ' View full analysis'}
      </button>

      {/* ── Expanded content ── */}
      {isExpanded && (
        <div className="expand-content animate-fade-in-up">

          {/* fitAnalysis */}
          {candidate.fitAnalysis && (
            <div style={{ marginBottom: 14 }}>
              <div style={{
                fontSize: 11, fontWeight: 600, textTransform: 'uppercase' as const,
                letterSpacing: '.8px', color: 'var(--text-muted)', marginBottom: 6,
              }}>
                Fit Analysis
              </div>
              <p className="fit-summary">"{candidate.fitAnalysis}"</p>
            </div>
          )}

          {/* matchReasoning */}
          {candidate.matchReasoning && (
            <div style={{ marginBottom: 14 }}>
              <div style={{
                fontSize: 11, fontWeight: 600, textTransform: 'uppercase' as const,
                letterSpacing: '.8px', color: 'var(--text-muted)', marginBottom: 6,
              }}>
                Match Reasoning
              </div>
              <p style={{ fontSize: 13, color: 'var(--text-secondary)', lineHeight: 1.6 }}>
                {candidate.matchReasoning}
              </p>
            </div>
          )}

          {/* All matched skills */}
          {allMatchedSkills.length > 0 && (
            <div style={{ marginBottom: 12 }}>
              <div style={{
                fontSize: 11, fontWeight: 600, textTransform: 'uppercase' as const,
                letterSpacing: '.8px', color: 'var(--text-muted)', marginBottom: 6,
              }}>
                Matched Skills ({allMatchedSkills.length})
              </div>
              <div style={{ display: 'flex', flexWrap: 'wrap' as const, gap: 6 }}>
                {allMatchedSkills.map((s, i) => (
                  <span key={i} className="skill-tag skill-match">✓ {s}</span>
                ))}
              </div>
            </div>
          )}

          {/* All missing skills */}
          {allMissingSkills.length > 0 && (
            <div style={{ marginBottom: 12 }}>
              <div style={{
                fontSize: 11, fontWeight: 600, textTransform: 'uppercase' as const,
                letterSpacing: '.8px', color: 'var(--text-muted)', marginBottom: 6,
              }}>
                Missing Skills ({allMissingSkills.length})
              </div>
              <div style={{ display: 'flex', flexWrap: 'wrap' as const, gap: 6 }}>
                {allMissingSkills.map((s, i) => (
                  <span key={i} className="skill-tag skill-miss">✕ {s}</span>
                ))}
              </div>
            </div>
          )}

          {/* Score breakdown */}
          <div className="expand-meta">
            {candidate.matchPercentage != null && (
              <div className="expand-meta-item">
                Overall Match: <span>{candidate.matchPercentage}%</span>
              </div>
            )}
            {candidate.skillMatchPercentage != null && (
              <div className="expand-meta-item">
                Skill Match: <span>{candidate.skillMatchPercentage}%</span>
              </div>
            )}
            {candidate.experienceMatchPercentage != null && (
              <div className="expand-meta-item">
                Experience: <span>{candidate.experienceMatchPercentage}%</span>
              </div>
            )}
            {candidate.rankPosition != null && (
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
    const headers = ['Rank', 'Name', 'Email', 'Mobile', 'Match%', 'Skill%', 'Exp%', 'Fit', 'Matched Skills', 'Missing Skills', 'Fit Analysis'];
    const rows = candidates.map(c => [
      c.rankPosition ?? '',
      c.name,
      c.email,
      c.mobile || c.phone || '',
      `${c.matchPercentage ?? 0}%`,
      `${c.skillMatchPercentage ?? 0}%`,
      `${c.experienceMatchPercentage ?? 0}%`,
      (c.matchPercentage ?? 0) >= 80 ? 'Strong' : (c.matchPercentage ?? 0) >= 60 ? 'Possible' : 'Weak',
      (c.matchedSkills || []).join(';'),
      (c.missingSkills || []).join(';'),
      (c.fitAnalysis || '').replace(/,/g, ' '),
    ]);
    const csv = [headers, ...rows].map(r => r.join(',')).join('\n');
    const blob = new Blob([csv], { type: 'text/csv' });
    const a = document.createElement('a');
    a.href = URL.createObjectURL(blob);
    a.download = 'candidates.csv';
    a.click();
  };

  // Summary stats
  const strongCount = candidates.filter(c => (c.matchPercentage ?? 0) >= 80).length;
  const possibleCount = candidates.filter(c => { const p = c.matchPercentage ?? 0; return p >= 60 && p < 80; }).length;

  return (
    <div>
      {/* Header */}
      <div className="results-header">
        <div>
          <h2 className="results-title">{candidates.length} Candidates Ranked</h2>
          <p className="results-subtitle">
            <span style={{ color: '#10B981', fontWeight: 600 }}>{strongCount} strong</span>
            {' · '}
            <span style={{ color: '#F59E0B', fontWeight: 600 }}>{possibleCount} possible</span>
            {' · '}
            {candidates.length - strongCount - possibleCount} weak fits
          </p>
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
            <option value="strong">Strong Fit (≥80%)</option>
            <option value="possible">Possible Fit (60–79%)</option>
            <option value="weak">Weak Fit (&lt;60%)</option>
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
