package com.mcp.mcp_server.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mcp.mcp_server.entity.JobDescription;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * AI-Enhanced Job Description Parsing Service using Claude Haiku
 * Provides intelligent extraction of JD metadata using LLM analysis
 *
 * This service also generates Zoho Recruit criteria expressions for
 * candidate search and filtering based on parsed job descriptions.
 *
 * Criteria Format (Zoho Recruit API):
 * - Single condition: (Field:operator:value)
 * - Chained (up to 10): ((Field1:op:val1)and(Field2:op:val2))
 * - Operators: equals, not_equal, contains, starts_with, greater_than, greater_equal, less_than, less_equal
 * - Numeric fields: Experience_in_Years, Current_Salary, Expected_Salary
 * - String fields: all others use string comparison
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AIEnhancedJobDescriptionService {

    private final ObjectProvider<ChatClient> chatClientProvider;
    private final ObjectMapper objectMapper;

    private ChatClient getChatClient() {
        try {
            return chatClientProvider.getObject();
        } catch (Exception e) {
            log.error("Failed to get ChatClient: {}", e.getMessage(), e);
            throw new RuntimeException("ChatClient initialization failed. Please check if ANTHROPIC_API_KEY environment variable is set.", e);
        }
    }

    /**
     * Parse job description using Claude Haiku AI
     */
    public JobDescription parseJobDescriptionWithAI(String jobDescription) {
        log.debug("Parsing job description using Claude Haiku AI");

        try {
            // Verify API key is available
            String apiKey = System.getenv("ANTHROPIC_API_KEY");
            if (apiKey == null || apiKey.trim().isEmpty()) {
                log.error("ANTHROPIC_API_KEY environment variable is not set");
                throw new IllegalStateException("ANTHROPIC_API_KEY environment variable is required but not set");
            }

            // Use Claude to intelligently extract JD metadata
            String aiAnalysis = analyzeJobDescriptionWithClaude(jobDescription);
            return buildJobDescriptionFromAIAnalysis(aiAnalysis, jobDescription);
        } catch (Exception e) {
            log.error("AI parsing failed: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to parse job description using AI: " + e.getMessage(), e);
        }
    }

    /**
     * Use Claude Haiku to analyze job description
     */
    private String analyzeJobDescriptionWithClaude(String jobDescription) {
        String prompt = String.format("""
                Analyze the following job description and extract structured metadata as JSON.
                Return ONLY a valid JSON object (no markdown, no extra text) with these exact fields:
                {
                  "jobTitle": "extracted job title or designation",
                  "experienceLevel": "Junior/Mid/Senior/Executive",
                  "yearsOfExperience": number or null,
                  "requiredSkills": ["skill1", "skill2", "skill3", ...],
                  "preferredSkills": ["skill1", "skill2", ...],
                  "qualifications": ["qualification1", "qualification2", ...],
                  "responsibilities": ["responsibility1", "responsibility2", ...],
                  "department": "department name or null",
                  "location": "city/location or 'Remote' or null",
                  "state": "state or null",
                  "country": "country or 'India' or null",
                  "highestQualificationRequired": "B.Tech/M.Tech/MBA or null",
                  "industryOrEmployer": "expected industry or employer type or null",
                  "salaryRange": "salary range if mentioned or null"
                }

                Be comprehensive and extract all relevant skills, requirements, and benefits mentioned.
                Mapping Guide:
                - jobTitle → Current_Job_Title (Zoho field)
                - experienceLevel → Experience_in_Years (converted: Junior=0, Mid=3, Senior=7, Lead=10)
                - yearsOfExperience → Experience_in_Years (Zoho field)
                - requiredSkills → Skill_Set (Zoho field)
                - preferredSkills → Skill_Set (Zoho field)
                - qualifications → Highest_Qualification_Held (Zoho field)
                - location → City (Zoho field)
                - state → State (Zoho field)
                - country → Country (Zoho field)
                - highestQualificationRequired → Highest_Qualification_Held (Zoho field)
                - department → Department (extracted metadata)
                - responsibilities → Responsibilities (extracted metadata)
                - industryOrEmployer → Current_Employer (Zoho field)
                - salaryRange → Current_Salary/Expected_Salary (Zoho fields)

                Job Description:
                %s
                """, jobDescription);

         log.debug("Claude AI Analysis Prompt: {}", prompt);
         String response = getChatClient().prompt()
                 .user(prompt)
                 .call()
                 .content();

         log.debug("Claude AI Analysis Response: {}", response);
         return response;
    }

    /**
     * Build JobDescription entity from AI analysis
     */
    private JobDescription buildJobDescriptionFromAIAnalysis(String aiResponse, String originalJD) {
        try {
            // Clean the response (remove markdown code blocks if present)
            String cleanedResponse = aiResponse
                    .replaceAll("```json\\n?", "")
                    .replaceAll("```\\n?", "")
                    .trim();

            // Try to extract JSON if there's extra text (Claude might add explanation)
            // Look for the first { and last } to extract just the JSON
            int jsonStart = cleanedResponse.indexOf('{');
            int jsonEnd = cleanedResponse.lastIndexOf('}');

            if (jsonStart != -1 && jsonEnd != -1 && jsonEnd > jsonStart) {
                cleanedResponse = cleanedResponse.substring(jsonStart, jsonEnd + 1);
                log.debug("Extracted JSON from response: {}", cleanedResponse);
            }

            JsonNode jsonNode = objectMapper.readTree(cleanedResponse);

            return JobDescription.builder()
                    .jobTitle(getTextValue(jsonNode, "jobTitle", "Not Specified"))
                    .jobDescription(originalJD)
                    .experienceLevel(getTextValue(jsonNode, "experienceLevel", "Mid"))
                    .yearsOfExperience(getIntValue(jsonNode, "yearsOfExperience"))
                    .requiredSkills(getListValue(jsonNode, "requiredSkills"))
                    .preferredSkills(getListValue(jsonNode, "preferredSkills"))
                    .qualifications(getListValue(jsonNode, "qualifications"))
                    .responsibilities(getListValue(jsonNode, "responsibilities"))
                    .department(getTextValue(jsonNode, "department", "Not Specified"))
                    .location(getTextValue(jsonNode, "location", "Not Specified"))
                    .build();
        } catch (Exception e) {
            log.error("Failed to parse AI response: {}", aiResponse, e);
            throw new RuntimeException("Failed to parse AI response for job description: " + e.getMessage(), e);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // JSON helper methods
    // ─────────────────────────────────────────────────────────────────────────

    private String getTextValue(JsonNode node, String field, String defaultValue) {
        if (node.has(field) && !node.get(field).isNull()) {
            return node.get(field).asText();
        }
        return defaultValue;
    }

    private Integer getIntValue(JsonNode node, String field) {
        if (node.has(field) && !node.get(field).isNull()) {
            return node.get(field).asInt();
        }
        return null;
    }

    private List<String> getListValue(JsonNode node, String field) {
        List<String> list = new ArrayList<>();
        if (node.has(field) && node.get(field).isArray()) {
            for (JsonNode item : node.get(field)) {
                list.add(item.asText());
            }
        }
        return list;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Zoho Recruit Criteria Generation Methods
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Generate Zoho Recruit search criteria from a parsed job description
     *
     * Creates a criteria expression for searching candidates matching the JD.
     * Format: (Field:operator:value) or ((Field1:op:val1)and(Field2:op:val2))
     *
     * @param jd The parsed job description
     * @return Zoho Recruit criteria string
     */
    public String generateZohoCriteria(JobDescription jd) {
        log.debug("Generating criteria for: {}", jd.getJobTitle());

        ZohoCriteriaBuilder.CriteriaFilter filter = new ZohoCriteriaBuilder.CriteriaFilter();

        // Add required skills
        if (jd.getRequiredSkills() != null) {
            jd.getRequiredSkills().stream()
                    .filter(s -> s != null && !s.trim().isEmpty())
                    .forEach(filter::addSkill);
        }

        // Add experience requirement
        if (jd.getYearsOfExperience() != null && jd.getYearsOfExperience() > 0) {
            filter.addExperience(jd.getYearsOfExperience());
        }

        // Add location
        if (jd.getLocation() != null && !jd.getLocation().isEmpty()
                && !jd.getLocation().equals("Not Specified")) {
            filter.addLocation(jd.getLocation());
        }

        // Add experience level-based requirement
        if (jd.getExperienceLevel() != null) {
            int minYears = mapExperienceLevelToYears(jd.getExperienceLevel());
            if (minYears > 0) {
                filter.addExperience(minYears);
            }
        }

        return filter.build();
    }

    /**
     * Generate criteria including preferred skills
     *
     * @param jd The parsed job description
     * @param includePreferred Include preferred skills in search
     * @return Zoho Recruit criteria string
     */
    public String generateZohoCriteriaWithPreferences(JobDescription jd, boolean includePreferred) {
        ZohoCriteriaBuilder.CriteriaFilter filter = new ZohoCriteriaBuilder.CriteriaFilter();

        // Add required skills
        if (jd.getRequiredSkills() != null) {
            jd.getRequiredSkills().stream()
                    .filter(s -> s != null && !s.trim().isEmpty())
                    .forEach(filter::addSkill);
        }

        // Add preferred skills if requested
        if (includePreferred && jd.getPreferredSkills() != null) {
            jd.getPreferredSkills().stream()
                    .filter(s -> s != null && !s.trim().isEmpty())
                    .forEach(filter::addSkill);
        }

        // Add experience and location
        if (jd.getYearsOfExperience() != null && jd.getYearsOfExperience() > 0) {
            filter.addExperience(jd.getYearsOfExperience());
        }
        if (jd.getLocation() != null && !jd.getLocation().isEmpty()
                && !jd.getLocation().equals("Not Specified")) {
            filter.addLocation(jd.getLocation());
        }

        return filter.build();
    }

    private int mapExperienceLevelToYears(String level) {
        if (level == null) return 0;
        return switch (level.toLowerCase()) {
            case "junior", "entry-level", "entry level" -> 0;
            case "mid", "mid-level", "mid level", "intermediate" -> 3;
            case "senior", "senior-level", "senior level" -> 7;
            case "lead", "principal", "executive", "c-level" -> 10;
            default -> 0;
        };
    }
}

