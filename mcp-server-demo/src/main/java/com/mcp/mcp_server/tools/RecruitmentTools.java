package com.mcp.mcp_server.tools;

import com.mcp.mcp_server.entity.Candidate;
import com.mcp.mcp_server.entity.JobDescription;
import com.mcp.mcp_server.entity.RankedCandidate;
import com.mcp.mcp_server.service.AIEnhancedCandidateRankingService;
import com.mcp.mcp_server.service.CandidateDetailAnalysisService;
import com.mcp.mcp_server.service.ZohoRecruitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * MCP Tools for intelligent candidate recruitment and job matching.
 * Exposes a single unified tool that fetches candidates from Zoho Recruit,
 * ranks them using AI, and returns the top N results.
 *
 * Job description parsing is NOT done here — the caller (MCP client / AI agent)
 * is expected to parse the JD once and pass structured fields.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecruitmentTools {

    private final ZohoRecruitService zohoRecruitService;
    private final AIEnhancedCandidateRankingService aiEnhancedCandidateRankingService;
    private final CandidateDetailAnalysisService candidateDetailAnalysisService;

    /**
     * Single unified recruitment tool: fetch → rank → return top N candidates.
     */
    @Tool(description = """
            Finds and ranks the best candidates from Zoho Recruit ATS for a given job.
            Accepts pre-parsed job description fields (no raw JD text parsing is done here).
            
            Steps performed:
            1. Builds search criteria from the provided structured job fields
            2. Fetches matching candidate profiles from Zoho Recruit
            3. Ranks all candidates using Claude Haiku AI with semantic skill matching (SINGLE BATCH)
            4. Returns only the top N candidates sorted by match percentage (highest first)
            
            Each ranked candidate includes: name, email, phone, experience, match percentage,
            skill/experience match scores, matched/missing skills, and AI fit analysis.
            
            COST EFFECTIVE: Single batch ranking ensures consistent scoring and lower token usage.
            Default page size is 20 if not specified.
            """)
    public Map<String, Object> findAndRankCandidates(
            @ToolParam(description = "Job title or designation") String jobTitle,
            @ToolParam(description = "Comma-separated list of required skills") String requiredSkills,
            @ToolParam(description = "Comma-separated list of preferred/nice-to-have skills (optional)") String preferredSkills,
            @ToolParam(description = "Experience level: Junior, Mid, Senior, Lead, Executive (optional)") String experienceLevel,
            @ToolParam(description = "Minimum years of experience required (optional)") Integer minYearsOfExperience,
            @ToolParam(description = "Maximum years of experience required (optional)") Integer maxYearsOfExperience,
            @ToolParam(description = "Job location or 'Remote' (optional)") String location,
            @ToolParam(description = "Department name (optional)") String department,
            @ToolParam(description = "Number of top candidates to return (default: 20, max: 100)") Integer pageSize) {

        log.info("MCP tool called: findAndRankCandidates (cost-effective single batch ranking)");
        long startTime = System.currentTimeMillis();

        try {
            // 1. Build structured JobDescription from input params
            List<String> reqSkills = parseCommaSeparated(requiredSkills);
            List<String> prefSkills = parseCommaSeparated(preferredSkills);
            int topN = resolvePageSize(pageSize);

            JobDescription jd = JobDescription.builder()
                    .jobTitle(jobTitle)
                    .requiredSkills(reqSkills)
                    .preferredSkills(prefSkills)
                    .experienceLevel(experienceLevel)
                    .minYearsOfExperience(minYearsOfExperience)
                    .maxYearsOfExperience(maxYearsOfExperience)
                    .location(location)
                    .department(department)
                    .qualifications(Collections.emptyList())
                    .responsibilities(Collections.emptyList())
                    .build();

            // 2. Build search criteria & fetch from Zoho Recruit
            Map<String, String> searchCriteria = buildSearchCriteria(jd);
            List<Candidate> candidates = zohoRecruitService.searchCandidates(searchCriteria, Math.min(topN * 3, 200));

            log.info("Fetched {} candidates from Zoho Recruit in {}ms",
                    candidates.size(), System.currentTimeMillis() - startTime);

            if (candidates.isEmpty()) {
                return buildSuccessResult(jd, Collections.emptyList(), 0, startTime);
            }

            // 3. Rank using AI (single batch for consistency and cost-effectiveness)
            long rankStartTime = System.currentTimeMillis();
            List<RankedCandidate> ranked = aiEnhancedCandidateRankingService.rankCandidatesWithAI(candidates, jd);
            long rankingTimeMs = System.currentTimeMillis() - rankStartTime;
            log.info("Ranked {} candidates using AI (single batch) in {}ms (avg: {}ms per candidate)",
                    ranked.size(), rankingTimeMs, ranked.isEmpty() ? 0 : rankingTimeMs / ranked.size());

            // 4. Return top N only
            List<RankedCandidate> topCandidates = ranked.stream().limit(topN).toList();

            return buildSuccessResult(jd, topCandidates, candidates.size(), startTime);

        } catch (Exception e) {
            log.error("Error in findAndRankCandidates", e);
            return Map.of(
                    "success", false,
                    "error", "Failed to find and rank candidates: " + e.getMessage()
            );
        }
    }

    /**
     * Finds and ranks candidates with parallel batch processing for speed optimization.
     * Use this when speed is more important than cost.
     */
    @Tool(description = """
            Finds and ranks the best candidates from Zoho Recruit ATS with PARALLEL BATCH RANKING.
            
            Steps performed:
            1. Builds search criteria from the provided structured job fields
            2. Fetches matching candidate profiles from Zoho Recruit
            3. Splits candidates into batches (10 per batch) and ranks each batch in PARALLEL
            4. Merges and sorts all results by match percentage
            5. Returns only the top N candidates
            
            SPEED OPTIMIZED: Parallel batch ranking is ~2-3x faster than single batch.
            COST TRADE-OFF: Uses ~25-50% more tokens due to repeated prompt overhead.
            
            Use this tool when:
            - Speed is critical (real-time recruiting scenarios)
            - Budget allows for higher token usage
            - You need results quickly
            
            Default page size is 20 if not specified.
            """)
    public Map<String, Object> findAndRankCandidatesWithBatching(
            @ToolParam(description = "Job title or designation") String jobTitle,
            @ToolParam(description = "Comma-separated list of required skills") String requiredSkills,
            @ToolParam(description = "Comma-separated list of preferred/nice-to-have skills (optional)") String preferredSkills,
            @ToolParam(description = "Experience level: Junior, Mid, Senior, Lead, Executive (optional)") String experienceLevel,
            @ToolParam(description = "Minimum years of experience required (optional)") Integer minYearsOfExperience,
            @ToolParam(description = "Maximum years of experience required (optional)") Integer maxYearsOfExperience,
            @ToolParam(description = "Job location or 'Remote' (optional)") String location,
            @ToolParam(description = "Department name (optional)") String department,
            @ToolParam(description = "Number of top candidates to return (default: 20, max: 100)") Integer pageSize) {

        log.info("MCP tool called: findAndRankCandidatesWithBatching (speed-optimized parallel batch ranking)");
        long startTime = System.currentTimeMillis();

        try {
            // 1. Build structured JobDescription from input params
            List<String> reqSkills = parseCommaSeparated(requiredSkills);
            List<String> prefSkills = parseCommaSeparated(preferredSkills);
            int topN = resolvePageSize(pageSize);

            JobDescription jd = JobDescription.builder()
                    .jobTitle(jobTitle)
                    .requiredSkills(reqSkills)
                    .preferredSkills(prefSkills)
                    .experienceLevel(experienceLevel)
                    .minYearsOfExperience(minYearsOfExperience)
                    .maxYearsOfExperience(maxYearsOfExperience)
                    .location(location)
                    .department(department)
                    .qualifications(Collections.emptyList())
                    .responsibilities(Collections.emptyList())
                    .build();

            // 2. Build search criteria & fetch from Zoho Recruit
            Map<String, String> searchCriteria = buildSearchCriteria(jd);
            List<Candidate> candidates = zohoRecruitService.searchCandidates(searchCriteria, Math.min(topN * 3, 200));

            log.info("Fetched {} candidates from Zoho Recruit in {}ms",
                    candidates.size(), System.currentTimeMillis() - startTime);

            if (candidates.isEmpty()) {
                return buildSuccessResult(jd, Collections.emptyList(), 0, startTime);
            }

            // 3. Rank using parallel batch processing for speed
            long rankStartTime = System.currentTimeMillis();
            List<RankedCandidate> ranked = aiEnhancedCandidateRankingService.rankCandidatesWithAIBatching(candidates, jd);
            long rankingTimeMs = System.currentTimeMillis() - rankStartTime;
            log.info("Ranked {} candidates using parallel batch ranking in {}ms (avg: {}ms per candidate)",
                    ranked.size(), rankingTimeMs, ranked.isEmpty() ? 0 : rankingTimeMs / ranked.size());

            // 4. Return top N only
            List<RankedCandidate> topCandidates = ranked.stream().limit(topN).toList();

            return buildSuccessResult(jd, topCandidates, candidates.size(), startTime);

        } catch (Exception e) {
            log.error("Error in findAndRankCandidatesWithBatching", e);
            return Map.of(
                    "success", false,
                    "error", "Failed to find and rank candidates with batching: " + e.getMessage()
            );
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────────────────

    private Map<String, Object> buildSuccessResult(JobDescription jd,
                                                    List<RankedCandidate> topCandidates,
                                                    int totalFetched,
                                                    long startTime) {
        List<Map<String, Object>> output = new ArrayList<>(topCandidates.size());
        for (int i = 0; i < topCandidates.size(); i++) {
            RankedCandidate rc = topCandidates.get(i);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("rankPosition", i + 1);
            m.put("candidateId", rc.getCandidateId());
            m.put("name", rc.getName());
            m.put("email", rc.getEmail());
            m.put("mobile", rc.getMobile() != null ? rc.getMobile() : "N/A");
            m.put("experience", rc.getExperience());
            m.put("designation", rc.getDesignation() != null ? rc.getDesignation() : "N/A");
            m.put("matchPercentage", rc.getMatchPercentage());
            m.put("skillMatchPercentage", rc.getSkillMatchPercentage() != null ? rc.getSkillMatchPercentage() : 0.0);
            m.put("experienceMatchPercentage", rc.getExperienceMatchPercentage() != null ? rc.getExperienceMatchPercentage() : 0.0);
            m.put("matchedSkills", rc.getMatchedSkills());
            m.put("missingSkills", rc.getMissingSkills());
            m.put("fitAnalysis", rc.getFitAnalysis());
            m.put("matchReasoning", rc.getMatchReasoning());
            output.add(m);
        }

        long elapsed = System.currentTimeMillis() - startTime;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("jobTitle", jd.getJobTitle());
        result.put("requiredSkills", jd.getRequiredSkills());
        result.put("preferredSkills", jd.getPreferredSkills());
        result.put("experienceLevel", jd.getExperienceLevel());
        result.put("totalCandidatesFetched", totalFetched);
        result.put("totalCandidatesReturned", topCandidates.size());
        result.put("rankedCandidates", output);
        result.put("elapsedMs", elapsed);
        result.put("message", topCandidates.isEmpty()
                ? "No candidates found matching the criteria"
                : String.format("Returning top %d of %d candidates for %s",
                topCandidates.size(), totalFetched, jd.getJobTitle()));
        return result;
    }

    private Map<String, String> buildSearchCriteria(JobDescription jd) {
        Map<String, String> criteria = new LinkedHashMap<>();
        if (jd.getJobTitle() != null && !jd.getJobTitle().isBlank()) {
            criteria.put("designation", jd.getJobTitle());
        }
        if (!jd.getRequiredSkills().isEmpty()) {
            criteria.put("skills", jd.getRequiredSkills().stream().limit(3).collect(Collectors.joining(",")));
        }
        if (jd.getMinYearsOfExperience() != null) {
            criteria.put("min_experience_years", String.valueOf(jd.getMinYearsOfExperience()));
        }
        if (jd.getMaxYearsOfExperience() != null) {
            criteria.put("max_experience_years", String.valueOf(jd.getMaxYearsOfExperience()));
        }
        if (jd.getLocation() != null && !jd.getLocation().isBlank()) {
            criteria.put("location", jd.getLocation());
        }
        return criteria;
    }

    private List<String> parseCommaSeparated(String input) {
        if (input == null || input.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.stream(input.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    private int resolvePageSize(Integer pageSize) {
        if (pageSize == null || pageSize <= 0) return 20;
        return Math.min(pageSize, 100);
    }

    /**
     * Generate detailed fit analysis for multiple candidates in parallel.
     * Each candidate is analyzed concurrently to maximize performance.
     */
    @Tool(description = """
            Generates detailed fit analysis for multiple candidates in parallel.
            Each candidate receives a comprehensive assessment including strengths,
            weaknesses, cultural fit, estimated ramp-up time, and hiring recommendation.
            
            Analysis is performed concurrently for optimal performance.
            
            Returns map of candidateId -> detailed fit analysis.
            """)
    public Map<String, Object> generateDetailedAnalysisForMultipleCandidates(
            @ToolParam(description = "Comma-separated list of candidate IDs to analyze") String candidateIds,
            @ToolParam(description = "Job title") String jobTitle,
            @ToolParam(description = "Comma-separated required skills") String requiredSkills,
            @ToolParam(description = "Comma-separated preferred skills (optional)") String preferredSkills,
            @ToolParam(description = "Experience level (optional)") String experienceLevel) {

        log.info("MCP tool called: generateDetailedAnalysisForMultipleCandidates");
        long startTime = System.currentTimeMillis();

        try {
            // Parse input
            List<String> ids = parseCommaSeparated(candidateIds);
            if (ids.isEmpty()) {
                return Map.of("success", false, "error", "No candidate IDs provided");
            }

            // Build job description
            JobDescription jd = JobDescription.builder()
                    .jobTitle(jobTitle)
                    .requiredSkills(parseCommaSeparated(requiredSkills))
                    .preferredSkills(parseCommaSeparated(preferredSkills))
                    .experienceLevel(experienceLevel)
                    .qualifications(Collections.emptyList())
                    .responsibilities(Collections.emptyList())
                    .build();

            // Create mock RankedCandidates (in real scenario, these come from ranking)
            // For this tool, we'll create basic candidates with IDs
            List<RankedCandidate> candidates = ids.stream()
                    .map(id -> RankedCandidate.builder()
                            .candidateId(id)
                            .name(id)
                            .matchPercentage(0.0)
                            .build())
                    .toList();

            // Run parallel analysis for fit details
            long analysisStartTime = System.currentTimeMillis();
            Map<String, Map<String, Object>> fitAnalysis = aiEnhancedCandidateRankingService
                    .analyzeMultipleCandidatesInParallel(
                            candidates,
                            jd,
                            candidateDetailAnalysisService::generateDetailedFitAnalysis
                    );

            long analysisTimeMs = System.currentTimeMillis() - analysisStartTime;
            log.info("Parallel fit analysis completed for {} candidates in {}ms",
                    fitAnalysis.size(), analysisTimeMs);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", true);
            result.put("jobTitle", jobTitle);
            result.put("totalCandidatesAnalyzed", fitAnalysis.size());
            result.put("analysisType", "fitAnalysis");
            result.put("detailedAnalysis", fitAnalysis);
            result.put("elapsedMs", System.currentTimeMillis() - startTime);

            return result;

        } catch (Exception e) {
            log.error("Error in generateDetailedAnalysisForMultipleCandidates", e);
            return Map.of(
                    "success", false,
                    "error", "Failed to analyze candidates: " + e.getMessage()
            );
        }
    }

    /**
     * Generate interview questions for multiple candidates in parallel.
     * Each candidate receives tailored technical, behavioral, cultural, and gap-filling questions.
     */
    @Tool(description = """
            Generates customized interview questions for multiple candidates in parallel.
            Questions are tailored to each candidate's matched/missing skills and the job requirements.
            
            Includes technical, behavioral, cultural fit, and gap-filling question categories.
            
            Returns map of candidateId -> interview questions by category.
            """)
    public Map<String, Object> generateInterviewQuestionsForMultipleCandidates(
            @ToolParam(description = "Comma-separated list of candidate IDs") String candidateIds,
            @ToolParam(description = "Job title") String jobTitle,
            @ToolParam(description = "Comma-separated required skills") String requiredSkills,
            @ToolParam(description = "Comma-separated preferred skills (optional)") String preferredSkills) {

        log.info("MCP tool called: generateInterviewQuestionsForMultipleCandidates");
        long startTime = System.currentTimeMillis();

        try {
            // Parse input
            List<String> ids = parseCommaSeparated(candidateIds);
            if (ids.isEmpty()) {
                return Map.of("success", false, "error", "No candidate IDs provided");
            }

            // Build job description
            JobDescription jd = JobDescription.builder()
                    .jobTitle(jobTitle)
                    .requiredSkills(parseCommaSeparated(requiredSkills))
                    .preferredSkills(parseCommaSeparated(preferredSkills))
                    .qualifications(Collections.emptyList())
                    .responsibilities(Collections.emptyList())
                    .build();

            // Create mock RankedCandidates
            List<RankedCandidate> candidates = ids.stream()
                    .map(id -> RankedCandidate.builder()
                            .candidateId(id)
                            .name(id)
                            .matchPercentage(0.0)
                            .build())
                    .toList();

            // Run parallel question generation
            long genStartTime = System.currentTimeMillis();
            Map<String, Map<String, Object>> questions = aiEnhancedCandidateRankingService
                    .analyzeMultipleCandidatesInParallel(
                            candidates,
                            jd,
                            candidateDetailAnalysisService::generateInterviewQuestions
                    );

            long genTimeMs = System.currentTimeMillis() - genStartTime;
            log.info("Parallel interview question generation completed for {} candidates in {}ms",
                    questions.size(), genTimeMs);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", true);
            result.put("jobTitle", jobTitle);
            result.put("totalCandidatesProcessed", questions.size());
            result.put("analysisType", "interviewQuestions");
            result.put("generatedQuestions", questions);
            result.put("elapsedMs", System.currentTimeMillis() - startTime);

            return result;

        } catch (Exception e) {
            log.error("Error in generateInterviewQuestionsForMultipleCandidates", e);
            return Map.of(
                    "success", false,
                    "error", "Failed to generate interview questions: " + e.getMessage()
            );
        }
    }

    /**
     * Generate strengths and weaknesses assessment for multiple candidates in parallel.
     */
    @Tool(description = """
            Generates strengths and weaknesses assessment for multiple candidates in parallel.
            Identifies technical strengths, soft skill strengths, technical gaps, and development areas.
            
            Returns map of candidateId -> strengths/weaknesses analysis.
            """)
    public Map<String, Object> generateStrengthsAndWeaknessesForMultipleCandidates(
            @ToolParam(description = "Comma-separated list of candidate IDs") String candidateIds,
            @ToolParam(description = "Job title") String jobTitle,
            @ToolParam(description = "Experience level required") String experienceLevel,
            @ToolParam(description = "Comma-separated required skills") String requiredSkills) {

        log.info("MCP tool called: generateStrengthsAndWeaknessesForMultipleCandidates");
        long startTime = System.currentTimeMillis();

        try {
            // Parse input
            List<String> ids = parseCommaSeparated(candidateIds);
            if (ids.isEmpty()) {
                return Map.of("success", false, "error", "No candidate IDs provided");
            }

            // Build job description
            JobDescription jd = JobDescription.builder()
                    .jobTitle(jobTitle)
                    .experienceLevel(experienceLevel)
                    .requiredSkills(parseCommaSeparated(requiredSkills))
                    .preferredSkills(Collections.emptyList())
                    .qualifications(Collections.emptyList())
                    .responsibilities(Collections.emptyList())
                    .build();

            // Create mock RankedCandidates
            List<RankedCandidate> candidates = ids.stream()
                    .map(id -> RankedCandidate.builder()
                            .candidateId(id)
                            .name(id)
                            .matchPercentage(0.0)
                            .build())
                    .toList();

            // Run parallel strengths/weaknesses analysis
            long analysisStartTime = System.currentTimeMillis();
            Map<String, Map<String, Object>> assessment = aiEnhancedCandidateRankingService
                    .analyzeMultipleCandidatesInParallel(
                            candidates,
                            jd,
                            candidateDetailAnalysisService::generateStrengthsAndWeaknesses
                    );

            long analysisTimeMs = System.currentTimeMillis() - analysisStartTime;
            log.info("Parallel strengths/weaknesses analysis completed for {} candidates in {}ms",
                    assessment.size(), analysisTimeMs);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", true);
            result.put("jobTitle", jobTitle);
            result.put("totalCandidatesAssessed", assessment.size());
            result.put("analysisType", "strengthsAndWeaknesses");
            result.put("assessment", assessment);
            result.put("elapsedMs", System.currentTimeMillis() - startTime);

            return result;

        } catch (Exception e) {
            log.error("Error in generateStrengthsAndWeaknessesForMultipleCandidates", e);
            return Map.of(
                    "success", false,
                    "error", "Failed to generate strengths/weaknesses assessment: " + e.getMessage()
            );
        }
    }
}
