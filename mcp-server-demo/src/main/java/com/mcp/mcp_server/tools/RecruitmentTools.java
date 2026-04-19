package com.mcp.mcp_server.tools;

import com.mcp.mcp_server.entity.Candidate;
import com.mcp.mcp_server.entity.JobDescription;
import com.mcp.mcp_server.entity.RankedCandidate;
import com.mcp.mcp_server.service.AIEnhancedCandidateRankingService;
import com.mcp.mcp_server.service.AIEnhancedJobDescriptionService;
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
 * Integrates with Zoho Recruit ATS and Claude Haiku AI for intelligent analysis.
 *
 * This is a pure AI-enhanced implementation using only:
 * - AIEnhancedJobDescriptionService (Claude-powered JD parsing)
 * - AIEnhancedCandidateRankingService (Claude-powered candidate ranking)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecruitmentTools {

    private final AIEnhancedJobDescriptionService aiEnhancedJobDescriptionService;
    private final ZohoRecruitService zohoRecruitService;
    private final AIEnhancedCandidateRankingService aiEnhancedCandidateRankingService;


    // ─────────────────────────────────────────────────────────────────────────
    // Task 1 – JD Ingestion: Parse and extract key information from JD
    // ─────────────────────────────────────────────────────────────────────────
    @Tool(description = """
            Ingests and parses a job description to extract structured metadata including:
            - Job title
            - Required and preferred skills
            - Experience level and years required
            - Key qualifications and responsibilities
            - Department and location
            
            Uses AI model for intelligent extraction of job description metadata.
            """)
    public Map<String, Object> parseJobDescription(
            @ToolParam(description = "The complete job description text to parse") String jobDescription) {

        log.debug("MCP tool called: parseJobDescription");

        try {
            JobDescription parsed = aiEnhancedJobDescriptionService.parseJobDescriptionWithAI(jobDescription);
            log.info("Job description parsed successfully using Claude Haiku AI");

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", true);
            result.put("jobTitle", parsed.getJobTitle());
            result.put("experienceLevel", parsed.getExperienceLevel());
            result.put("yearsOfExperience", parsed.getYearsOfExperience() != null ? parsed.getYearsOfExperience() : "Not specified");
            result.put("requiredSkills", parsed.getRequiredSkills());
            result.put("preferredSkills", parsed.getPreferredSkills());
            result.put("qualifications", parsed.getQualifications());
            result.put("responsibilities", parsed.getResponsibilities());
            result.put("department", parsed.getDepartment());
            result.put("location", parsed.getLocation());
            result.put("message", "Job description successfully parsed using AI");
            return result;
        } catch (Exception e) {
            log.error("Error parsing job description", e);
            return Map.of(
                    "success", false,
                    "error", "Failed to parse job description: " + e.getMessage()
            );
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Task 2 – LinkedIn/Zoho Search: Search candidates in Zoho Recruit ATS
    // ─────────────────────────────────────────────────────────────────────────
    @Tool(description = """
            Searches Zoho Recruit ATS for candidate profiles matching the given job description.
            
            Steps:
            1. Parses the job description using Claude Haiku AI
            2. Generates intelligent search queries/filters
            3. Retrieves matching candidate profiles from Zoho Recruit
            4. Handles pagination for large result sets
            
            Returns contact information and basic profile details for each candidate.
            """)
    public Map<String, Object> searchCandidatesInZohoRecruit(
            @ToolParam(description = "The job description to search candidates for") String jobDescription,
            @ToolParam(description = "Optional: Maximum number of candidates to return (default: 50)") Integer maxResults) {

        log.debug("MCP tool called: searchCandidatesInZohoRecruit");

        try {
            int limit = maxResults != null ? maxResults : 50;

            // Step 1: Parse the job description using AI
            JobDescription parsed = aiEnhancedJobDescriptionService.parseJobDescriptionWithAI(jobDescription);

            // Step 2: Generate search criteria from parsed JD
            Map<String, String> searchCriteria = generateSearchCriteria(parsed);

            // Step 3: Search Zoho Recruit
            List<Candidate> candidates = zohoRecruitService.searchCandidates(searchCriteria, 20);

            // Limit results
            candidates = candidates.stream().limit(limit).toList();

            // Step 4: Format output (PII-aware - only recruiter-relevant info)
            List<Map<String, Object>> candidateList = candidates.stream()
                    .map(candidate -> {
                        Map<String, Object> map = new LinkedHashMap<>();
                        map.put("candidateId", candidate.getCandidateId() != null ? candidate.getCandidateId() : "N/A");
                        map.put("name", candidate.getName() != null ? candidate.getName() : "N/A");
                        map.put("email", candidate.getEmail() != null ? candidate.getEmail() : "N/A");
                        map.put("phone", candidate.getPhone() != null ? candidate.getPhone() : "N/A");
                        map.put("currentPosition", candidate.getCurrentPosition() != null ? candidate.getCurrentPosition() : "N/A");
                        map.put("experience", candidate.getExperience() != null ? candidate.getExperience() : "N/A");
                        return map;
                    })
                    .collect(Collectors.toList());

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", true);
            result.put("jobTitle", parsed.getJobTitle());
            result.put("searchCriteria", searchCriteria);
            result.put("candidatesFound", candidates.size());
            result.put("candidates", candidateList);
            result.put("message", String.format("Found %d candidates matching the job description", candidates.size()));
            return result;
        } catch (Exception e) {
            log.error("Error searching candidates in Zoho Recruit", e);
            return Map.of(
                    "success", false,
                    "error", "Failed to search candidates: " + e.getMessage()
            );
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Task 3 & 4 – Ranking and Structured Output
    // ─────────────────────────────────────────────────────────────────────────
    @Tool(description = """
            Complete end-to-end recruitment pipeline with AI-powered analysis:
            1. Uses Claude Haiku to parse the job description intelligently
            2. Searches Zoho Recruit for matching candidates
            3. Uses Claude Haiku to rank candidates by semantic relevance with match percentage
            4. Returns a recruiter-ready ranked list with:
               - Candidate name, email, phone
               - Matched skills vs required skills
               - Match percentage (0-100%)
               - AI-powered fit analysis explaining strengths/gaps
               - Semantic skill matching beyond keyword matching
            
            Results are sorted by match percentage (highest first).
            """)
    public Map<String, Object> findAndRankCandidatesForJD(
            @ToolParam(description = "The complete job description") String jobDescription,
            @ToolParam(description = "Optional: Maximum number of candidates to return (default: 20)") Integer maxResults) {

        log.debug("MCP tool called: findAndRankCandidatesForJD");

        try {
            int limit = maxResults != null ? maxResults : 20;

            // Parse JD using AI
            JobDescription parsed = aiEnhancedJobDescriptionService.parseJobDescriptionWithAI(jobDescription);
            log.info("Job description parsed using Claude Haiku AI");

            // Generate search criteria
            Map<String, String> searchCriteria = generateSearchCriteria(parsed);

            // Search candidates
            List<Candidate> candidates = zohoRecruitService.searchCandidates(searchCriteria, 20);

            // Rank candidates using AI
            List<RankedCandidate> rankedCandidates = aiEnhancedCandidateRankingService.rankCandidatesWithAI(candidates, parsed);
            log.info("Ranked {} candidates using Claude Haiku AI", rankedCandidates.size());

            // Limit results
            rankedCandidates = rankedCandidates.stream().limit(limit).toList();

            // Format for output - with rankPosition added
            List<Map<String, Object>> output = new ArrayList<>();
            for (int i = 0; i < rankedCandidates.size(); i++) {
                RankedCandidate ranked = rankedCandidates.get(i);
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("candidateId", ranked.getCandidateId());
                map.put("name", ranked.getName());
                map.put("email", ranked.getEmail());
                map.put("phone", ranked.getPhone() != null ? ranked.getPhone() : "N/A");
                map.put("matchPercentage", ranked.getMatchPercentage());  // Numeric value, not formatted string
                map.put("skillMatchPercentage", ranked.getSkillMatchPercentage() != null ? ranked.getSkillMatchPercentage() : 0.0);
                map.put("experienceMatchPercentage", ranked.getExperienceMatchPercentage() != null ? ranked.getExperienceMatchPercentage() : 0.0);
                map.put("matchedSkills", ranked.getMatchedSkills());
                map.put("missingSkills", ranked.getMissingSkills());
                map.put("fitAnalysis", ranked.getFitAnalysis());
                map.put("matchReasoning", ranked.getMatchReasoning());
                map.put("rankPosition", i + 1);  // Position in ranked list (1-based)
                output.add(map);
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", true);
            result.put("jobTitle", parsed.getJobTitle());
            result.put("department", parsed.getDepartment());
            result.put("location", parsed.getLocation());
            result.put("requiredSkills", parsed.getRequiredSkills());
            result.put("preferredSkills", parsed.getPreferredSkills());
            result.put("experienceLevel", parsed.getExperienceLevel());
            result.put("yearsRequired", parsed.getYearsOfExperience() != null ? parsed.getYearsOfExperience() : "Not specified");
            result.put("totalCandidatesEvaluated", rankedCandidates.size());
            result.put("rankedCandidates", output);
            result.put("message", String.format("Successfully ranked %d candidates for %s position",
                    rankedCandidates.size(), parsed.getJobTitle()));
            return result;
        } catch (Exception e) {
            log.error("Error in end-to-end candidate ranking", e);
            return Map.of(
                    "success", false,
                    "error", "Failed to find and rank candidates: " + e.getMessage()
            );
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Utility: Generate intelligent search criteria from parsed JD
    // ─────────────────────────────────────────────────────────────────────────
    @Tool(description = """
            Generates intelligent search queries and filters from a job description.
            Uses Claude Haiku AI to parse and extract search criteria.
            These filters can be used to search Zoho Recruit or other talent databases.
            
            Returns:
            - Primary search terms (job title, key skills)
            - Filter criteria (experience level, location, etc.)
            - Boolean search operators for refined queries
            """)
    public Map<String, Object> generateSearchFiltersFromJD(
            @ToolParam(description = "The job description to generate search filters for") String jobDescription) {

        log.debug("MCP tool called: generateSearchFiltersFromJD");

        try {
            JobDescription parsed = aiEnhancedJobDescriptionService.parseJobDescriptionWithAI(jobDescription);
            Map<String, String> filters = generateSearchCriteria(parsed);

            // Build Boolean search query
            String booleanQuery = "\"" + parsed.getJobTitle() + "\" OR " +
                    String.join(" OR ", parsed.getRequiredSkills());

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", true);
            result.put("jobTitle", parsed.getJobTitle());
            result.put("primarySearchTerms", String.join(", ", parsed.getRequiredSkills()));
            result.put("experienceLevelFilter", parsed.getExperienceLevel());
            result.put("yearsOfExperienceFilter", parsed.getYearsOfExperience() != null ? parsed.getYearsOfExperience() : "Not specified");
            result.put("locationFilter", parsed.getLocation());
            result.put("departmentFilter", parsed.getDepartment());
            result.put("zohoRecruitFilters", filters);
            result.put("booleanSearchQuery", booleanQuery);
            result.put("requiredSkills", parsed.getRequiredSkills());
            result.put("preferredSkills", parsed.getPreferredSkills());
            return result;
        } catch (Exception e) {
            log.error("Error generating search filters", e);
            return Map.of(
                    "success", false,
                    "error", "Failed to generate search filters: " + e.getMessage()
            );
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helper Method: Generate search criteria from JobDescription
    // ─────────────────────────────────────────────────────────────────────────
    private Map<String, String> generateSearchCriteria(JobDescription jobDescription) {
        Map<String, String> criteria = new LinkedHashMap<>();

        // Add primary search terms
        if (jobDescription.getJobTitle() != null && !jobDescription.getJobTitle().isEmpty()) {
            criteria.put("designation", jobDescription.getJobTitle());
        }

        // Add key skills as search criteria
        if (!jobDescription.getRequiredSkills().isEmpty()) {
            String topSkills = jobDescription.getRequiredSkills().stream()
                    .limit(3)
                    .collect(Collectors.joining(","));
            criteria.put("skills", topSkills);
        }

        // Add experience level
        if (jobDescription.getExperienceLevel() != null) {
            criteria.put("experience_level", jobDescription.getExperienceLevel());
        }

        // Add location
        if (jobDescription.getLocation() != null && !jobDescription.getLocation().isEmpty()) {
            criteria.put("location", jobDescription.getLocation());
        }

        return criteria;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Advanced AI Feature: Detailed Candidate Fit Analysis
    // ─────────────────────────────────────────────────────────────────────────
    @Tool(description = """
            Get detailed AI-powered fit analysis for a specific candidate against a job description.
            Uses Claude Haiku to analyze:
            - Candidate strengths relative to the role
            - Skill gaps and weaknesses
            - Development areas and growth potential
            - Cultural and team fit assessment
            - Interview focus areas
            - Estimated ramp-up time
            
            Useful for pre-interview screening and decision making.
            """)
    public Map<String, Object> getDetailedCandidateFitAnalysis(
            @ToolParam(description = "The job description") String jobDescription,
            @ToolParam(description = "Candidate name") String candidateName,
            @ToolParam(description = "Candidate's current position") String currentPosition,
            @ToolParam(description = "Candidate's experience/background summary") String experience,
            @ToolParam(description = "Comma-separated list of candidate skills") String skills) {

        log.debug("MCP tool called: getDetailedCandidateFitAnalysis for {}", candidateName);

        try {
            // Build candidate object from parameters
            Candidate candidate = Candidate.builder()
                    .name(candidateName)
                    .currentPosition(currentPosition)
                    .experience(experience)
                    .skills(Arrays.stream(skills.split(","))
                            .map(String::trim)
                            .filter(s -> !s.isEmpty())
                            .collect(Collectors.toList()))
                    .build();

            // Parse job description using AI
            JobDescription parsed = aiEnhancedJobDescriptionService.parseJobDescriptionWithAI(jobDescription);

            // Get detailed analysis using AI
            return aiEnhancedCandidateRankingService.getDetailedFitAnalysis(candidate, parsed);
        } catch (Exception e) {
            log.error("Error getting detailed fit analysis", e);
            return Map.of(
                    "success", false,
                    "error", "Failed to analyze candidate fit: " + e.getMessage()
            );
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Advanced AI Feature: Generate Interview Questions
    // ─────────────────────────────────────────────────────────────────────────
    @Tool(description = """
            Generate AI-powered customized interview questions for a candidate and job.
            Claude Haiku generates targeted questions that assess:
            - Technical skills and domain knowledge
            - Behavioral indicators and soft skills
            - Culture fit and team alignment
            - Problem-solving abilities
            - Role-specific competencies
            
            Each question includes a category and rationale for why it matters.
            Perfect for structured interviews and evaluation consistency.
            """)
    public Map<String, Object> generateCustomInterviewQuestions(
            @ToolParam(description = "The job description") String jobDescription,
            @ToolParam(description = "Candidate name") String candidateName,
            @ToolParam(description = "Candidate's current position") String currentPosition,
            @ToolParam(description = "Candidate's experience/background summary") String experience,
            @ToolParam(description = "Comma-separated list of candidate skills") String skills,
            @ToolParam(description = "Optional: Number of questions to generate (default: 8)") Integer questionCount) {

        log.debug("MCP tool called: generateCustomInterviewQuestions for {}", candidateName);

        try {
            int count = questionCount != null ? Math.min(questionCount, 15) : 8;

            // Build candidate object from parameters
            Candidate candidate = Candidate.builder()
                    .name(candidateName)
                    .currentPosition(currentPosition)
                    .experience(experience)
                    .skills(Arrays.stream(skills.split(","))
                            .map(String::trim)
                            .filter(s -> !s.isEmpty())
                            .collect(Collectors.toList()))
                    .build();

            // Parse job description using AI
            JobDescription parsed = aiEnhancedJobDescriptionService.parseJobDescriptionWithAI(jobDescription);

            // Generate interview questions using AI
            return aiEnhancedCandidateRankingService.generateInterviewQuestions(candidate, parsed, count);
        } catch (Exception e) {
            log.error("Error generating interview questions", e);
            return Map.of(
                    "success", false,
                    "error", "Failed to generate interview questions: " + e.getMessage()
            );
        }
    }

}








