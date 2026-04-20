import React, { useRef } from 'react';

const SAMPLE_JDS: Record<string, string> = {
  java: `Senior Java Developer — 6-10 years experience.
Required: Java, Spring Boot, Microservices, Kafka, AWS, REST APIs.
Preferred: Docker, Kubernetes, Redis, BFSI domain.
Location: Bangalore. Hybrid work.
Responsibilities: Design and build distributed backend systems for capital markets platform.`,
  data: `Senior Data Engineer — 5-8 years experience.
Required: Python, Apache Spark, Airflow, SQL, Snowflake, dbt.
Preferred: Databricks, AWS Glue, Kafka, Scala.
Location: Hyderabad / Remote.
Responsibilities: Build and maintain scalable data pipelines.`,
  devops: `DevOps Lead — 7-12 years experience.
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
