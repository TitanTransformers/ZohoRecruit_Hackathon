import React, { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';

const THEMES = [
  { key: 'dark', label: 'Dark', icon: '🌙', color: '#27272A' },
  { key: 'light', label: 'Light', icon: '☀️', color: '#F4F4F5' },
  { key: 'ocean', label: 'Ocean', icon: '🌊', color: '#0F1F3D' },
  { key: 'sunset', label: 'Sunset', icon: '🌅', color: '#2D1212' },
  { key: 'forest', label: 'Forest', icon: '🌲', color: '#122212' },
];

interface ModernNavbarProps {
}

const ModernNavbar: React.FC<ModernNavbarProps> = () => {
  const navigate = useNavigate();
  const [currentTheme, setCurrentTheme] = useState('dark');
  const [isThemeOpen, setIsThemeOpen] = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);

  // Get logged in user
  const userStr = localStorage.getItem('wissen_user');
  const user = userStr ? JSON.parse(userStr) : null;

  // Restore saved theme
  useEffect(() => {
    const saved = localStorage.getItem('wissen-theme') || 'dark';
    setCurrentTheme(saved);
    applyTheme(saved);
  }, []);

  // Close dropdown on outside click
  useEffect(() => {
    const handleClick = (e: MouseEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(e.target as Node)) {
        setIsThemeOpen(false);
      }
    };
    document.addEventListener('mousedown', handleClick);
    return () => document.removeEventListener('mousedown', handleClick);
  }, []);

  const applyTheme = (theme: string) => {
    if (theme === 'dark') {
      document.documentElement.removeAttribute('data-theme');
    } else {
      document.documentElement.setAttribute('data-theme', theme);
    }
  };

  const selectTheme = (themeKey: string) => {
    setCurrentTheme(themeKey);
    applyTheme(themeKey);
    localStorage.setItem('wissen-theme', themeKey);
    setIsThemeOpen(false);
  };

  const handleLogout = () => {
    localStorage.removeItem('wissen_user');
    navigate('/login');
  };

  const activeTheme = THEMES.find(t => t.key === currentTheme) || THEMES[0];

  return (
    <nav className="navbar">
      <div className="nav-logo">
        <div className="nav-logo-icon">✦</div>
        <span className="nav-logo-text">Wissen Recruit Ninja</span>
      </div>
      <div className="nav-right">


        {/* Theme Dropdown */}
        <div className="theme-dropdown-wrap" ref={dropdownRef}>
          <button
            className="theme-selector"
            onClick={() => setIsThemeOpen(!isThemeOpen)}
            type="button"
          >
            <span>{activeTheme.icon}</span>
            <span>{activeTheme.label}</span>
            <span style={{ fontSize: 10 }}>▾</span>
          </button>
          {isThemeOpen && (
            <div className="theme-dropdown">
              {THEMES.map(theme => (
                <button
                  key={theme.key}
                  className={`theme-option ${currentTheme === theme.key ? 'active' : ''}`}
                  onClick={() => selectTheme(theme.key)}
                  type="button"
                >
                  <div className="theme-dot" style={{ background: theme.color }} />
                  <span>{theme.icon}</span>
                  <span>{theme.label}</span>
                </button>
              ))}
            </div>
          )}
        </div>


        <div className="hackathon-pill">AI Hackathon 2026</div>

        {/* User & Logout */}
        {user && (
          <>
            <span style={{
              fontSize: 12, color: 'var(--text-secondary)', fontWeight: 500,
              maxWidth: 120, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' as const,
            }}>
              {user.name}
            </span>
            <button className="nav-btn" onClick={handleLogout} type="button" title="Sign Out">
              ↪ Logout
            </button>
          </>
        )}
      </div>
    </nav>
  );
};

export default ModernNavbar;

