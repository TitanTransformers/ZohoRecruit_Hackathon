package com.mcp.mcp_server.service;

import com.fasterxml.jackson.core.type.TypeReference;
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
     * Use Claude Haiku to analyze all candidates against the job description.
     * Sends candidates in batches to avoid response truncation.
     */
    private String analyzeAllCandidatesWithClaude(List<Candidate> candidates, JobDescription jobDescription) {
        String jobReqSkills = String.join(", ", jobDescription.getRequiredSkills());
        String jobPrefSkills = String.join(", ", jobDescription.getPreferredSkills());

        // Build compact candidate list (minimal fields to reduce prompt size)
        StringBuilder candidatesJson = new StringBuilder("[");
        for (int i = 0; i < candidates.size(); i++) {
            Candidate c = candidates.get(i);
            if (i > 0) candidatesJson.append(",");
            candidatesJson.append(String.format(
                    "{\"id\":%s,\"title\":%s,\"skills\":%s}",
                    escapeJsonString(c.getCandidateId()),
                    escapeJsonString(c.getCurrentPosition()),
                    escapeJsonString(c.getSkills() != null ? String.join(", ", c.getSkills()) : "")));
        }
        candidatesJson.append("]");

        String prompt = String.format("""
                Rank candidates against this job. Return ONLY a JSON array, no markdown.
                
                JOB: %s | Required: %s | Preferred: %s | Level: %s | Years: %s
                
                CANDIDATES: %s
                
                For EACH candidate return EXACTLY this JSON (keep reasoning under 80 chars):
                {"candidateId":"id","matchPercentage":85.5,"skillMatchPercentage":80,"experienceMatchPercentage":90,"matchedSkills":["s1","s2"],"missingSkills":["s3"],"matchReasoning":"short reason","fitAnalysis":"short analysis"}
                
                Rules: matchPercentage = 0.60*skill + 0.25*experience + 0.15*soft. Be concise.
                """,
                jobDescription.getJobTitle(), jobReqSkills, jobPrefSkills,
                jobDescription.getExperienceLevel(),
                jobDescription.getYearsOfExperience() != null ? jobDescription.getYearsOfExperience() : "N/A",
                candidatesJson);

        String response = getChatClient().prompt()
                .user(prompt)
                .call()
                .content();

        log.debug("Claude AI Ranking Response length: {}", response != null ? response.length() : 0);
        return response;
    }

    /**
     * Parse AI ranking results and build RankedCandidate list
     * Deserializes JSON response directly and enriches with candidate data
     */
    private List<RankedCandidate> parseAIRankingResults(String aiResponse, List<Candidate> candidates) {
        try {
            // Step 1: Extract and validate JSON
            String cleanedResponse = extractAndValidateJSON(aiResponse);
            log.debug("Parsing cleaned JSON response (length: {})", cleanedResponse.length());

            // Step 2: Parse JSON array directly into List<Map>
            List<Map<String, Object>> rankingResults = objectMapper.readValue(cleanedResponse,
                    new TypeReference<List<Map<String, Object>>>() {});

            if (rankingResults == null || rankingResults.isEmpty()) {
                log.warn("No ranking results found in parsed JSON");
                return new ArrayList<>();
            }

            log.debug("Successfully parsed {} ranking results from AI response", rankingResults.size());

            // Step 3: Create candidate lookup map
            Map<String, Candidate> candidateMap = candidates.stream()
                    .collect(Collectors.toMap(Candidate::getCandidateId, c -> c));

            // Step 4: Convert each ranking result to RankedCandidate
            List<RankedCandidate> rankedCandidates = rankingResults.stream()
                    .map(result -> convertToRankedCandidate(result, candidateMap))
                    .filter(Objects::nonNull).sorted(Comparator.comparingDouble(RankedCandidate::getMatchPercentage).reversed()).collect(Collectors.toList());

            // Step 5: Sort by match percentage in descending order

            log.info("Successfully created {} ranked candidates from {} AI results",
                    rankedCandidates.size(), rankingResults.size());
            return rankedCandidates;

        } catch (Exception e) {
            log.error("Failed to parse AI ranking results: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Convert a ranking result Map to RankedCandidate entity
     * Enriches AI-generated ranking data with candidate profile information
     */
    private RankedCandidate convertToRankedCandidate(Map<String, Object> result,
                                                      Map<String, Candidate> candidateMap) {
        try {
            String candidateId = getString(result, "candidateId");
            if (candidateId == null || candidateId.isBlank()) {
                log.warn("Skipping ranking result without valid candidateId");
                return null;
            }

            Candidate candidate = candidateMap.get(candidateId);
            if (candidate == null) {
                log.debug("Candidate not found in lookup map for ID: {}", candidateId);
                return null;
            }

            // Build RankedCandidate from AI ranking data + candidate profile
            return RankedCandidate.builder()
                    .candidateId(candidateId)
                    .name(candidate.getName())
                    .email(candidate.getEmail())
                    .phone(candidate.getPhone())
                    .matchPercentage(Math.min(100.0, getDouble(result, "matchPercentage")))
                    .skillMatchPercentage(getDouble(result, "skillMatchPercentage"))
                    .experienceMatchPercentage(getDouble(result, "experienceMatchPercentage"))
                    .matchedSkills(getList(result, "matchedSkills"))
                    .missingSkills(getList(result, "missingSkills"))
                    .matchReasoning(getString(result, "matchReasoning"))
                    .fitAnalysis(getString(result, "fitAnalysis"))
                    .build();

        } catch (Exception e) {
            log.warn("Error converting ranking result to RankedCandidate: {}", e.getMessage());
            return null;
        }
    }


    /**
     * Safely get string value from Map
     */
    private String getString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value != null) {
            return value.toString();
        }
        return null;
    }

    /**
     * Safely get double value from Map
     */
    private Double getDouble(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value != null) {
            try {
                if (value instanceof Number) {
                    return Math.min(100.0, ((Number) value).doubleValue());
                }
                return Math.min(100.0, Double.parseDouble(value.toString()));
            } catch (Exception e) {
                log.warn("Error parsing double value for key {}: {}", key, e.getMessage());
            }
        }
        return 0.0;
    }

    /**
     * Safely get list value from Map
     */
    @SuppressWarnings("unchecked")
    private List<String> getList(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof List) {
            List<?> list = (List<?>) value;
            return list.stream()
                    .map(Object::toString)
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    /**
     * Attempt to recover from JSON parsing errors by closing incomplete structures
     */
    private String attemptJsonRecovery(String jsonString) {
        if (jsonString == null || jsonString.trim().isEmpty()) {
            return null;
        }

        // Find start of JSON
        int startIdx = jsonString.indexOf('[');
        char closeChar = ']';

        if (startIdx == -1) {
            startIdx = jsonString.indexOf('{');
            closeChar = '}';
        }

        if (startIdx == -1) {
            return null;
        }

        // Try to find last valid object/array end
        int lastValidIdx = -1;
        int bracketCount = 0;
        boolean inString = false;
        boolean escaped = false;

        for (int i = startIdx; i < jsonString.length(); i++) {
            char c = jsonString.charAt(i);

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
                if (c == '[' || c == '{') {
                    bracketCount++;
                } else if (c == ']' || c == '}') {
                    bracketCount--;
                    if (bracketCount == 0) {
                        lastValidIdx = i;
                    }
                }
            }
        }

        // If we found a valid complete structure, return it
        if (lastValidIdx > startIdx) {
            String recovered = jsonString.substring(startIdx, lastValidIdx + 1);
            log.debug("Recovered complete JSON structure (length: {})", recovered.length());
            return recovered;
        }

        // If no complete structure but we have partial, try to close it
        if (bracketCount > 0 && lastValidIdx > startIdx) {
            log.debug("Attempting to close incomplete JSON with {} open brackets", bracketCount);
            return attemptPartialJsonRecovery(jsonString.substring(startIdx), bracketCount);
        }

        return null;
    }

    /**
     * Extract valid JSON from AI response with robust error handling
     */
    private String extractAndValidateJSON(String aiResponse) {
        if (aiResponse == null || aiResponse.trim().isEmpty()) {
            throw new IllegalArgumentException("AI response is empty or null");
        }

        // Log the full response for debugging incomplete JSON
        log.debug("Full AI response length: {}", aiResponse.length());
        log.debug("Full AI response preview: {}", aiResponse.substring(0, Math.min(500, aiResponse.length())));

        // Step 1: Remove markdown code blocks
        String cleaned = aiResponse
                .replaceAll("```(?:json)?\\s*\\n?", "")
                .replaceAll("```\\s*\\n?", "")
                .trim();

        // Step 2: Extract JSON using bracket matching for arrays or objects
        String extracted = extractJsonByBracketMatching(cleaned);

        // If extraction failed, try recovery for incomplete JSON
        if (extracted == null || extracted.isEmpty()) {
            log.warn("Initial extraction failed, attempting recovery...");
            extracted = attemptJsonRecovery(cleaned);
        }

        if (extracted == null || extracted.isEmpty()) {
            log.error("Could not extract valid JSON from response. Cleaned response: {}",
                    cleaned.substring(0, Math.min(500, cleaned.length())));
            throw new IllegalArgumentException("No valid JSON found in AI response");
        }

        return extracted;
    }

    /**
     * Extract JSON from text using proper bracket matching
     * Handles both arrays [...] and objects {...}
     */
    private String extractJsonByBracketMatching(String text) {
        // Try to find JSON array first
        int startIdx = text.indexOf('[');
        char openChar = '[';
        char closeChar = ']';

        // If no array, look for JSON object
        if (startIdx == -1) {
            startIdx = text.indexOf('{');
            openChar = '{';
            closeChar = '}';
        }

        if (startIdx == -1) {
            log.debug("No JSON start character found");
            return null;
        }

        log.debug("JSON starts at index {} with char: {}", startIdx, openChar);

        int bracketCount = 0;
        boolean inString = false;
        boolean previousCharEscaped = false;
        int lastValidIndex = -1;

        for (int i = startIdx; i < text.length(); i++) {
            char c = text.charAt(i);
            boolean currentCharEscaped = false;

            // Check if current character is escaped
            if (c == '\\' && inString && !previousCharEscaped) {
                currentCharEscaped = true;
            }

            // Handle string boundaries (only if not escaped)
            if (c == '"' && !previousCharEscaped) {
                inString = !inString;
            }

            // Only count brackets outside of strings
            if (!inString) {
                if (c == openChar || c == '{' || c == '[') {
                    bracketCount++;
                    lastValidIndex = i;
                } else if (c == closeChar || c == '}' || c == ']') {
                    bracketCount--;
                    lastValidIndex = i;
                    if (bracketCount == 0) {
                        // Found matching closing bracket
                        String potential = text.substring(startIdx, i + 1);
                        log.debug("Successfully extracted JSON (length: {})", potential.length());
                        return potential;
                    }
                }
            }

            // Update escape status for next iteration
            previousCharEscaped = currentCharEscaped;
        }

        // If we get here, JSON was not properly closed
        log.warn("Unclosed JSON structure - found {} unmatched opening brackets. Last valid index: {}", bracketCount, lastValidIndex);

        // Try to recover by returning partial JSON with proper closure
        if (lastValidIndex > startIdx) {
            return attemptPartialJsonRecovery(text.substring(startIdx, lastValidIndex + 1), bracketCount);
        }

        return null;
    }

    /**
     * Recover incomplete JSON array by truncating to the last complete object
     * and properly closing the array structure
     */
    private String attemptPartialJsonRecovery(String incompleteJson, int openBracketCount) {
        if (openBracketCount <= 0) {
            return incompleteJson;
        }

        log.warn("JSON is truncated with {} unclosed brackets. Attempting recovery by finding last complete object...", openBracketCount);

        // Find the last complete JSON object by scanning for the last '}'
        // that properly closes an object at the array element level
        int depth = 0;
        int lastCompleteObjectEnd = -1;
        boolean inString = false;
        boolean escaped = false;

        for (int i = 0; i < incompleteJson.length(); i++) {
            char c = incompleteJson.charAt(i);

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
                if (c == '{' || c == '[') {
                    depth++;
                } else if (c == '}' || c == ']') {
                    depth--;
                    // depth == 1 means we just closed an object at the top-level array
                    if (depth == 1 && c == '}') {
                        lastCompleteObjectEnd = i;
                    }
                }
            }
        }

        if (lastCompleteObjectEnd > 0) {
            // Truncate to the last complete object and close the array
            String recovered = incompleteJson.substring(0, lastCompleteObjectEnd + 1) + "]";
            log.info("Recovery successful: truncated incomplete last object. Recovered JSON length: {}", recovered.length());
            return recovered;
        }

        log.error("Recovery failed: could not find any complete object in the array");
        return null;
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

              String cleanedResponse = extractAndValidateJSON(response);

             Map<String, Object> analysis = parseJsonToMap(cleanedResponse);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("candidateId", candidate.getCandidateId());
            result.put("candidateName", candidate.getName());
            result.put("jobTitle", jobDescription.getJobTitle());
            result.put("overallFit", getString(analysis, "overallFit"));
            result.put("strengths", getList(analysis, "strengths"));
            result.put("weaknesses", getList(analysis, "weaknesses"));
            result.put("developmentAreas", getList(analysis, "developmentAreas"));
            result.put("potentialToGrow", analysis.get("potentialToGrow"));
            result.put("culturalFit", getString(analysis, "culturalFit"));
            result.put("recommendedInterviewFocus", getList(analysis, "recommendedInterviewFocus"));
            result.put("estimatedRampUpTime", getString(analysis, "estimatedRampUpTime"));

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

              String cleanedResponse = extractAndValidateJSON(response);

             Map<String, Object> questionsData = parseJsonToMap(cleanedResponse);
            List<?> questionsArray = (List<?>) questionsData.get("questions");

            List<Map<String, String>> questions = new ArrayList<>();
            if (questionsArray != null) {
                for (Object q : questionsArray) {
                    if (q instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> qMap = (Map<String, Object>) q;
                        Map<String, String> question = new LinkedHashMap<>();
                        question.put("question", getString(qMap, "question"));
                        question.put("category", getString(qMap, "category"));
                        question.put("rationale", getString(qMap, "rationale"));
                        questions.add(question);
                    }
                }
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

    /**
     * Escape a string for JSON and wrap in quotes
     * Handles null values by returning "null"
     */
    private String escapeJsonString(String value) {
        if (value == null || value.isEmpty()) {
            return "\"\"";
        }
        // Escape special JSON characters
        String escaped = value
                .replace("\\", "\\\\")  // Backslash must be first
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t")
                .replace("\b", "\\b")
                .replace("\f", "\\f");
        return "\"" + escaped + "\"";
    }


    /**
     * Parse JSON string to Map<String, Object>
     */
    private Map<String, Object> parseJsonToMap(String jsonString) {
        try {
            return objectMapper.readValue(jsonString, new TypeReference<>() {});
        } catch (Exception e) {
            log.error("Failed to parse JSON to map: {}", e.getMessage(), e);
            return new LinkedHashMap<>();
        }
    }
}

