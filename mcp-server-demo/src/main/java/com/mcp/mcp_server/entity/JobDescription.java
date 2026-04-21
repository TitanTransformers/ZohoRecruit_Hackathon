package com.mcp.mcp_server.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Represents a parsed Job Description with extracted metadata
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobDescription {
    private String jobTitle;
    private String jobDescription;
    private List<String> requiredSkills;
    private List<String> preferredSkills;
    private String experienceLevel;
    private Integer minYearsOfExperience;
    private Integer maxYearsOfExperience;
    private List<String> qualifications;
    private List<String> responsibilities;
    private String department;
    private String location;
    private Map<String, Object> additionalMetadata;
}

