package com.mcp.mcp_server.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mcp.mcp_server.entity.Candidate;
import com.mcp.mcp_server.entity.JobDescription;
import com.mcp.mcp_server.entity.RankedCandidate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * AI-Enhanced Candidate Ranking Service using Claude Haiku
 * Provides intelligent semantic matching and ranking of candidates against job descriptions
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AIEnhancedCandidateRankingService {

    private final ObjectProvider<ChatClient> chatClientProvider;
    private final ObjectMapper objectMapper;

    private ChatClient getChatClient() {
        return chatClientProvider.getObject();
    }

    /**
     * Rank candidates using Claude Haiku AI for semantic analysis
     */
    public List<RankedCandidate> rankCandidatesWithAI(List<Candidate> candidates, JobDescription jobDescription) {
        log.debug("Ranking {} candidates with Claude Haiku AI", candidates.size());

        try {
            // Use Claude to analyze all candidates against JD
            String aiAnalysis = analyzeAllCandidatesWithClaude(candidates, jobDescription);
            List<RankedCandidate> rankedCandidates = parseAIRankingResults(aiAnalysis, candidates);

            if (!rankedCandidates.isEmpty()) {
                return rankedCandidates;
            }
        } catch (Exception e) {
            log.error("AI ranking failed", e);
            throw new RuntimeException("Failed to rank candidates using AI", e);
        }

        return new ArrayList<>();
    }

    /**
     * Use Claude Haiku to analyze all candidates against the job description
     */
    private String analyzeAllCandidatesWithClaude(List<Candidate> candidates, JobDescription jobDescription) {
        StringBuilder candidatesJson = new StringBuilder();
        for (int i = 0; i < candidates.size(); i++) {
            Candidate c = candidates.get(i);
            candidatesJson.append(String.format("""
                    {
                      "id": "%s",
                      "name": "%s",
                      "email": "%s",
                      "currentPosition": "%s",
                      "experience": "%s",
                      "skills": "%s"
                    }""",
                    c.getCandidateId() != null ? c.getCandidateId() : "",
                    c.getName() != null ? c.getName() : "",
                    c.getEmail() != null ? c.getEmail() : "",
                    c.getCurrentPosition() != null ? c.getCurrentPosition() : "",
                    c.getExperience() != null ? c.getExperience() : "",
                    c.getSkills() != null ? String.join(", ", c.getSkills()) : ""));
            if (i < candidates.size() - 1) {
                candidatesJson.append(",");
            }
        }

        String jobReqSkills = String.join(", ", jobDescription.getRequiredSkills());
        String jobPrefSkills = String.join(", ", jobDescription.getPreferredSkills());

        String prompt = String.format("""
                Analyze each candidate against the job description and rank them by fit.
                
                JOB DESCRIPTION:
                - Title: %s
                - Required Skills: %s
                - Preferred Skills: %s
                - Experience Level: %s
                - Years Required: %s
                
                CANDIDATES:
                [%s]
                
                Return ONLY a valid JSON array with one object per candidate (no markdown, no extra text):
                [
                  {
                    "candidateId": "id",
                    "matchPercentage": 85.5,
                    "skillMatchPercentage": 80,
                    "experienceMatchPercentage": 90,
                    "matchedSkills": ["skill1", "skill2"],
                    "missingSkills": ["skill3"],
                    "matchReasoning": "detailed reasoning...",
                    "fitAnalysis": "summary of fit..."
                  },
                  ...
                ]
                
                Scoring guidelines:
                - matchPercentage: overall score 0-100 = (0.60 * skillMatch) + (0.25 * experienceMatch) + (0.15 * softSkillMatch)
                - skillMatchPercentage: percentage of required skills present in candidate profile (0-100)
                - experienceMatchPercentage: alignment of experience with JD requirements (0-100)
                - Consider skill overlap (60%% weight), experience (25%% weight), and soft skills (15%% weight)
                - Be realistic but fair in assessment
                """,
                jobDescription.getJobTitle(),
                jobReqSkills,
                jobPrefSkills,
                jobDescription.getExperienceLevel(),
                jobDescription.getYearsOfExperience() != null ? jobDescription.getYearsOfExperience() : "Not specified",
                candidatesJson.toString());

        String response = getChatClient().prompt()
                .user(prompt)
                .call()
                .content();

        log.debug("Claude AI Ranking Response: {}", response);
        return response;
    }

    /**
     * Parse AI ranking results and build RankedCandidate list
     */
    private List<RankedCandidate> parseAIRankingResults(String aiResponse, List<Candidate> candidates) {
        try {
            // Clean response
            String cleanedResponse = aiResponse
                    .replaceAll("```json\\n?", "")
                    .replaceAll("```\\n?", "")
                    .trim();

            JsonNode[] rankingResults = objectMapper.readValue(cleanedResponse, JsonNode[].class);
            Map<String, Candidate> candidateMap = candidates.stream()
                    .collect(Collectors.toMap(c -> c.getCandidateId(), c -> c));

            List<RankedCandidate> ranked = new ArrayList<>();
            for (JsonNode result : rankingResults) {
                String candidateId = result.get("candidateId").asText();
                Candidate candidate = candidateMap.get(candidateId);

                if (candidate != null) {
                    RankedCandidate rankedCandidate = RankedCandidate.builder()
                            .candidateId(candidateId)
                            .name(candidate.getName())
                            .email(candidate.getEmail())
                            .phone(candidate.getPhone())
                            .matchPercentage(Math.min(100.0, result.get("matchPercentage").asDouble()))
                            .skillMatchPercentage(getDoubleValue(result, "skillMatchPercentage"))
                            .experienceMatchPercentage(getDoubleValue(result, "experienceMatchPercentage"))
                            .matchedSkills(getListValue(result, "matchedSkills"))
                            .missingSkills(getListValue(result, "missingSkills"))
                            .matchReasoning(result.get("matchReasoning").asText())
                            .fitAnalysis(result.get("fitAnalysis").asText())
                            .build();

                    ranked.add(rankedCandidate);
                }
            }

            // Sort by match percentage descending
            ranked.sort(Comparator.comparingDouble(RankedCandidate::getMatchPercentage).reversed());
            return ranked;
        } catch (Exception e) {
            log.error("Failed to parse AI ranking results", e);
            return new ArrayList<>();
        }
    }

    /**
     * Get detailed AI-powered fit analysis for a single candidate
     */
    public Map<String, Object> getDetailedFitAnalysis(Candidate candidate, JobDescription jobDescription) {
        log.debug("Getting detailed fit analysis for candidate: {}", candidate.getName());

        try {
            String prompt = String.format("""
                    Provide a detailed fit analysis for this candidate against the job description.
                    
                    CANDIDATE:
                    - Name: %s
                    - Current Position: %s
                    - Experience: %s
                    - Skills: %s
                    
                    JOB:
                    - Title: %s
                    - Required Skills: %s
                    - Preferred Skills: %s
                    - Experience Level: %s
                    - Responsibilities: %s
                    
                    Return ONLY valid JSON (no markdown):
                    {
                      "overallFit": "percentage number 0-100",
                      "strengths": ["strength1", "strength2"],
                      "weaknesses": ["weakness1", "weakness2"],
                      "developmentAreas": ["area1", "area2"],
                      "potentialToGrow": true/false,
                      "culturalFit": "assessment of cultural fit",
                      "recommendedInterviewFocus": ["focus1", "focus2"],
                      "estimatedRampUpTime": "time estimate"
                    }
                    """,
                    candidate.getName(),
                    candidate.getCurrentPosition(),
                    candidate.getExperience(),
                    candidate.getSkills() != null ? String.join(", ", candidate.getSkills()) : "Not specified",
                    jobDescription.getJobTitle(),
                    String.join(", ", jobDescription.getRequiredSkills()),
                    String.join(", ", jobDescription.getPreferredSkills()),
                    jobDescription.getExperienceLevel(),
                     String.join("; ", jobDescription.getResponsibilities()));

             String response = getChatClient().prompt()
                     .user(prompt)
                     .call()
                     .content();

             String cleanedResponse = response
                     .replaceAll("```json\\n?", "")
                    .replaceAll("```\\n?", "")
                    .trim();

            JsonNode analysis = objectMapper.readTree(cleanedResponse);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("candidateId", candidate.getCandidateId());
            result.put("candidateName", candidate.getName());
            result.put("jobTitle", jobDescription.getJobTitle());
            result.put("overallFit", analysis.get("overallFit").asText());
            result.put("strengths", getListValue(analysis, "strengths"));
            result.put("weaknesses", getListValue(analysis, "weaknesses"));
            result.put("developmentAreas", getListValue(analysis, "developmentAreas"));
            result.put("potentialToGrow", analysis.get("potentialToGrow").asBoolean());
            result.put("culturalFit", analysis.get("culturalFit").asText());
            result.put("recommendedInterviewFocus", getListValue(analysis, "recommendedInterviewFocus"));
            result.put("estimatedRampUpTime", analysis.get("estimatedRampUpTime").asText());

            return result;
        } catch (Exception e) {
            log.error("Error getting detailed fit analysis", e);
            return Map.of("error", "Failed to analyze candidate fit: " + e.getMessage());
        }
    }

    /**
     * Generate interview questions tailored to candidate and job
     */
    public Map<String, Object> generateInterviewQuestions(Candidate candidate, JobDescription jobDescription, int questionCount) {
        log.debug("Generating {} interview questions for candidate: {}", questionCount, candidate.getName());

        try {
            String prompt = String.format("""
                    Generate %d targeted interview questions for evaluating this candidate for the position.
                    Focus on assessing both technical and soft skills.
                    
                    CANDIDATE:
                    - Name: %s
                    - Current Position: %s
                    - Experience: %s
                    - Skills: %s
                    
                    JOB:
                    - Title: %s
                    - Required Skills: %s
                    - Key Responsibilities: %s
                    
                    Return ONLY valid JSON (no markdown):
                    {
                      "questions": [
                        {
                          "question": "question text",
                          "category": "technical/behavioral/domain/culture",
                          "rationale": "why this question is important for this candidate"
                        }
                      ]
                    }
                    """,
                    questionCount,
                    candidate.getName(),
                    candidate.getCurrentPosition(),
                    candidate.getExperience(),
                    candidate.getSkills() != null ? String.join(", ", candidate.getSkills()) : "Not specified",
                    jobDescription.getJobTitle(),
                    String.join(", ", jobDescription.getRequiredSkills()),
                     String.join("; ", jobDescription.getResponsibilities().stream().limit(3).collect(Collectors.toList())));

             String response = getChatClient().prompt()
                     .user(prompt)
                     .call()
                     .content();

             String cleanedResponse = response
                     .replaceAll("```json\\n?", "")
                    .replaceAll("```\\n?", "")
                    .trim();

            JsonNode questionsNode = objectMapper.readTree(cleanedResponse);
            JsonNode questionsArray = questionsNode.get("questions");

            List<Map<String, String>> questions = new ArrayList<>();
            for (JsonNode q : questionsArray) {
                Map<String, String> question = new LinkedHashMap<>();
                question.put("question", q.get("question").asText());
                question.put("category", q.get("category").asText());
                question.put("rationale", q.get("rationale").asText());
                questions.add(question);
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("candidateId", candidate.getCandidateId());
            result.put("candidateName", candidate.getName());
            result.put("jobTitle", jobDescription.getJobTitle());
            result.put("totalQuestions", questions.size());
            result.put("questions", questions);

            return result;
        } catch (Exception e) {
            log.error("Error generating interview questions", e);
            return Map.of("error", "Failed to generate questions: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helper methods
    // ─────────────────────────────────────────────────────────────────────────

    private List<String> getListValue(JsonNode node, String field) {
        List<String> list = new ArrayList<>();
        if (node.has(field) && node.get(field).isArray()) {
            for (JsonNode item : node.get(field)) {
                list.add(item.asText());
            }
        }
        return list;
    }

    /**
     * Get double value from JsonNode
     * Safely extracts numeric values, returning 0.0 if field doesn't exist or is null
     */
    private Double getDoubleValue(JsonNode node, String field) {
        if (node.has(field) && !node.get(field).isNull()) {
            return Math.min(100.0, node.get(field).asDouble());
        }
        return 0.0;
    }
}

