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
import java.util.concurrent.*;
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
     * Batch size for ranking operations
     */
    private static final int RANKING_BATCH_SIZE = 10;

    /**
     * Rank candidates using Claude Haiku AI for semantic analysis.
     * All candidates ranked in a single batch for consistent relative scoring.
     *
     * COST EFFECTIVE: ~$0.15-0.20 for 31 candidates
     * Speed: ~23 seconds
     */
    public List<RankedCandidate> rankCandidatesWithAI(List<Candidate> candidates, JobDescription jobDescription) {
        log.debug("Ranking {} candidates with Claude Haiku AI (single batch for consistency)", candidates.size());

        try {
            // Use Claude to analyze all candidates against JD in one batch for relative consistency
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
     * Rank candidates using Claude Haiku AI with parallel batch processing.
     * Splits candidates into batches and ranks each batch in parallel.
     *
     * FASTER but LESS COST EFFECTIVE: ~$0.25-0.30 for 31 candidates
     * Speed: ~8-10 seconds (parallel)
     * Note: Ranking scores within each batch are consistent relative to that batch,
     * but absolute scores may vary between batches.
     *
     * @param candidates List of candidates to rank
     * @param jobDescription Job description context
     * @return List of ranked candidates sorted by match percentage
     */
    public List<RankedCandidate> rankCandidatesWithAIBatching(List<Candidate> candidates, JobDescription jobDescription) {
        log.debug("Ranking {} candidates with parallel batch processing (batch size: {})",
                candidates.size(), RANKING_BATCH_SIZE);

        if (candidates.isEmpty()) {
            return new ArrayList<>();
        }

        int threadPoolSize = Math.min(4, (candidates.size() + RANKING_BATCH_SIZE - 1) / RANKING_BATCH_SIZE);
        ExecutorService executorService = Executors.newFixedThreadPool(threadPoolSize);

        try {
            List<RankedCandidate> allRankedCandidates = Collections.synchronizedList(new ArrayList<>());

            // Split into batches for parallel processing
            int batches = (candidates.size() + RANKING_BATCH_SIZE - 1) / RANKING_BATCH_SIZE;
            long startTime = System.currentTimeMillis();
            List<Future<List<RankedCandidate>>> futures = new ArrayList<>();

            log.info("Submitting {} batches for parallel ranking (batch size: {})", batches, RANKING_BATCH_SIZE);

            for (int i = 0; i < batches; i++) {
                final int batchIndex = i;
                int start = i * RANKING_BATCH_SIZE;
                int end = Math.min(start + RANKING_BATCH_SIZE, candidates.size());
                List<Candidate> batch = candidates.subList(start, end);

                // Submit batch ranking task to thread pool
                Future<List<RankedCandidate>> future = executorService.submit(() -> {
                    try {
                        log.debug("Ranking batch {}/{}: {} candidates on thread {}",
                                batchIndex + 1, batches, batch.size(), Thread.currentThread().getName());

                        String aiAnalysis = analyzeAllCandidatesWithClaude(batch, jobDescription);
                        List<RankedCandidate> batchResults = parseAIRankingResults(aiAnalysis, batch);

                        log.debug("Batch {}/{} completed with {} results",
                                batchIndex + 1, batches, batchResults.size());

                        return batchResults;
                    } catch (Exception e) {
                        log.error("Error ranking batch {}: {}", batchIndex + 1, e.getMessage(), e);
                        return Collections.emptyList();
                    }
                });

                futures.add(future);
            }

            // Wait for all batch ranking tasks to complete and collect results
            log.debug("Waiting for {} batch ranking tasks to complete...", futures.size());
            for (int i = 0; i < futures.size(); i++) {
                try {
                    List<RankedCandidate> batchResults = futures.get(i).get(60, TimeUnit.SECONDS);
                    allRankedCandidates.addAll(batchResults);

                    long elapsedMs = System.currentTimeMillis() - startTime;
                    log.debug("Batch {}/{} collected. Total elapsed: {}ms",
                            i + 1, futures.size(), elapsedMs);
                } catch (TimeoutException e) {
                    log.error("Batch {} ranking timed out after 60 seconds", i + 1);
                    futures.get(i).cancel(true);
                } catch (Exception e) {
                    log.error("Error collecting batch {} results: {}", i + 1, e.getMessage(), e);
                }
            }

            // Sort all results by match percentage (highest first)
            allRankedCandidates.sort(Comparator.comparingDouble(RankedCandidate::getMatchPercentage).reversed());

            long totalTimeMs = System.currentTimeMillis() - startTime;
            log.info("Parallel batch ranking completed: {} candidates in {}ms (avg: {}ms per candidate)",
                    allRankedCandidates.size(), totalTimeMs,
                    allRankedCandidates.isEmpty() ? 0 : totalTimeMs / allRankedCandidates.size());

            if (!allRankedCandidates.isEmpty()) {
                return allRankedCandidates;
            }
        } catch (Exception e) {
            log.error("Parallel batch ranking failed", e);
            throw new RuntimeException("Failed to rank candidates using parallel batching", e);
        } finally {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                    log.warn("Executor service did not terminate in time, forcing shutdown");
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                log.warn("Interrupted while waiting for executor shutdown", e);
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        return new ArrayList<>();
    }

    /**
     * Analyze candidates in parallel for detailed insights (fit analysis, interview questions, etc.)
     * Uses thread pool to process multiple candidates concurrently.
     *
     * @param candidates List of candidates to analyze in parallel
     * @param jobDescription Job description context for analysis
     * @param analysisTask Lambda that performs the actual analysis on each candidate
     * @return Map of candidateId -> analysis result
     */
    public Map<String, Map<String, Object>> analyzeMultipleCandidatesInParallel(
            List<RankedCandidate> candidates,
            JobDescription jobDescription,
            CandidateAnalysisTask analysisTask) {

        int threadPoolSize = Math.min(4, candidates.size());
        ExecutorService executorService = Executors.newFixedThreadPool(threadPoolSize);
        Map<String, Map<String, Object>> results = new ConcurrentHashMap<>();

        try {
            List<Future<Void>> futures = new ArrayList<>();
            long startTime = System.currentTimeMillis();

            log.info("Starting parallel candidate analysis for {} candidates (threads: {})",
                    candidates.size(), threadPoolSize);

            // Submit analysis task for each candidate
            for (int i = 0; i < candidates.size(); i++) {
                final int candidateIndex = i;
                RankedCandidate candidate = candidates.get(i);

                Future<Void> future = executorService.submit(() -> {
                    try {
                        log.debug("Analyzing candidate {}/{}: {} (ID: {}) on thread {}",
                                candidateIndex + 1, candidates.size(), candidate.getName(),
                                candidate.getCandidateId(), Thread.currentThread().getName());

                        // Perform the actual analysis
                        Map<String, Object> analysisResult = analysisTask.analyze(candidate, jobDescription);
                        results.put(candidate.getCandidateId(), analysisResult);

                        log.debug("Candidate {}/{} analysis completed", candidateIndex + 1, candidates.size());
                        return null;
                    } catch (Exception e) {
                        log.error("Error analyzing candidate {}: {}", candidate.getCandidateId(), e.getMessage(), e);
                        return null;
                    }
                });

                futures.add(future);
            }

            // Wait for all analysis tasks to complete
            log.debug("Waiting for {} candidate analysis tasks to complete...", futures.size());
            for (int i = 0; i < futures.size(); i++) {
                try {
                    futures.get(i).get(120, TimeUnit.SECONDS);
                } catch (TimeoutException e) {
                    log.error("Candidate analysis task {} timed out after 120 seconds", i + 1);
                    futures.get(i).cancel(true);
                } catch (Exception e) {
                    log.error("Error waiting for candidate analysis {}: {}", i + 1, e.getMessage(), e);
                }
            }

            long totalTimeMs = System.currentTimeMillis() - startTime;
            log.info("Parallel candidate analysis completed: {} candidates in {}ms (avg: {}ms per candidate)",
                    results.size(), totalTimeMs,
                    results.isEmpty() ? 0 : totalTimeMs / results.size());

            return results;

        } catch (Exception e) {
            log.error("Parallel candidate analysis failed", e);
            return new HashMap<>();
        } finally {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                    log.warn("Executor service did not terminate in time, forcing shutdown");
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                log.warn("Interrupted while waiting for executor shutdown", e);
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Functional interface for candidate analysis tasks
     */
    @FunctionalInterface
    public interface CandidateAnalysisTask {
        Map<String, Object> analyze(RankedCandidate candidate, JobDescription jobDescription) throws Exception;
    }

    /**
     * Use Claude Haiku to analyze all candidates against the job description.
     * Sends candidates in batches to avoid response truncation.
     */
    private String analyzeAllCandidatesWithClaude(List<Candidate> candidates, JobDescription jobDescription) {
        String jobReqSkills = String.join(", ", jobDescription.getRequiredSkills());
        String jobPrefSkills = String.join(", ", jobDescription.getPreferredSkills());

        // Build compact candidate list using Candidate.toString()
        String candidatesJson = "[" + candidates.stream()
                .map(Candidate::toString)
                .collect(Collectors.joining(",")) + "]";

        String prompt = String.format("""
                Rank candidates against this job. Return ONLY a JSON array, no markdown.
                
                JOB: %s | Required: %s | Preferred: %s | Level: %s | minYearsExperience: %s | maxYearsExperience: %s
                
                CANDIDATES: %s
                
                For EACH candidate return EXACTLY this JSON (keep reasoning under 80 chars):
                {"candidateId":"id","matchPercentage":85.5,"skillMatchPercentage":80,"experienceMatchPercentage":90,"matchedSkills":["s1","s2"],"missingSkills":["s3"],"matchReasoning":"short reason","fitAnalysis":"short analysis"}
                
                Rules: matchPercentage = 0.60*skill + 0.25*experience + 0.15*soft. Be concise.
                """,
                jobDescription.getJobTitle(), jobReqSkills, jobPrefSkills,
                jobDescription.getExperienceLevel(),
                jobDescription.getMinYearsOfExperience() != null ? jobDescription.getMinYearsOfExperience() : "N/A",
                jobDescription.getMaxYearsOfExperience() != null ? jobDescription.getMaxYearsOfExperience() : "N/A",
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
                    .mobile(candidate.getMobile() != null ? candidate.getMobile() : "N/A")
                    .experience(candidate.getYearsOfExperience())
                    .designation(candidate.getCurrentDesignation() != null ? candidate.getCurrentDesignation() : candidate.getDesignation())
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

        if (startIdx == -1) {
            startIdx = jsonString.indexOf('{');
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

        // If no array, look for JSON object
        if (startIdx == -1) {
            startIdx = text.indexOf('{');
            openChar = '{';
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
                if (c == '{' || c == '[') {
                    bracketCount++;
                    lastValidIndex = i;
                } else if (c == '}' || c == ']') {
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


}

