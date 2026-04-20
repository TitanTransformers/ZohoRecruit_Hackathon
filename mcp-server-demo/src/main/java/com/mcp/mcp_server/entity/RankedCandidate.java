package com.mcp.mcp_server.entity;

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
public class RankedCandidate {
    private String candidateId;
    private String name;
    private String email;
    private String mobile;
    private Double matchPercentage;              // Overall match percentage (0-100)
    private Double skillMatchPercentage;         // Skill-specific match percentage (0-100)
    private Double experienceMatchPercentage;    // Experience-specific match percentage (0-100)
    private Integer rankPosition;                // Position in ranked list (1-based)
    private List<String> matchedSkills;
    private List<String> missingSkills;
    private String matchReasoning;
    private String fitAnalysis;
}

