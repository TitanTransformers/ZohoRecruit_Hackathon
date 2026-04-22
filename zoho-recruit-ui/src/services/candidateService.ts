/**
 * Candidate Service
 * Handles all API calls related to candidate search and profile retrieval
 */

import type { CandidateProfile, ApiResponse, SearchResponse } from '../types/candidate';
import { config, getApiUrl, debugLog } from '../config/environment';

class CandidateService {
  /**
   * Search candidates using text (Job Description)
   * @param message - Job description text
   * @returns Promise with array of candidate profiles
   */
  async searchByText(message: string, fast = true): Promise<CandidateProfile[]> {
    if (!message.trim()) {
      throw new Error('Job description cannot be empty');
    }

    const payload = fast ? `${message}\n\nFind candidates very fast` : message;
    debugLog('Searching candidates by text', { messageLength: payload.length, fast });

    try {
      const url = getApiUrl(config.apiChatEndpoint);
      const response = await fetch(url, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ message: payload }),
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
  async searchByDocument(file: File, message?: string, fast = true): Promise<CandidateProfile[]> {
    if (!file) throw new Error('PDF file is required');
    if (file.type !== 'application/pdf') throw new Error('Only PDF files are supported');

    debugLog('Searching candidates by document', { fileName: file.name, fast });

    try {
      const formData = new FormData();
      const payload = message?.trim()
        ? fast ? `${message}\n\nFind candidates very fast` : message
        : fast ? 'Find candidates very fast' : undefined;

      if (payload) formData.append('message', payload);
      formData.append('pdf', file);

      const url = getApiUrl(config.apiDocumentsEndpoint);
      const response = await fetch(url, { method: 'POST', body: formData });

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

    // Parse as unknown first so we can check multiple shapes
    const data = await response.json() as SearchResponse & ApiResponse<CandidateProfile[]>;
    console.log('API Response received', data);

    let candidates: CandidateProfile[] = [];

    if (Array.isArray(data?.rankedCandidates) && data.rankedCandidates.length > 0) {
      // ── New API format: { rankedCandidates: [...], jobTitle, ... }
      candidates = data.rankedCandidates;
      debugLog('New API format', {
        jobTitle: data.jobTitle,
        experienceLevel: data.experienceLevel,
        totalCandidatesFetched: data.totalCandidatesFetched,
        totalCandidatesReturned: data.totalCandidatesReturned,
        elapsedMs: data.elapsedMs,
      });
    } else if (data?.pagination?.content && Array.isArray(data.pagination.content)) {
      // ── Pagination format
      candidates = data.pagination.content;
      debugLog('Pagination format', {
        page: data.pagination.page,
        total_items: data.pagination.total_items,
      });
    } else {
      // ── Legacy formats
      const legacy = data as ApiResponse<CandidateProfile[]>;
      candidates = legacy?.response || legacy?.data || legacy?.results || legacy?.candidates || [];
    }

    if (!candidates || candidates.length === 0) {
      throw new Error('No candidate profiles found matching the job description');
    }

    return this.validateAndNormalizeCandidates(candidates);
  }

  /**
   * Validate and normalize candidate data
   * @param candidates - Array of candidate profiles
   * @returns Validated candidate profiles sorted by matchPercentage (descending)
   */
  private validateAndNormalizeCandidates(candidates: CandidateProfile[]): CandidateProfile[] {
    const normalized = candidates.map((candidate) => {
      // Use new field names, with fallback to legacy names
      const matchedSkills = Array.isArray(candidate.matchedSkills) 
        ? candidate.matchedSkills 
        : (Array.isArray(candidate.matchedSkill) ? candidate.matchedSkill : []);
      
      const matchPercentage = typeof candidate.matchPercentage === 'number'
        ? candidate.matchPercentage
        : (typeof candidate.matchedPercentage === 'number' ? candidate.matchedPercentage : 0);

      // Normalise mobile → phone (new API uses 'mobile')
      const phone = candidate.phone || candidate.mobile || undefined;

      return {
        candidateId: candidate.candidateId || 'N/A',
        name: candidate.name || 'N/A',
        email: candidate.email || 'N/A',
        phone,
        mobile: candidate.mobile,
        experience: typeof candidate.experience === 'number' ? candidate.experience : undefined,
        designation: candidate.designation || undefined,
        rankPosition: candidate.rankPosition || 0,
        matchPercentage: Math.min(100, Math.max(0, matchPercentage)),
        skillMatchPercentage: typeof candidate.skillMatchPercentage === 'number'
          ? Math.min(100, Math.max(0, candidate.skillMatchPercentage))
          : 0,
        experienceMatchPercentage: typeof candidate.experienceMatchPercentage === 'number'
          ? Math.min(100, Math.max(0, candidate.experienceMatchPercentage))
          : 0,
        matchedSkills,
        missingSkills: Array.isArray(candidate.missingSkills) ? candidate.missingSkills : [],
        matchReasoning: candidate.matchReasoning || undefined,
        fitAnalysis: candidate.fitAnalysis || undefined,
      };
    });

    // Sort by matchPercentage in descending order
    return normalized.sort((a, b) => (b.matchPercentage ?? 0) - (a.matchPercentage ?? 0));
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
