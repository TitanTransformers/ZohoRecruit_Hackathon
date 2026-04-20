import React from 'react';

interface SidebarProps {
  activePage: string;
  onNavigate: (page: string) => void;
  stats: {
    apiCalls: number;
    candidatesFound: number;
    avgMatch: string;
    totalCost: string;
  };
}

const navItems = [
  { key: 'source', icon: '⚡', label: 'Source Candidates' },
  { key: 'past', icon: '📋', label: 'Past Searches' },
  { key: 'analytics', icon: '📊', label: 'Analytics' },
  { key: 'settings', icon: '⚙', label: 'Settings' },
];

const Sidebar: React.FC<SidebarProps> = ({ activePage, onNavigate, stats }) => {
  return (
    <aside className="sidebar">
      <div className="sidebar-label">Navigation</div>
      {navItems.map((item) => (
        <button
          key={item.key}
          className={`sidebar-nav-item ${activePage === item.key ? 'active' : ''}`}
          onClick={() => onNavigate(item.key)}
        >
          <span className="nav-icon">{item.icon}</span>
          {item.label}
        </button>
      ))}

      <div className="sidebar-spacer" />

      <div className="sidebar-stats">
        <div className="stats-title">Session Stats</div>
        <div className="stat-row">
          <span className="stat-label">API calls made</span>
          <span className="stat-value">{stats.apiCalls}</span>
        </div>
        <div className="stat-row">
          <span className="stat-label">Candidates found</span>
          <span className="stat-value">{stats.candidatesFound}</span>
        </div>
        <div className="stat-row">
          <span className="stat-label">Avg match %</span>
          <span className="stat-value">{stats.avgMatch}</span>
        </div>
        <div className="stat-row">
          <span className="stat-label">Total cost</span>
          <span className="stat-value">{stats.totalCost}</span>
        </div>
      </div>

      {/* Branding Footer */}
      <div className="sidebar-footer">
        <div className="sidebar-footer-text">Made by</div>
        <div className="sidebar-footer-brand">Titan Transformers</div>
      </div>
    </aside>
  );
};

export default Sidebar;
