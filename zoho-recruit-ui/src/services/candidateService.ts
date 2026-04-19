/**
 * Candidate Service
 * Handles all API calls related to candidate search and profile retrieval
 */

import type { CandidateProfile, ApiResponse } from '../types/candidate';
import { config, getApiUrl, debugLog } from '../config/environment';

class CandidateService {
  /**
   * Search candidates using text (Job Description)
   * @param message - Job description text
   * @returns Promise with array of candidate profiles
   */
  async searchByText(message: string): Promise<CandidateProfile[]> {
    if (!message.trim()) {
      throw new Error('Job description cannot be empty');
    }

    debugLog('Searching candidates by text', { messageLength: message.length });

    try {
      const url = getApiUrl(config.apiChatEndpoint);
      const response = await fetch(url, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ message }),
      });

      return this.handleResponse(response);
    } catch (error) {
      this.handleError(error);
    }
  }

  /**
   * Search candidates using document (PDF) with optional text
   * @param file - PDF file
   * @param message - Optional job description text
   * @returns Promise with array of candidate profiles
   */
  async searchByDocument(file: File, message?: string): Promise<CandidateProfile[]> {
    if (!file) {
      throw new Error('PDF file is required');
    }

    if (file.type !== 'application/pdf') {
      throw new Error('Only PDF files are supported');
    }

    debugLog('Searching candidates by document', { fileName: file.name, fileSize: file.size });

    try {
      const formData = new FormData();

      if (message?.trim()) {
        formData.append('message', message);
      }

      formData.append('pdf', file);

      const url = getApiUrl(config.apiDocumentsEndpoint);
      const response = await fetch(url, {
        method: 'POST',
        body: formData,
      });

      return this.handleResponse(response);
    } catch (error) {
      this.handleError(error);
    }
  }

  /**
   * Handle API response
   * @param response - Fetch response object
   * @returns Promise with parsed candidate profiles
   */
  private async handleResponse(response: Response): Promise<CandidateProfile[]> {
    if (!response.ok) {
      throw new Error(`API Error: ${response.status} ${response.statusText}`);
    }

    const data: ApiResponse<CandidateProfile[]> = await response.json();
    debugLog('API Response received', data);

    // Support multiple response formats
    const candidates = this.extractCandidates(data);

    if (!candidates || candidates.length === 0) {
      throw new Error('No candidate profiles found matching the job description');
    }

    return this.validateAndNormalizeCandidates(candidates);
  }

  /**
   * Extract candidates from various API response formats
   * @param data - API response data
   * @returns Array of candidates or null
   */
  private extractCandidates(data: ApiResponse<CandidateProfile[]>): CandidateProfile[] | null {
    // Check for direct array response
    if (Array.isArray(data)) {
      return data as CandidateProfile[];
    }

    // Check for nested array in various properties
    if (data.results && Array.isArray(data.results)) {
      return data.results;
    }

    if (data.candidates && Array.isArray(data.candidates)) {
      return data.candidates;
    }

    if (data.data && Array.isArray(data.data)) {
      return data.data;
    }

    return null;
  }

  /**
   * Validate and normalize candidate data
   * @param candidates - Array of candidate profiles
   * @returns Validated candidate profiles
   */
  private validateAndNormalizeCandidates(candidates: CandidateProfile[]): CandidateProfile[] {
    return candidates.map((candidate) => {
      return {
        name: candidate.name || 'N/A',
        email: candidate.email || 'N/A',
        matchedSkill: Array.isArray(candidate.matchedSkill) ? candidate.matchedSkill : [],
        missingSkills: Array.isArray(candidate.missingSkills) ? candidate.missingSkills : [],
        analysis: candidate.analysis || 'No analysis available',
        matchedPercentage: typeof candidate.matchedPercentage === 'number' 
          ? Math.min(100, Math.max(0, candidate.matchedPercentage))
          : 0,
      };
    });
  }

  /**
   * Handle API errors
   * @param error - Error object
   */
  private handleError(error: unknown): never {
    const errorMessage = error instanceof Error ? error.message : 'An unknown error occurred';
    debugLog('API Error', errorMessage);
    throw new Error(errorMessage);
  }
}

export const candidateService = new CandidateService();
