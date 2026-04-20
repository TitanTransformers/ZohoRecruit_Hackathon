package com.mcp.mcp_server.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mcp.mcp_server.entity.JobDescription;
import com.mcp.mcp_server.entity.RankedCandidate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Parallel Candidate Detail Analysis Service
 * Provides detailed analysis of individual candidates:
 * - Strengths and weakness assessment
 * - Cultural fit analysis
 * - Ramp-up time estimation
 * - Interview question generation
 *
 * All analysis is designed to run in parallel via AIEnhancedCandidateRankingService.analyzeMultipleCandidatesInParallel()
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CandidateDetailAnalysisService {

    private final ObjectProvider<ChatClient> chatClientProvider;
    private final ObjectMapper objectMapper;

    private ChatClient getChatClient() {
        return chatClientProvider.getObject();
    }

    /**
     * Generate detailed fit analysis for a single candidate against a job description
     *
     * Returns a map with:
     * - strengths: [str] key strengths for this role
     * - weaknesses: [str] skill/experience gaps
     * - culturalFit: str cultural fit assessment
     * - rampUpDays: int estimated ramp-up time in days
     * - overallFitScore: 0-100
     * - recommendation: str hire/consider/skip recommendation
     */
    public Map<String, Object> generateDetailedFitAnalysis(RankedCandidate candidate, JobDescription jobDescription) {
        log.debug("Generating detailed fit analysis for candidate: {}", candidate.getCandidateId());

        try {
            String prompt = String.format("""
                    Provide a detailed fit analysis for this candidate. Return ONLY valid JSON, no markdown.
                    
                    JOB: %s
                    Required Skills: %s
                    Preferred Skills: %s
                    Experience Level: %s
                    
                    CANDIDATE: %s
                    Matched Skills: %s
                    Missing Skills: %s
                    Match: %s%%
                    
                    Analyze and return this JSON:
                    {
                      "strengths": ["strength1", "strength2"],
                      "weaknesses": ["weakness1", "weakness2"],
                      "culturalFitAssessment": "short assessment",
                      "estimatedRampUpDays": 30,
                      "overallFitScore": 75,
                      "recommendation": "HIRE|CONSIDER|SKIP",
                      "keyInsights": "detailed insights about this candidate"
                    }
                    """,
                    jobDescription.getJobTitle(),
                    String.join(", ", jobDescription.getRequiredSkills()),
                    String.join(", ", jobDescription.getPreferredSkills()),
                    jobDescription.getExperienceLevel() != null ? jobDescription.getExperienceLevel() : "N/A",
                    candidate.getName(),
                    String.join(", ", candidate.getMatchedSkills() != null ? candidate.getMatchedSkills() : Collections.emptyList()),
                    String.join(", ", candidate.getMissingSkills() != null ? candidate.getMissingSkills() : Collections.emptyList()),
                    candidate.getMatchPercentage()
            );

            String response = getChatClient().prompt()
                    .user(prompt)
                    .call()
                    .content();

            log.debug("Fit analysis response length for {}: {}", candidate.getCandidateId(),
                    response != null ? response.length() : 0);

            Map<String, Object> result = parseAnalysisResponse(response, "fit analysis");
            result.put("candidateId", candidate.getCandidateId());
            result.put("candidateName", candidate.getName());
            result.put("analysisType", "fitAnalysis");

            return result;

        } catch (Exception e) {
            log.error("Error generating fit analysis for candidate {}: {}", candidate.getCandidateId(), e.getMessage(), e);
            return buildErrorResult(candidate.getCandidateId(), "fitAnalysis", e.getMessage());
        }
    }

    /**
     * Generate customized interview questions for a candidate-job pairing
     *
     * Returns a map with:
     * - technicalQuestions: [str] technical/domain questions
     * - behavioralQuestions: [str] behavioral assessment questions
     * - culturalFitQuestions: [str] cultural fit questions
     * - gapFillingQuestions: [str] questions targeting skill gaps
     */
    public Map<String, Object> generateInterviewQuestions(RankedCandidate candidate, JobDescription jobDescription) {
        log.debug("Generating interview questions for candidate: {}", candidate.getCandidateId());

        try {
            String prompt = String.format("""
                    Generate interview questions tailored to this candidate and job. Return ONLY valid JSON, no markdown.
                    
                    JOB: %s
                    Required Skills: %s
                    
                    CANDIDATE: %s
                    Matched Skills: %s
                    Missing Skills: %s
                    
                    Return exactly this JSON structure:
                    {
                      "technicalQuestions": [
                        "question1 about required technology",
                        "question2 about architecture/design"
                      ],
                      "behavioralQuestions": [
                        "question1 about teamwork",
                        "question2 about handling challenges"
                      ],
                      "culturalFitQuestions": [
                        "question1 about work style",
                        "question2 about values alignment"
                      ],
                      "gapFillingQuestions": [
                        "question to assess learning in missing skill",
                        "question about willingness to learn"
                      ]
                    }
                    """,
                    jobDescription.getJobTitle(),
                    String.join(", ", jobDescription.getRequiredSkills()),
                    candidate.getName(),
                    String.join(", ", candidate.getMatchedSkills() != null ? candidate.getMatchedSkills() : Collections.emptyList()),
                    String.join(", ", candidate.getMissingSkills() != null ? candidate.getMissingSkills() : Collections.emptyList())
            );

            String response = getChatClient().prompt()
                    .user(prompt)
                    .call()
                    .content();

            log.debug("Interview questions response length for {}: {}", candidate.getCandidateId(),
                    response != null ? response.length() : 0);

            Map<String, Object> result = parseAnalysisResponse(response, "interview questions");
            result.put("candidateId", candidate.getCandidateId());
            result.put("candidateName", candidate.getName());
            result.put("analysisType", "interviewQuestions");

            return result;

        } catch (Exception e) {
            log.error("Error generating interview questions for candidate {}: {}", candidate.getCandidateId(), e.getMessage(), e);
            return buildErrorResult(candidate.getCandidateId(), "interviewQuestions", e.getMessage());
        }
    }

    /**
     * Generate strengths and weaknesses assessment
     *
     * Returns a map with:
     * - technicalStrengths: [str]
     * - softSkillStrengths: [str]
     * - technicalGaps: [str]
     * - developmentAreas: [str]
     */
    public Map<String, Object> generateStrengthsAndWeaknesses(RankedCandidate candidate, JobDescription jobDescription) {
        log.debug("Generating strengths/weaknesses for candidate: {}", candidate.getCandidateId());

        try {
            String prompt = String.format("""
                    Analyze candidate strengths and weaknesses for this role. Return ONLY valid JSON, no markdown.
                    
                    JOB: %s (%s level)
                    Required: %s
                    
                    CANDIDATE: %s
                    Matched Skills: %s
                    Missing Skills: %s
                    Match Score: %s%%
                    
                    Return this JSON:
                    {
                      "technicalStrengths": ["strength1", "strength2"],
                      "softSkillStrengths": ["strength1", "strength2"],
                      "technicalGaps": ["gap1", "gap2"],
                      "developmentAreas": ["area1", "area2"],
                      "readinessScore": 75,
                      "assessmentSummary": "one paragraph summary"
                    }
                    """,
                    jobDescription.getJobTitle(),
                    jobDescription.getExperienceLevel() != null ? jobDescription.getExperienceLevel() : "N/A",
                    String.join(", ", jobDescription.getRequiredSkills()),
                    candidate.getName(),
                    String.join(", ", candidate.getMatchedSkills() != null ? candidate.getMatchedSkills() : Collections.emptyList()),
                    String.join(", ", candidate.getMissingSkills() != null ? candidate.getMissingSkills() : Collections.emptyList()),
                    candidate.getMatchPercentage()
            );

            String response = getChatClient().prompt()
                    .user(prompt)
                    .call()
                    .content();

            Map<String, Object> result = parseAnalysisResponse(response, "strengths/weaknesses");
            result.put("candidateId", candidate.getCandidateId());
            result.put("candidateName", candidate.getName());
            result.put("analysisType", "strengthsWeaknesses");

            return result;

        } catch (Exception e) {
            log.error("Error generating strengths/weaknesses for candidate {}: {}", candidate.getCandidateId(), e.getMessage(), e);
            return buildErrorResult(candidate.getCandidateId(), "strengthsWeaknesses", e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Parse JSON response from Claude, handling markdown and malformed JSON
     */
    private Map<String, Object> parseAnalysisResponse(String response, String analysisType) {
        try {
            // Remove markdown code blocks
            String cleaned = response
                    .replaceAll("```(?:json)?\\s*\\n?", "")
                    .replaceAll("```\\s*\\n?", "")
                    .trim();

            // Extract JSON
            int startIdx = cleaned.indexOf('{');
            if (startIdx == -1) {
                log.warn("No JSON found in {} response", analysisType);
                return new HashMap<>();
            }

            // Find closing brace
            int depth = 0;
            int endIdx = -1;
            boolean inString = false;
            boolean escaped = false;

            for (int i = startIdx; i < cleaned.length(); i++) {
                char c = cleaned.charAt(i);

                if (escaped) {
                    escaped = false;
                    continue;
                }

                if (c == '\\' && inString) {
                    escaped = true;
                    continue;
                }

                if (c == '"') {
                    inString = !inString;
                    continue;
                }

                if (!inString) {
                    if (c == '{') {
                        depth++;
                    } else if (c == '}') {
                        depth--;
                        if (depth == 0) {
                            endIdx = i;
                            break;
                        }
                    }
                }
            }

            if (endIdx <= startIdx) {
                log.warn("Could not find complete JSON in {} response", analysisType);
                return new HashMap<>();
            }

            String jsonStr = cleaned.substring(startIdx, endIdx + 1);
            Map<String, Object> result = objectMapper.readValue(jsonStr, new TypeReference<Map<String, Object>>() {});

            log.debug("Successfully parsed {} response", analysisType);
            return result;

        } catch (Exception e) {
            log.error("Error parsing {} response: {}", analysisType, e.getMessage(), e);
            return new HashMap<>();
        }
    }

    /**
     * Build error result for failed analysis
     */
    private Map<String, Object> buildErrorResult(String candidateId, String analysisType, String errorMsg) {
        return Map.of(
                "candidateId", candidateId,
                "analysisType", analysisType,
                "error", true,
                "errorMessage", errorMsg,
                "success", false
        );
    }
}


