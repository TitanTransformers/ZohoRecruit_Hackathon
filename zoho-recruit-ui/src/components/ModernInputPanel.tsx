import React, { useRef } from 'react';

const SAMPLE_JDS: Record<string, string> = {
  java: `Fetch top 5 candidates for below Job Description 

Wissen Technology is hiring a seasoned Java Developer to join our high-performing engineering team. This role demands strong technical expertise, ownership, and a passion for building scalable enterprise-grade applications from the ground up.

Experience: 5+ years  
Location: Bangalore

Key Responsibilities

• Experience in scalable Java applications.

• Collaborate with cross-functional teams to define, design, and deliver new features.

• Solve complex technical problems with innovative, simple solutions.

• Write clean, efficient, and well-documented code.

• Participate in code reviews to maintain code quality.

• Continuously discover, evaluate, and implement new technologies to maximize development efficiency.

• Exhibit ownership and responsibility for assigned deliverables.

Qualifications and Required Skills

• 5+ years of hands-on Java development experience.

• Strong experience in building products or applications from scratch (not just maintenance/support).

• Good understanding of object-oriented programming principles.

• Strong knowledge of data structures, algorithms, and design patterns.

• Should have strong problem-solving abilities.

• Familiarity with RESTful APIs and microservices architecture is a plus.

• Strong debugging and troubleshooting skills.

Good to Have Skills

• Excellent problem-solving skills and logical reasoning.

• High levels of ownership, accountability, and self-drive.

• Strong communication skills and a collaborative approach.

• Proactive attitude and high standard of work ethics.

• Evidence of recognitions or awards in previous roles.

• Commitment to continuous learning and improvement.`,
  data: `Senior Data Engineer — 5-8 years experience.
Required: Python, Apache Spark, Airflow, SQL, Snowflake, dbt.
Preferred: Databricks, AWS Glue, Kafka, Scala.
Location: Hyderabad / Remote.
Responsibilities: Build and maintain scalable data pipelines.`,
  devops: `DevOps Lead — 17-12 years experience.
Required: Kubernetes, Terraform, Jenkins, AWS/GCP, Docker, Helm.
Preferred: ArgoCD, Prometheus, Grafana, Python scripting, SRE practices.
Location: Pune. On-site preferred.
Responsibilities: Own CI/CD infrastructure for 30+ microservices.`,
};

interface ModernInputPanelProps {
  text: string;
  pdfFile: File | null;
  loading: boolean;
  error: string | null;
  onTextChange: (text: string) => void;
  onFileChange: (file: File | null) => void;
  onSubmit: () => void;
  onReset: () => void;
  maxTextLength?: number;
}

const ModernInputPanel: React.FC<ModernInputPanelProps> = ({
  text,
  pdfFile,
  loading,
  error,
  onTextChange,
  onFileChange,
  onSubmit,
  onReset,
}) => {
  const fileInputRef = useRef<HTMLInputElement>(null);

  const charCount = text.length;
  const wordCount = text.trim() ? text.trim().split(/\s+/).length : 0;
  const hasInput = text.trim().length > 0 || pdfFile !== null;

  const handleTextChange = (e: React.ChangeEvent<HTMLTextAreaElement>) => {
    onTextChange(e.target.value);
  };

  const handleFileSelect = (file: File | null) => {
    if (file && file.type === 'application/pdf') {
      onFileChange(file);
    } else if (file) {
      // Not a PDF
      onFileChange(null);
    }
  };

  const quickFill = (type: string) => {
    const jd = SAMPLE_JDS[type];
    if (jd) {
      onTextChange(jd);
    }
  };

  return (
    <>
      <div className="glass-card jd-panel">
        <div className="panel-header">
          <h2 className="panel-title">Paste Job Description</h2>
          <span className="panel-subtitle">Supports .txt .pdf .docx</span>
        </div>

        <textarea
          className="jd-textarea"
          value={text}
          onChange={handleTextChange}
          disabled={loading}
          placeholder={`Senior Java Developer — 6-10 years\nSkills: Spring Boot, Microservices, Kafka, AWS\nDomain: BFSI / Capital Markets\nLocation: Bangalore / Hybrid`}
        />

        <div className="textarea-meta">
          <span>{charCount} characters</span>
          <span>{wordCount} words</span>
        </div>

        <div className="quick-row">
          <button
            className="upload-chip"
            onClick={() => fileInputRef.current?.click()}
            type="button"
          >
            {pdfFile ? `📄 ${pdfFile.name}` : '+ Upload JD file'}
          </button>
          <input
            ref={fileInputRef}
            type="file"
            accept=".pdf"
            onChange={(e) => handleFileSelect(e.target.files?.[0] || null)}
            style={{ display: 'none' }}
          />
          <div className="quick-btns">
            <button className="quick-btn" onClick={() => quickFill('java')} type="button">Java Backend</button>
            <button className="quick-btn" onClick={() => quickFill('data')} type="button">Data Engineer</button>
            <button className="quick-btn" onClick={() => quickFill('devops')} type="button">DevOps Lead</button>
          </div>
        </div>

        <button
          className="submit-btn"
          onClick={onSubmit}
          disabled={loading || !hasInput}
          type="button"
        >
          {loading && <div className="btn-spinner" />}
          <span>{loading ? 'Searching Zoho Recruit...' : '⚡ Find Best Candidates'}</span>
        </button>
      </div>

      {error && (
        <div className="error-alert">{error}</div>
      )}

      {pdfFile && (
        <div style={{ marginBottom: 16 }}>
          <button
            className="ghost-btn"
            onClick={() => {
              onFileChange(null);
              onReset();
            }}
            type="button"
          >
            ✕ Remove {pdfFile.name}
          </button>
        </div>
      )}
    </>
  );
};

export default ModernInputPanel;
