/**
 * Candidate Response Type
 * Represents the API response structure for a candidate profile
 */
export interface CandidateProfile {
  name: string;
  email: string;
  matchedSkill: string[];
  missingSkills: string[];
  analysis: string;
  matchedPercentage: number;
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
 * API Response Type
 * Generic wrapper for API responses
 */
export interface ApiResponse<T> {
  data?: T;
  results?: T;
  candidates?: T;
  error?: string;
  message?: string;
  status?: number;
}
