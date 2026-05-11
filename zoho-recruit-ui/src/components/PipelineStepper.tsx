import React from 'react';

interface PipelineStepperProps {
  activeStep: 'idle' | 'parse' | 'query' | 'search' | 'rank';
  logMessage?: string;
}

const steps = [
  { key: 'parse', label: 'Parsing JD', icon: '🧠' },
  { key: 'query', label: 'Generating Queries', icon: '🔍' },
  { key: 'search', label: 'Searching Zoho', icon: '📡' },
  { key: 'rank', label: 'Ranking Candidates', icon: '🏆' },
];

const stepOrder: Record<string, number> = {
  idle: -1,
  parse: 0,
  query: 1,
  search: 2,
  rank: 3,
};

const PIPELINE_LOGS: Record<string, string> = {
  parse: '🧠 Parsing JD...',
  query: '🔍 Generated search strategies',
  search: '📡 Searching Zoho Recruit...',
  rank: '✅ Completed',
};

const CheckSvg: React.FC<{ index: number }> = ({ index }) => (
  <svg width="22" height="22" viewBox="0 0 22 22" fill="none">
    <polyline
      points="4,11 9,16 18,6"
      stroke={`url(#ckGrad${index})`}
      strokeWidth="2.5"
      strokeLinecap="round"
      strokeLinejoin="round"
      strokeDasharray="24"
      strokeDashoffset="24"
      style={{ animation: 'checkDraw .4s ease .1s forwards' }}
    />
    <defs>
      <linearGradient id={`ckGrad${index}`} x1="4" y1="11" x2="18" y2="6" gradientUnits="userSpaceOnUse">
        <stop offset="0%" stopColor="#A78BFA" />
        <stop offset="100%" stopColor="#5EEAD4" />
      </linearGradient>
    </defs>
  </svg>
);

const PipelineStepper: React.FC<PipelineStepperProps> = ({ activeStep }) => {
  const currentStepIndex = stepOrder[activeStep];

  // Pick the most recent relevant log message
  let logMsg = 'Initializing pipeline...';
  if (activeStep !== 'idle') {
    logMsg = PIPELINE_LOGS[activeStep] || logMsg;
  }

  return (
    <div className="glass-card pipeline-card">
      <div className="pipeline-header">Search Pipeline</div>
      <div className="pipeline-steps">
        {steps.map((step, index) => {
          const isActive = currentStepIndex === index;
          const isCompleted = currentStepIndex > index;

          let nodeClass = 'step-node waiting';
          let labelClass = 'step-label';
          if (isCompleted) {
            nodeClass = 'step-node done';
            labelClass = 'step-label done';
          } else if (isActive) {
            nodeClass = 'step-node active';
            labelClass = 'step-label active';
          }

          return (
            <React.Fragment key={step.key}>
              <div className="pipeline-step">
                <div className={nodeClass}>
                  <span className="step-icon-inner">
                    {isCompleted ? <CheckSvg index={index} /> : step.icon}
                  </span>
                </div>
                <div className={labelClass}>{step.label}</div>
              </div>

              {index < steps.length - 1 && (
                <div className={`step-connector ${isCompleted ? 'done' : ''}`} />
              )}
            </React.Fragment>
          );
        })}
      </div>
      <div className="pipeline-log">
        <span style={{ color: activeStep === 'idle' ? 'var(--text-muted)' : undefined }}>
          → {logMsg}
        </span>
      </div>
    </div>
  );
};

export default PipelineStepper;
