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
                  "minYearsOfExperience": number or null,
                  "maxYearsOfExperience": number or null,
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

                IMPORTANT - Experience Range Extraction:
                - Extract BOTH minimum and maximum years if a range is specified (e.g., "5-10 years" → min: 5, max: 10)
                - If only one number is given with "years", use it as BOTH min and max
                - If experience level is given (Junior/Mid/Senior), map it: Junior=0, Mid=3, Senior=7, Lead=10
                - Priority: Explicit year range > Explicit single year > Experience level

                Be comprehensive and extract all relevant skills, requirements, and benefits mentioned.
                Mapping Guide:
                - jobTitle → Current_Job_Title (Zoho field)
                - experienceLevel → Experience level category (Zoho field)
                - minYearsOfExperience → Minimum Experience_in_Years (Zoho field)
                - maxYearsOfExperience → Maximum Experience_in_Years (Zoho field)
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
            String cleanedResponse = extractAndValidateJSON(aiResponse);
            log.debug("Parsing cleaned JSON response: {}", cleanedResponse);

            JsonNode jsonNode = objectMapper.readTree(cleanedResponse);

            return JobDescription.builder()
                    .jobTitle(getTextValue(jsonNode, "jobTitle", "Not Specified"))
                    .jobDescription(originalJD)
                    .experienceLevel(getTextValue(jsonNode, "experienceLevel", "Mid"))
                    .minYearsOfExperience(getIntValue(jsonNode, "minYearsOfExperience"))
                    .maxYearsOfExperience(getIntValue(jsonNode, "maxYearsOfExperience"))
                    .requiredSkills(getListValue(jsonNode, "requiredSkills"))
                    .preferredSkills(getListValue(jsonNode, "preferredSkills"))
                    .qualifications(getListValue(jsonNode, "qualifications"))
                    .responsibilities(getListValue(jsonNode, "responsibilities"))
                    .department(getTextValue(jsonNode, "department", "Not Specified"))
                    .location(getTextValue(jsonNode, "location", "Not Specified"))
                    .build();
        } catch (Exception e) {
            log.error("Failed to parse AI response. Response preview: {}",
                    aiResponse.length() > 500 ? aiResponse.substring(0, 500) + "..." : aiResponse, e);
            throw new RuntimeException("Failed to parse AI response for job description: " + e.getMessage(), e);
        }
    }

    /**
     * Extract valid JSON from AI response with robust error handling
     * Handles markdown code blocks and validates JSON structure
     */
    private String extractAndValidateJSON(String aiResponse) {
        if (aiResponse == null || aiResponse.trim().isEmpty()) {
            throw new IllegalArgumentException("AI response is empty or null");
        }

        // Step 1: Remove markdown code blocks
        String cleaned = aiResponse
                .replaceAll("```(?:json)?\\s*\\n?", "")
                .replaceAll("```\\s*\\n?", "")
                .trim();

        // Step 2: Extract JSON using bracket matching for robustness
        String extracted = extractJsonByBracketMatching(cleaned);

        if (extracted == null || extracted.isEmpty()) {
            log.error("Could not extract valid JSON from response: {}", cleaned.substring(0, Math.min(200, cleaned.length())));
            throw new IllegalArgumentException("No valid JSON object found in AI response");
        }

        return extracted;
    }

    /**
     * Extract JSON from text using proper bracket matching
     * This avoids the naive "first { to last }" approach
     */
    private String extractJsonByBracketMatching(String text) {
        int startIdx = text.indexOf('{');
        if (startIdx == -1) {
            return null;
        }

        int braceCount = 0;
        boolean inString = false;
        boolean previousCharEscaped = false;

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

            // Only count braces outside of strings
            if (!inString) {
                if (c == '{') {
                    braceCount++;
                } else if (c == '}') {
                    braceCount--;
                    if (braceCount == 0) {
                        // Found matching closing brace
                        String potential = text.substring(startIdx, i + 1);
                        log.debug("Extracted potential JSON: {}", potential.substring(0, Math.min(100, potential.length())) + "...");
                        return potential;
                    }
                }
            }

            // Update escape status for next iteration
            previousCharEscaped = currentCharEscaped;
        }

        // If we get here, JSON was not properly closed
        if (braceCount > 0) {
            log.warn("Unclosed JSON object - found {} unmatched opening braces", braceCount);
            return null;
        }

        return null;
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

}

