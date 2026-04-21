package com.mcp.mcp_server.service;

import com.mcp.mcp_server.entity.Candidate;
import com.mcp.mcp_server.util.ZohoRecruitCandidateSearchField;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Service for integrating with ZohoRecruit API to search for candidate profiles.
 * This is a wrapper/facade around the new Zoho Recruit OAuth and API services
 * that maintains backward compatibility with existing code.
 * Uses ZohoCriteriaBuilder for building valid Zoho Recruit filter criteria.
 *
 * VALIDATION RULES:
 * - Only fields from ZohoRecruitCandidateSearchField enum are used
 * - Operators are automatically selected based on field type
 * - Numeric fields use numeric operators (greater_equal, less_equal)
 * - String fields use string operators (contains, equals)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ZohoRecruitService {

    private final ZohoRecruitAPIService zohoRecruitAPIService;

    /**
     * Search for candidates in Zoho Recruit using criteria
     *
     * @param searchCriteria Map of search criteria (e.g., skills, experience, location)
     * @param pageSize       Number of results per page
     * @return List of candidate profiles
     */
    public List<Candidate> searchCandidates(Map<String, String> searchCriteria, int pageSize) {
        try {
            log.info("Searching candidates with criteria: {}", searchCriteria);

            // Build criteria string using ZohoCriteriaBuilder with field validation
            String criteria = buildCriteriaStringUsingBuilder(searchCriteria);
            log.debug("Built Zoho criteria: {}", criteria);

            // Call the new API service
            return zohoRecruitAPIService.searchCandidates(criteria, 1, pageSize);
        } catch (Exception e) {
            log.error("Error searching candidates in Zoho Recruit", e);
            return new ArrayList<>();
        }
    }


    /**
     * Build Zoho Recruit search criteria string from map using ZohoCriteriaBuilder
     * Converts map like {"skills": "Java,Python", "experience_years": "5"}
     * to valid Zoho criteria string using proper field names and operators
     *
     * VALIDATION ENSURES:
     * - Only valid field names from ZohoRecruitCandidateSearchField are used
     * - Correct operators for each field type
     * - Numeric fields don't use string operators
     *
     * @param searchCriteria Map with keys: skills, experience_years, location, status, etc.
     * @return Valid Zoho Recruit criteria string
     */
    private String buildCriteriaStringUsingBuilder(Map<String, String> searchCriteria) {
        if (searchCriteria == null || searchCriteria.isEmpty()) {
            return "";
        }

        ZohoCriteriaBuilder.CriteriaFilter filter = new ZohoCriteriaBuilder.CriteriaFilter();

        try {
            // Process each search criterion
            for (Map.Entry<String, String> entry : searchCriteria.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();

                if (value == null || value.isEmpty()) {
                    continue;
                }

                switch (key.toLowerCase()) {
                    case "skills" -> {
                        // Handle multiple skills (comma-separated)
                        String[] skills = value.split(",");
                        for (String skill : skills) {
                            filter.addSkill(skill.trim());
                        }
                    }
                    case "skill" -> filter.addSkill(value);

                    case "experience_years", "experience_in_years", "years_of_experience" -> {
                        try {
                            filter.addExperience(Integer.parseInt(value));
                        } catch (NumberFormatException e) {
                            log.warn("Invalid experience value (must be numeric): {}. Skipping.", value);
                        }
                    }

                    case "location", "city" -> filter.addLocation(value);

                    case "status", "candidate_status" -> filter.addStatus(value);

                    case "current_salary" -> {
                        try {
                            Integer salary = Integer.parseInt(value);
                            filter.addCondition(ZohoRecruitCandidateSearchField.CURRENT_SALARY,
                                    ZohoCriteriaBuilder.Operator.GREATER_EQUAL, salary);
                        } catch (NumberFormatException e) {
                            log.warn("Invalid salary value (must be numeric): {}. Skipping.", value);
                        }
                    }

                    case "expected_salary" -> {
                        try {
                            Integer salary = Integer.parseInt(value);
                            filter.addCondition(ZohoRecruitCandidateSearchField.EXPECTED_SALARY,
                                    ZohoCriteriaBuilder.Operator.GREATER_EQUAL, salary);
                        } catch (NumberFormatException e) {
                            log.warn("Invalid salary value (must be numeric): {}. Skipping.", value);
                        }
                    }

                    case "designation", "job_title" ->
                        filter.addCondition(ZohoRecruitCandidateSearchField.DESIGNATION,
                                ZohoCriteriaBuilder.Operator.CONTAINS, value);

                    case "company", "current_employer" ->
                        filter.addCondition(ZohoRecruitCandidateSearchField.CURRENT_EMPLOYER,
                                ZohoCriteriaBuilder.Operator.CONTAINS, value);

                    case "email" ->
                        filter.addCondition(ZohoRecruitCandidateSearchField.EMAIL,
                                ZohoCriteriaBuilder.Operator.EQUALS, value);

                    case "phone", "mobile" ->
                        filter.addCondition(ZohoRecruitCandidateSearchField.MOBILE,
                                ZohoCriteriaBuilder.Operator.EQUALS, value);

                    // Additional fields
                    case "qualification" ->
                        filter.addCondition(ZohoRecruitCandidateSearchField.HIGHEST_QUALIFICATION_HELD,
                                ZohoCriteriaBuilder.Operator.EQUALS, value);

                    case "state" ->
                        filter.addCondition(ZohoRecruitCandidateSearchField.STATE,
                                ZohoCriteriaBuilder.Operator.EQUALS, value);

                    case "country" ->
                        filter.addCondition(ZohoRecruitCandidateSearchField.COUNTRY,
                                ZohoCriteriaBuilder.Operator.EQUALS, value);

                    default -> {
                        log.debug("Unknown search criterion key: {}. Attempting to map...", key);
                        // Try to find matching field in enum by name normalization
                        attemptToAddCondition(filter, key, value);
                    }
                }
            }

            return filter.build();
        } catch (Exception e) {
            log.error("Error building criteria with ZohoCriteriaBuilder", e);
            throw new IllegalArgumentException("Invalid search criteria: " + e.getMessage(), e);
        }
    }

    /**
     * Attempt to add a condition for unmapped keys by finding matching field in enum
     */
    private void attemptToAddCondition(ZohoCriteriaBuilder.CriteriaFilter filter, String key, String value) {
        try {
            // Try exact match
            for (ZohoRecruitCandidateSearchField field : ZohoRecruitCandidateSearchField.values()) {
                if (field.getFieldName().equalsIgnoreCase(key)) {
                    // Determine operator based on field type
                    ZohoCriteriaBuilder.Operator op = field.isNumeric() ?
                            ZohoCriteriaBuilder.Operator.GREATER_EQUAL :
                            ZohoCriteriaBuilder.Operator.CONTAINS;

                    if (field.isNumeric()) {
                        try {
                            filter.addCondition(field, op, Integer.parseInt(value));
                        } catch (NumberFormatException e) {
                            log.warn("Cannot parse numeric value for field {}: {}", field.getFieldName(), value);
                        }
                    } else {
                        filter.addCondition(field, op, value);
                    }
                    return;
                }
            }
            log.warn("No matching field found for criterion: {}. Ignoring.", key);
        } catch (Exception e) {
            log.warn("Error attempting to add condition for key {}: {}", key, e.getMessage());
        }
    }


}


