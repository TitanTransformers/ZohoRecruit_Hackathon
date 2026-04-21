package com.mcp.mcp_server.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Represents a candidate profile from Zoho Recruit API
 * Maps to all fields from Zoho Recruit's Candidate object via @JsonProperty annotations
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class Candidate {
    // ─────────────────────────────────────────────────────────────────────────
    // Identifier Fields
    // ─────────────────────────────────────────────────────────────────────────
    @JsonProperty("id")
    private String id;

    @JsonProperty("Candidate_ID")
    private String candidateId;

    // ─────────────────────────────────────────────────────────────────────────
    // Name Fields
    // ─────────────────────────────────────────────────────────────────────────
    @JsonProperty("First_Name")
    private String firstName;

    @JsonProperty("Last_Name")
    private String lastName;

    @JsonProperty("Full_Name")
    private String name;

    // ─────────────────────────────────────────────────────────────────────────
    // Contact Fields
    // ─────────────────────────────────────────────────────────────────────────
    @JsonProperty("Email")
    private String email;

    @JsonProperty("Phone")
    private String phone;

    @JsonProperty("Mobile")
    private String mobile;

    @JsonProperty("Email_Opt_Out")
    private Boolean emailOptOut;

    // ─────────────────────────────────────────────────────────────────────────
    // Job Profile Fields
    // ─────────────────────────────────────────────────────────────────────────
    @JsonProperty("Current_Employer")
    private String currentCompany;

    @JsonProperty("Current_Job_Title")
    private String currentDesignation;

    @JsonProperty("Designation")
    private String designation;

    // ─────────────────────────────────────────────────────────────────────────
    // Location Fields
    // ─────────────────────────────────────────────────────────────────────────
    @JsonProperty("City")
    private String city;

    @JsonProperty("State")
    private String state;

    @JsonProperty("Country")
    private String country;

    // ─────────────────────────────────────────────────────────────────────────
    // Skills & Experience Fields
    // ─────────────────────────────────────────────────────────────────────────
    private List<String> skills;

    @JsonProperty("Experience_in_Years")
    private Integer yearsOfExperience;

    @JsonProperty("experience")
    private String experience;

    @JsonProperty("Experience_Details")
    private List<Map<String, Object>> experienceDetails;

    // ─────────────────────────────────────────────────────────────────────────
    // Education Fields
    // ─────────────────────────────────────────────────────────────────────────
    @JsonProperty("Highest_Qualification_Held")
    private String highestQualification;

    @JsonProperty("Educational_Details")
    private List<Map<String, Object>> educationalDetails;

    // ─────────────────────────────────────────────────────────────────────────
    // Salary Fields
    // ─────────────────────────────────────────────────────────────────────────
    @JsonProperty("Current_Salary")
    private Long currentSalary;

    @JsonProperty("Expected_Salary")
    private Long expectedSalary;

    @JsonProperty("Salary")
    private Long salary;

    // ─────────────────────────────────────────────────────────────────────────
    // Document Fields
    // ─────────────────────────────────────────────────────────────────────────
    @JsonProperty("resume_url")
    private String resumeUrl;

    @JsonProperty("Is_Attachment_Present")
    private Boolean isAttachmentPresent;

    // ─────────────────────────────────────────────────────────────────────────
    // Status & Classification Fields
    // ─────────────────────────────────────────────────────────────────────────
    @JsonProperty("Candidate_Status")
    private String status;

    @JsonProperty("Is_Unqualified")
    private Boolean isUnqualified;

    @JsonProperty("$approved")
    private Boolean approved;

    @JsonProperty("Is_Locked")
    private Boolean isLocked;

    // ─────────────────────────────────────────────────────────────────────────
    // Source & Tracking Fields
    // ─────────────────────────────────────────────────────────────────────────
    @JsonProperty("Source")
    private String source;

    @JsonProperty("Associated_Tags")
    private List<Map<String, Object>> associatedTags;

    @JsonProperty("Rating")
    private Map<String, Object> rating;

    // ─────────────────────────────────────────────────────────────────────────
    // Timestamps
    // ─────────────────────────────────────────────────────────────────────────
    @JsonProperty("Created_Time")
    private String createdTime;

    @JsonProperty("Updated_On")
    private String updatedOn;

    // ─────────────────────────────────────────────────────────────────────────
    // User References
    // ─────────────────────────────────────────────────────────────────────────
    @JsonProperty("Created_By")
    private Map<String, Object> createdBy;

    @JsonProperty("Lead_Owner")
    private Map<String, Object> leadOwner;

    /**
     * Custom setter for Skill_Set — populates the skills list.
     */
    @JsonSetter("Skill_Set")
    public void setSkillSet(String skillSet) {
        if (skillSet != null && !skillSet.isEmpty()) {
            this.skills = List.of(skillSet.split(",\\s*"));
        } else {
            this.skills = new ArrayList<>();
        }
    }

    /**
     * Post-deserialization hook to derive computed fields.
     * Called after Jackson populates all fields.
     */
    @JsonProperty
    private void deriveComputedFields() {
        // Derive full name if not provided
        if (this.name == null) {
            this.name = firstName != null && lastName != null ? firstName + " " + lastName :
                        firstName != null ? firstName :
                        lastName;
        }
    }

    /**
     * Compact JSON representation for AI prompt consumption.
     */
    @Override
    public String toString() {
        return "{\"id\":\"" + esc(candidateId)
                + "\",\"title\":\"" + esc(currentDesignation)
                + "\",\"skills\":\"" + esc(skills != null ? String.join(", ", skills) : "")
                + "\",\"yoe\":" + (yearsOfExperience != null ? yearsOfExperience : "null")
                + "}";
    }

    private static String esc(String v) {
        if (v == null) return "";
        return v.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
