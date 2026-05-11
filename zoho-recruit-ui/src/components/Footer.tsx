import React from 'react';

const TEAM_MEMBERS = [
  'Sudarshan Garg',
  'Saurabh Kumar',
  'Rupam Swain',
  'Suryaprakash Rao',
];

const Footer: React.FC = () => (
  <footer className="app-footer">
    <div className="footer-line footer-line-top">
      <span>Wissen Technology</span>
      <span className="footer-dot">·</span>
      <span className="footer-accent">AI Hackathon 2026</span>
      <span className="footer-dot">·</span>
      <span>AI-Powered Candidate Sourcing</span>
      <span className="footer-dot">·</span>
      <span>Built with <span className="footer-accent">Titan Transformers</span></span>
    </div>
    <div className="footer-line footer-line-team">
      {TEAM_MEMBERS.map((name, i) => (
        <React.Fragment key={name}>
          <span>{name}</span>
          {i < TEAM_MEMBERS.length - 1 && <span className="footer-dot">·</span>}
        </React.Fragment>
      ))}
    </div>
  </footer>
);

export default Footer;
