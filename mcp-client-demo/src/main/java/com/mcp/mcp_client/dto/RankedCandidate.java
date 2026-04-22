package com.mcp.mcp_client.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Represents a ranked candidate with match score and reasoning
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class RankedCandidate {

    @JsonAlias({"candidate_id", "id"})
    private String candidateId;

    @JsonAlias({"candidate_name", "full_name", "fullName"})
    private String name;

    @JsonAlias({"email_address", "emailAddress"})
    private String email;

    @JsonAlias({"phone_number", "phoneNumber", "mobile_number", "mobileNumber", "mobile", "Mobile"})
    private String mobile;

    @JsonAlias({"experience_years", "experienceYears", "years_of_experience", "yearsOfExperience"})
    private Integer experience;

    @JsonAlias({"current_designation", "currentDesignation", "job_title", "jobTitle", "current_job_title"})
    private String designation;

    private Double matchPercentage;

    @JsonAlias({"skill_match_percentage", "skillMatch", "skill_match"})
    private Double skillMatchPercentage;

    @JsonAlias({"experience_match_percentage", "experienceMatch", "experience_match"})
    private Double experienceMatchPercentage;

    @JsonAlias({"rank_position", "rank", "position"})
    private Integer rankPosition;

    @JsonAlias({"matched_skills", "matchingSkills", "matching_skills"})
    private List<String> matchedSkills;

    @JsonAlias({"missing_skills", "missingSkills"})
    private List<String> missingSkills;

    @JsonAlias({"match_reasoning", "reasoning", "matchReason"})
    private String matchReasoning;

    @JsonAlias({"fit_analysis", "fitAnalysis", "analysis"})
    private String fitAnalysis;
}


