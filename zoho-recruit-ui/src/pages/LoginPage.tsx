import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import './LoginPage.css';

const DUMMY_USERS = [
  { email: 'admin@wissen.com', password: 'admin123', name: 'Tony Stark', role: 'Administrator' },
  { email: 'recruiter@wissen.com', password: 'recruit123', name: 'Bruce Wayne', role: 'Senior Recruiter' },
  { email: 'demo@wissen.com', password: 'demo123', name: 'Peter Parker', role: 'Hiring Manager' },
];

const LoginPage: React.FC = () => {
  const navigate = useNavigate();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [showPassword, setShowPassword] = useState(false);
  const [activeQuickLogin, setActiveQuickLogin] = useState<number | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setIsLoading(true);

    // Simulate API delay
    await new Promise(r => setTimeout(r, 1200));

    const user = DUMMY_USERS.find(u => u.email === email && u.password === password);
    if (user) {
      localStorage.setItem('wissen_user', JSON.stringify(user));
      navigate('/');
    } else {
      setError('Invalid credentials. Try one of the quick login options below.');
    }
    setIsLoading(false);
  };

  const quickLogin = async (index: number) => {
    setActiveQuickLogin(index);
    const user = DUMMY_USERS[index];
    setEmail(user.email);
    setPassword(user.password);
    setError('');
    setIsLoading(true);

    await new Promise(r => setTimeout(r, 800));
    localStorage.setItem('wissen_user', JSON.stringify(user));
    navigate('/');
  };

  return (
    <div className="login-page">
      {/* Animated background */}
      <div className="login-bg">
        <div className="login-orb login-orb-1" />
        <div className="login-orb login-orb-2" />
        <div className="login-orb login-orb-3" />
        <div className="login-grid-overlay" />
      </div>

      {/* Main card */}
      <div className="login-container">
        {/* Left panel — Branding */}
        <div className="login-brand-panel">
          <div className="login-brand-content">
            {/* Wissen Logo */}
            <div className="login-logo-wrap">
              <div className="login-logo-icon">
                <svg width="48" height="48" viewBox="0 0 48 48" fill="none">
                  <rect width="48" height="48" rx="12" fill="url(#logoGrad)" />
                  <path d="M12 16L18 32L24 20L30 32L36 16" stroke="white" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" />                  <defs>
                    <linearGradient id="logoGrad" x1="0" y1="0" x2="48" y2="48">
                      <stop offset="0%" stopColor="#7C3AED" />
                      <stop offset="50%" stopColor="#2563EB" />
                      <stop offset="100%" stopColor="#0D9488" />
                    </linearGradient>
                  </defs>
                </svg>
              </div>
              <div>
                <h1 className="login-brand-name">Wissen</h1>
                <p className="login-brand-tagline">Intelligent AI ATS</p>
              </div>
            </div>

            <h2 className="login-hero-title">
              Intelligent Recruitment,<br />
              <span className="login-gradient-text">Powered by AI</span>
            </h2>
            <p className="login-hero-desc">
              Driving digital transformation in talent acquisition. Search, match, and rank candidates from Zoho Recruit with AI precision.
            </p>

            {/* Feature highlights */}
            <div className="login-features">
              <div className="login-feature">
                <span className="login-feature-icon">⚡</span>
                <div>
                  <div className="login-feature-title">AI-Powered Matching</div>
                  <div className="login-feature-desc">Smart candidate ranking using NLP</div>
                </div>
              </div>
              <div className="login-feature">
                <span className="login-feature-icon">🔍</span>
                <div>
                  <div className="login-feature-title">Deep ATS Search</div>
                  <div className="login-feature-desc">Integrated with Zoho Recruit</div>
                </div>
              </div>
              <div className="login-feature">
                <span className="login-feature-icon">📊</span>
                <div>
                  <div className="login-feature-title">Skill Analytics</div>
                  <div className="login-feature-desc">Detailed fit analysis per candidate</div>
                </div>
              </div>
            </div>

            <div className="login-brand-footer" style={{ flexDirection: 'column', alignItems: 'flex-start', gap: 8 }}>
              <div>
                <span>Made by </span>
                <span className="login-gradient-text" style={{ fontWeight: 700 }}>Titan Transformers</span>
                <span> · Wissen Hackathon 2026</span>
              </div>
              <div style={{ fontSize: 11, color: 'rgba(255,255,255,0.5)', display: 'flex', flexWrap: 'wrap', gap: 6, width: '100%' }}>
                <span>Sudarshan Garg</span>
                <span style={{ color: 'rgba(255,255,255,0.2)' }}>·</span>
                <span>Saurabh Kumar</span>
                <span style={{ color: 'rgba(255,255,255,0.2)' }}>·</span>
                <span>Rupam Swain</span>
                <span style={{ color: 'rgba(255,255,255,0.2)' }}>·</span>
                <span>Suryaprakash Rao</span>
              </div>
            </div>
          </div>
        </div>

        {/* Right panel — Login form */}
        <div className="login-form-panel">
          <div className="login-form-content">
            <div className="login-form-header">
              <h2 className="login-form-title">Welcome back</h2>
              <p className="login-form-subtitle">Sign in to access your recruitment dashboard</p>
            </div>

            <form onSubmit={handleSubmit} className="login-form">
              {/* Email */}
              <div className="login-field">
                <label className="login-label" htmlFor="email">Email Address</label>
                <div className="login-input-wrap">
                  <span className="login-input-icon">📧</span>
                  <input
                    id="email"
                    type="email"
                    className="login-input"
                    placeholder="you@wissen.com"
                    value={email}
                    onChange={e => setEmail(e.target.value)}
                    required
                    autoComplete="email"
                  />
                </div>
              </div>

              {/* Password */}
              <div className="login-field">
                <label className="login-label" htmlFor="password">Password</label>
                <div className="login-input-wrap">
                  <span className="login-input-icon">🔒</span>
                  <input
                    id="password"
                    type={showPassword ? 'text' : 'password'}
                    className="login-input"
                    placeholder="Enter your password"
                    value={password}
                    onChange={e => setPassword(e.target.value)}
                    required
                    autoComplete="current-password"
                  />
                  <button
                    type="button"
                    className="login-eye-btn"
                    onClick={() => setShowPassword(!showPassword)}
                    tabIndex={-1}
                  >
                    {showPassword ? '🙈' : '👁'}
                  </button>
                </div>
              </div>

              {/* Error */}
              {error && (
                <div className="login-error animate-fade-in">{error}</div>
              )}

              {/* Submit */}
              <button type="submit" className="login-submit" disabled={isLoading}>
                {isLoading ? (
                  <>
                    <div className="login-spinner" />
                    <span>Signing in...</span>
                  </>
                ) : (
                  <>
                    <span>Sign In</span>
                    <span>→</span>
                  </>
                )}
              </button>
            </form>

            {/* Divider */}
            <div className="login-divider">
              <span>Quick Login</span>
            </div>

            {/* Quick login cards */}
            <div className="login-quick-cards">
              {DUMMY_USERS.map((user, i) => (
                <button
                  key={i}
                  className={`login-quick-card ${activeQuickLogin === i ? 'active' : ''}`}
                  onClick={() => quickLogin(i)}
                  type="button"
                  disabled={isLoading}
                >
                  <div className="login-quick-avatar" style={{
                    background: i === 0 ? 'linear-gradient(135deg,#7C3AED,#4F46E5)' :
                      i === 1 ? 'linear-gradient(135deg,#0D9488,#0891B2)' :
                        'linear-gradient(135deg,#2563EB,#7C3AED)',
                  }}>
                    {user.name.split(' ').map(n => n[0]).join('')}
                  </div>
                  <div className="login-quick-info">
                    <div className="login-quick-name">{user.name}</div>
                    <div className="login-quick-role">{user.role}</div>
                  </div>
                  {activeQuickLogin === i && <div className="login-spinner" style={{ width: 16, height: 16 }} />}
                </button>
              ))}
            </div>

            <p className="login-footer-text">
              Protected by Wissen Enterprise Security
            </p>
          </div>
        </div>
      </div>
    </div>
  );
};

export default LoginPage;
