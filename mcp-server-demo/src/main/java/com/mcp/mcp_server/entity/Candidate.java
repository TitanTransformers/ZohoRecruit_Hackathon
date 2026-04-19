package com.mcp.mcp_server.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Represents a candidate profile from Zoho Recruit API
 * Maps to all fields from Zoho Recruit's Candidate object
 *
 * Field Mapping Reference:
 * - id: Internal Zoho ID (486812000001660001)
 * - Candidate_ID: Human-readable ID (ZR_001_CAND)
 * - Full_Name, First_Name, Last_Name: Name fields
 * - Email, Mobile, Phone: Contact fields
 * - Skill_Set: Comma-separated skills
 * - Experience_in_Years: Years of experience
 * - Current_Employer, Current_Job_Title, Designation: Job info
 * - City, State, Country: Location fields
 * - Current_Salary, Expected_Salary: Salary information
 * - Candidate_Status: Status (New, Shortlisted, Rejected, etc.)
 * - Created_Time, Updated_On: Timestamps
 * - $approved: Approval flag
 * - Is_Locked, Is_Unqualified: Status flags
 * - Educational_Details, Experience_Details: Complex arrays
 * - Associated_Tags: Tags array
 * - Source: Candidate source (LinkedIn, Indeed, etc.)
 * - Lead_Owner, Created_By: User references
 * - Rating, Is_Attachment_Present: Additional flags
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Candidate {
    // ─────────────────────────────────────────────────────────────────────────
    // Identifier Fields
    // ─────────────────────────────────────────────────────────────────────────
    private String id;                      // Zoho internal ID (from "id" field)
    private String candidateId;             // Human-readable ID (from "Candidate_ID" field)

    // ─────────────────────────────────────────────────────────────────────────
    // Name Fields
    // ─────────────────────────────────────────────────────────────────────────
    private String firstName;               // From "First_Name"
    private String lastName;                // From "Last_Name"
    private String name;                    // Full name from "Full_Name"

    // ─────────────────────────────────────────────────────────────────────────
    // Contact Fields
    // ─────────────────────────────────────────────────────────────────────────
    private String email;                   // From "Email"
    private String phone;                   // From "Phone" (may be null)
    private String mobile;                  // From "Mobile" (primary contact)
    private Boolean emailOptOut;            // From "Email_Opt_Out" flag

    // ─────────────────────────────────────────────────────────────────────────
    // Job Profile Fields
    // ─────────────────────────────────────────────────────────────────────────
    private String currentCompany;          // From "Current_Employer"
    private String currentDesignation;      // From "Current_Job_Title"
    private String currentPosition;         // Alternative/mapped from "Designation"
    private String designation;             // From "Designation" (desired role)

    // ─────────────────────────────────────────────────────────────────────────
    // Location Fields
    // ─────────────────────────────────────────────────────────────────────────
    private String location;                // Mapped from "City" or full location
    private String city;                    // From "City"
    private String state;                   // From "State"
    private String country;                 // From "Country"

    // ─────────────────────────────────────────────────────────────────────────
    // Skills & Experience Fields
    // ─────────────────────────────────────────────────────────────────────────
    private String skillsString;            // From "Skill_Set" (comma-separated)
    private List<String> skills;            // Parsed from "Skill_Set"
    private Integer yearsOfExperience;      // From "Experience_in_Years"
    private String experience;              // Experience description
    private List<Map<String, Object>> experienceDetails;  // From "Experience_Details" array

    // ─────────────────────────────────────────────────────────────────────────
    // Education Fields
    // ─────────────────────────────────────────────────────────────────────────
    private String highestQualification;    // From "Highest_Qualification_Held"
    private List<String> qualifications;    // Qualifications as list
    private List<Map<String, Object>> educationalDetails;  // From "Educational_Details" array

    // ─────────────────────────────────────────────────────────────────────────
    // Salary Fields
    // ─────────────────────────────────────────────────────────────────────────
    private Long currentSalary;             // From "Current_Salary"
    private Long expectedSalary;            // From "Expected_Salary"
    private Long salary;                    // From "Salary" field

    // ─────────────────────────────────────────────────────────────────────────
    // Document Fields
    // ─────────────────────────────────────────────────────────────────────────
    private String resume;                  // Resume text content
    private String resumeUrl;               // Resume URL
    private Boolean isAttachmentPresent;    // From "Is_Attachment_Present" flag

    // ─────────────────────────────────────────────────────────────────────────
    // Status & Classification Fields
    // ─────────────────────────────────────────────────────────────────────────
    private String status;                  // From "Candidate_Status" (Shortlisted, New, Rejected, etc.)
    private Boolean isUnqualified;          // From "Is_Unqualified" flag
    private Boolean approved;               // From "$approved" flag
    private Boolean isLocked;               // From "Is_Locked" flag

    // ─────────────────────────────────────────────────────────────────────────
    // Source & Tracking Fields
    // ─────────────────────────────────────────────────────────────────────────
    private String source;                  // From "Source" (LinkedIn, Indeed, etc.)
    private List<Map<String, Object>> associatedTags;  // From "Associated_Tags" array
    private Map<String, Object> rating;     // From "Rating" object (may be null)

    // ─────────────────────────────────────────────────────────────────────────
    // Timestamps
    // ─────────────────────────────────────────────────────────────────────────
    private String createdTime;             // From "Created_Time" (ISO timestamp)
    private String updatedOn;               // From "Updated_On" (ISO timestamp)
    private LocalDateTime createdAt;        // Parsed timestamp

    // ─────────────────────────────────────────────────────────────────────────
    // User References
    // ─────────────────────────────────────────────────────────────────────────
    private Map<String, Object> createdBy;  // From "Created_By" object {id, name}
    private Map<String, Object> leadOwner;  // From "Lead_Owner" object {id, name}

    // ─────────────────────────────────────────────────────────────────────────
    // API Response Storage
    // ─────────────────────────────────────────────────────────────────────────
    private Object zohoProfile;             // Original Zoho API response
}

