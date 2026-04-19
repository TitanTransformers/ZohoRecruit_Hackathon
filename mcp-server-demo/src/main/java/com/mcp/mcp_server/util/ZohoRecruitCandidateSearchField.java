package com.mcp.mcp_server.util;

import lombok.Getter;

/**
 * Zoho Recruit Candidate Fields Enum - Exhaustive API Reference
 * Defines ALL available candidate fields from Zoho Recruit API with their data types.
 * This enum provides type-safe field references for building search criteria expressions.
 * Supported Field Types:
 * - STRING: Text fields (use operators: equals, not_equal, contains, starts_with)
 * - NUMERIC: Numeric fields (use operators: greater_than, greater_equal, less_than, less_equal)
 * - BOOLEAN: Boolean fields (use operators: equals)
 * - COMPLEX: Complex/nested objects (limited filtering support)
 */
@Getter
public enum ZohoRecruitCandidateSearchField {

    // ─────────────────────────────────────────────────────────────────────────
    // Identifier Fields
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Internal Zoho ID
     * Example: "486812000001660001"
     */
    ID("id", FieldType.STRING, "Internal System ID"),

    /**
     * Human-readable Candidate ID
     * Example: "ZR_001_CAND"
     */
    CANDIDATE_ID("Candidate_ID", FieldType.STRING, "Human-readable Candidate ID"),

    // ─────────────────────────────────────────────────────────────────────────
    // Name Fields
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Full Name
     * Example: "Ananya Sharma"
     */
    FULL_NAME("Full_Name", FieldType.STRING, "Full Name"),

    /**
     * First Name
     * Example: "Ananya"
     */
    FIRST_NAME("First_Name", FieldType.STRING, "First Name"),

    /**
     * Last Name
     * Example: "Sharma"
     */
    LAST_NAME("Last_Name", FieldType.STRING, "Last Name"),

    // ─────────────────────────────────────────────────────────────────────────
    // Contact Fields
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Email Address
     * Example: "ananya.sharma@example.com"
     */
    EMAIL("Email", FieldType.STRING, "Email Address"),

    /**
     * Mobile Number (PRIMARY contact)
     * Example: "9876543210"
     * Note: Use this field for contact - preferred over Phone
     */
    MOBILE("Mobile", FieldType.STRING, "Mobile Number (Primary Contact)"),

    /**
     * Phone Number (SECONDARY contact)
     * Example: null or landline
     * Note: Often null - use Mobile as fallback
     */
    PHONE("Phone", FieldType.STRING, "Phone Number (Secondary)"),

    /**
     * Email Opt Out Flag
     * Example: false
     * Indicates if candidate opted out of email communication
     */
    EMAIL_OPT_OUT("Email_Opt_Out", FieldType.BOOLEAN, "Email Opt Out"),

    // ─────────────────────────────────────────────────────────────────────────
    // Skills & Experience Fields
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Skill Set (comma-separated string)
     * Example: "Java, Spring Boot, Hibernate, MySQL, Maven"
     * Operators: contains, equals (for exact match), starts_with
     */
    SKILL_SET("Skill_Set", FieldType.STRING, "Comma-separated Skills"),

    /**
     * Years of Experience (numeric)
     * Example: 5
     * Operators: greater_equal, greater_than, less_equal, less_than
     */
    EXPERIENCE_IN_YEARS("Experience_in_Years", FieldType.NUMERIC, "Years of Experience"),

    /**
     * Experience Details (complex nested array)
     * Contains detailed experience records
     * Example: [{company, designation, duration, ...}]
     */
    EXPERIENCE_DETAILS("Experience_Details", FieldType.COMPLEX, "Experience Details Array"),

    // ─────────────────────────────────────────────────────────────────────────
    // Job Profile Fields
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Current Job Title
     * Example: "Senior Software Engineer"
     */
    CURRENT_JOB_TITLE("Current_Job_Title", FieldType.STRING, "Current Job Title"),

    /**
     * Current Employer / Company
     * Example: "TCS"
     */
    CURRENT_EMPLOYER("Current_Employer", FieldType.STRING, "Current Company/Employer"),

    /**
     * Expected Job Title or Designation
     * Example: "Lead Engineer"
     */
    DESIGNATION("Designation", FieldType.STRING, "Desired Designation"),

    // ─────────────────────────────────────────────────────────────────────────
    // Location Fields
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * City
     * Example: "Bengaluru"
     */
    CITY("City", FieldType.STRING, "City"),

    /**
     * State/Province
     * Example: "Karnataka"
     */
    STATE("State", FieldType.STRING, "State/Province"),

    /**
     * Country
     * Example: "India"
     */
    COUNTRY("Country", FieldType.STRING, "Country"),

    // ─────────────────────────────────────────────────────────────────────────
    // Education Fields
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Highest Qualification Held
     * Example: "B.Tech", "M.Tech", "MBA", "B.Com"
     */
    HIGHEST_QUALIFICATION_HELD("Highest_Qualification_Held", FieldType.STRING, "Highest Qualification"),

    /**
     * Educational Details (complex nested array)
     * Contains detailed education records
     * Example: [{institution, qualification, score, ...}]
     */
    EDUCATIONAL_DETAILS("Educational_Details", FieldType.COMPLEX, "Educational Details Array"),

    // ─────────────────────────────────────────────────────────────────────────
    // Salary Fields (Numeric)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Current Salary (numeric)
     * Example: 1400000
     * Operators: greater_equal, greater_than, less_equal, less_than
     */
    CURRENT_SALARY("Current_Salary", FieldType.NUMERIC, "Current Salary"),

    /**
     * Expected Salary (numeric)
     * Example: 1800000
     * Operators: greater_equal, greater_than, less_equal, less_than
     */
    EXPECTED_SALARY("Expected_Salary", FieldType.NUMERIC, "Expected Salary"),

    /**
     * Salary (numeric) - Generic salary field
     * Example: 0
     * Operators: greater_equal, greater_than, less_equal, less_than
     */
    SALARY("Salary", FieldType.NUMERIC, "Salary"),

    // ─────────────────────────────────────────────────────────────────────────
    // Status & Classification Fields
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Candidate Status
     * Examples: "New", "In-Review", "Shortlisted", "Rejected"
     * Operators: equals, not_equal, contains
     */
    CANDIDATE_STATUS("Candidate_Status", FieldType.STRING, "Candidate Status"),

    /**
     * Is Unqualified Flag (boolean)
     * Example: false
     */
    IS_UNQUALIFIED("Is_Unqualified", FieldType.BOOLEAN, "Is Unqualified"),

    /**
     * Approved Flag (boolean)
     * Example: true
     * Zoho field name: $approved
     */
    APPROVED("$approved", FieldType.BOOLEAN, "Is Approved"),

    /**
     * Is Locked Flag (boolean)
     * Indicates if candidate record is locked
     * Example: false
     */
    IS_LOCKED("Is_Locked", FieldType.BOOLEAN, "Is Locked"),

    // ─────────────────────────────────────────────────────────────────────────
    // Source & Tracking Fields
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Source
     * Example: "LinkedIn"
     * Where candidate was sourced from
     */
    SOURCE("Source", FieldType.STRING, "Candidate Source"),

    /**
     * Associated Tags (complex array)
     * Tags associated with the candidate
     * Example: [{name, id, ...}]
     */
    ASSOCIATED_TAGS("Associated_Tags", FieldType.COMPLEX, "Associated Tags"),

    /**
     * Rating (complex object - nullable)
     * Rating given to candidate
     * Example: {value, ratedBy, ...}
     */
    RATING("Rating", FieldType.COMPLEX, "Candidate Rating"),

    /**
     * Lead Owner (complex object)
     * Who owns/manages this candidate's record
     * Example: {name: "John Doe", id: "123456"}
     */
    LEAD_OWNER("Lead_Owner", FieldType.COMPLEX, "Lead Owner"),

    // ─────────────────────────────────────────────────────────────────────────
    // Document & Attachment Fields
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Is Attachment Present (boolean)
     * Indicates if candidate has attachments
     * Example: true
     */
    IS_ATTACHMENT_PRESENT("Is_Attachment_Present", FieldType.BOOLEAN, "Is Attachment Present");

    // ─────────────────────────────────────────────────────────────────────────
    // Enum Implementation
    // ─────────────────────────────────────────────────────────────────────────

    private final String fieldName;
    private final FieldType fieldType;
    private final String description;

    ZohoRecruitCandidateSearchField(String fieldName, FieldType fieldType, String description) {
        this.fieldName = fieldName;
        this.fieldType = fieldType;
        this.description = description;
    }

    /**
     * Check if this field is numeric (requires numeric operators)
     */
    public boolean isNumeric() {
        return fieldType == FieldType.NUMERIC;
    }

    /**
     * Check if this field is string type (uses string operators)
     */
    public boolean isString() {
        return fieldType == FieldType.STRING;
    }

    /**
     * Check if this field is boolean type
     */
    public boolean isBoolean() {
        return fieldType == FieldType.BOOLEAN;
    }

    /**
     * Check if this field is complex type (nested objects/arrays)
     */
    public boolean isComplex() {
        return fieldType == FieldType.COMPLEX;
    }

    /**
     * Get recommended operator for this field type
     */
    public String getRecommendedOperator() {
        return switch (fieldType) {
            case STRING -> "contains";
            case NUMERIC -> "greater_equal";
            case BOOLEAN -> "equals";
            case COMPLEX -> "N/A";
        };
    }

    @Override
    public String toString() {
        return fieldName;
    }

    /**
     * Field Data Type Enum
     * Determines which operators are valid for the field
     */
    @Getter
    public enum FieldType {
        /**
         * String fields use: equals, not_equal, contains, starts_with
         * Examples: Email, City, Skill_Set, Current_Job_Title
         */
        STRING("String", "Text comparison operators: equals, not_equal, contains, starts_with"),

        /**
         * Numeric fields use: greater_than, greater_equal, less_than, less_equal
         * Examples: Experience_in_Years, Current_Salary, Expected_Salary, Salary
         */
        NUMERIC("Numeric", "Numeric comparison operators: greater_than, greater_equal, less_than, less_equal"),

        /**
         * Boolean fields use: equals (true/false)
         * Examples: Is_Unqualified, $approved, Is_Locked, Email_Opt_Out, Is_Attachment_Present
         */
        BOOLEAN("Boolean", "Boolean operators: equals (true/false)"),

        /**
         * Complex fields are nested objects/arrays
         * Limited filtering support
         * Examples: Experience_Details, Educational_Details, Associated_Tags, Rating, Lead_Owner
         */
        COMPLEX("Complex", "Complex nested objects/arrays - limited filtering support");

        private final String typeName;
        private final String description;

        FieldType(String typeName, String description) {
            this.typeName = typeName;
            this.description = description;
        }

        /**
         * Check if this type supports criteria filtering
         */
        public boolean supportsCriteria() {
            return this != COMPLEX;
        }
    }
}


