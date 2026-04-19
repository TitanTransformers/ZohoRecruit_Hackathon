/**
 * Candidate Response Type
 * Represents the API response structure for a candidate profile
 */
export interface CandidateProfile {
  candidateId?: string;
  name: string;
  email: string;
  phone?: string;
  matchPercentage?: number;
  skillMatchPercentage?: number;
  experienceMatchPercentage?: number;
  rankPosition?: number;
  matchedSkills?: string[];
  matchedSkill?: string[]; // Legacy support
  missingSkills: string[];
  matchReasoning?: string;
  fitAnalysis?: string;
  analysis?: string; // Legacy support
  matchedPercentage?: number; // Legacy support
}

/**
 * Search Request Type
 * Represents the payload sent to the API
 */
export interface SearchRequest {
  message?: string;
  pdf?: File;
}

/**
 * Pagination metadata
 */
export interface PaginationInfo {
  content: CandidateProfile[];
  page: number;
  page_size: number;
  total_pages: number;
  total_items: number;
  has_next: boolean;
  has_previous: boolean;
}

/**
 * API Response Type
 * Generic wrapper for API responses
 */
export interface ApiResponse<T> {
  response?: T | null;
  pagination?: PaginationInfo;
  data?: T;
  results?: T;
  candidates?: T;
  error?: string;
  message?: string;
  status?: number;
  tools_used?: string | null;
  timestamp?: number;
}
