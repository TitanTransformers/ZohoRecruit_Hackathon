import React from 'react';

interface PipelineStepperProps {
  activeStep: 'idle' | 'parse' | 'query' | 'search' | 'rank';
}

const steps = [
  { key: 'parse', label: 'Parse', icon: '📄' },
  { key: 'query', label: 'Query', icon: '🔎' },
  { key: 'search', label: 'Search', icon: '⚡' },
  { key: 'rank', label: 'Rank', icon: '🏆' },
];

const stepOrder: Record<string, number> = {
  idle: -1,
  parse: 0,
  query: 1,
  search: 2,
  rank: 3,
};

const PipelineStepper: React.FC<PipelineStepperProps> = ({ activeStep }) => {
  const currentStepIndex = stepOrder[activeStep];

  return (
    <div className="w-full glass rounded-xl p-6 border border-glass-border">
      <div className="flex items-center justify-between">
        {steps.map((step, index) => {
          const isActive = currentStepIndex === index;
          const isCompleted = currentStepIndex > index;

          return (
            <React.Fragment key={step.key}>
              {/* Step */}
              <div className="flex flex-col items-center flex-1">
                <div
                  className={`
                    w-12 h-12 rounded-full flex items-center justify-center text-xl
                    transition-all duration-300
                    ${
                      isCompleted
                        ? 'bg-gradient-to-br from-color-success to-color-success scale-110 animate-pulse'
                        : isActive
                          ? 'bg-gradient-to-br from-gradient-purple to-gradient-blue scale-110 shadow-lg animate-glow'
                          : 'bg-dark-bg-secondary border border-dark-border'
                    }
                  `}
                >
                  {isCompleted ? '✓' : step.icon}
                </div>
                <p
                  className={`
                    text-xs font-semibold mt-2 transition-colors duration-300
                    ${
                      isActive || isCompleted
                        ? 'text-gradient-purple'
                        : 'text-dark-text-secondary'
                    }
                  `}
                >
                  {step.label}
                </p>
              </div>

              {/* Connector */}
              {index < steps.length - 1 && (
                <div
                  className={`
                    h-1 flex-1 mx-2 rounded-full transition-all duration-300
                    ${isCompleted ? 'bg-gradient-to-r from-color-success to-color-success' : 'bg-dark-border'}
                  `}
                  style={{
                    height: isCompleted || isActive ? '3px' : '1px',
                  }}
                />
              )}
            </React.Fragment>
          );
        })}
      </div>
    </div>
  );
};

export default PipelineStepper;
