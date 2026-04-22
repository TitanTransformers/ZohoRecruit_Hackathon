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
     * IMPORTANT LOGIC:
     * - Skills, Designations, Locations → joined with OR (find candidates matching ANY)
     * - Experience range (min AND max) → joined with AND (must satisfy BOTH)
     * - Final: ((Skills OR Designations OR ...) OR ...) AND (MinExp AND MaxExp)
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

        ZohoCriteriaBuilder.CriteriaFilter orFilter = new ZohoCriteriaBuilder.CriteriaFilter();      // OR grouped conditions
        ZohoCriteriaBuilder.CriteriaFilter andExpFilter = new ZohoCriteriaBuilder.CriteriaFilter(); // AND for experience range
        Integer minExperience = null;
        Integer maxExperience = null;

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
                        // Handle multiple skills (comma-separated) - each added to OR filter
                        String[] skills = value.split(",");
                        for (String skill : skills) {
                            orFilter.addSkill(skill.trim());
                        }
                    }
                    case "skill" -> orFilter.addSkill(value);

                    case "experience_years", "experience_in_years", "years_of_experience" -> {
                        // Default experience (both min and max if single value)
                        try {
                            int years = Integer.parseInt(value);
                            minExperience = years;
                            maxExperience = years;
                        } catch (NumberFormatException e) {
                            log.warn("Invalid experience value (must be numeric): {}. Skipping.", value);
                        }
                    }

                    case "min_experience_years" -> {
                        try {
                            minExperience = Integer.parseInt(value);
                        } catch (NumberFormatException e) {
                            log.warn("Invalid min experience value: {}. Skipping.", value);
                        }
                    }

                    case "max_experience_years" -> {
                        try {
                            maxExperience = Integer.parseInt(value);
                        } catch (NumberFormatException e) {
                            log.warn("Invalid max experience value: {}. Skipping.", value);
                        }
                    }

                    case "location", "city" -> orFilter.addLocation(value);

                    case "status", "candidate_status" -> orFilter.addStatus(value);

                    case "current_salary" -> {
                        try {
                            Integer salary = Integer.parseInt(value);
                            orFilter.addCondition(ZohoRecruitCandidateSearchField.CURRENT_SALARY,
                                    ZohoCriteriaBuilder.Operator.GREATER_EQUAL, salary);
                        } catch (NumberFormatException e) {
                            log.warn("Invalid salary value (must be numeric): {}. Skipping.", value);
                        }
                    }

                    case "expected_salary" -> {
                        try {
                            Integer salary = Integer.parseInt(value);
                            orFilter.addCondition(ZohoRecruitCandidateSearchField.EXPECTED_SALARY,
                                    ZohoCriteriaBuilder.Operator.GREATER_EQUAL, salary);
                        } catch (NumberFormatException e) {
                            log.warn("Invalid salary value (must be numeric): {}. Skipping.", value);
                        }
                    }

                    case "designation", "job_title" ->
                        orFilter.addCondition(ZohoRecruitCandidateSearchField.DESIGNATION,
                                ZohoCriteriaBuilder.Operator.CONTAINS, value);

                    case "company", "current_employer" ->
                        orFilter.addCondition(ZohoRecruitCandidateSearchField.CURRENT_EMPLOYER,
                                ZohoCriteriaBuilder.Operator.CONTAINS, value);

                    case "email" ->
                        orFilter.addCondition(ZohoRecruitCandidateSearchField.EMAIL,
                                ZohoCriteriaBuilder.Operator.EQUALS, value);

                    case "phone", "mobile" ->
                        orFilter.addCondition(ZohoRecruitCandidateSearchField.MOBILE,
                                ZohoCriteriaBuilder.Operator.EQUALS, value);

                    // Additional fields
                    case "qualification" ->
                        orFilter.addCondition(ZohoRecruitCandidateSearchField.HIGHEST_QUALIFICATION_HELD,
                                ZohoCriteriaBuilder.Operator.EQUALS, value);

                    case "state" ->
                        orFilter.addCondition(ZohoRecruitCandidateSearchField.STATE,
                                ZohoCriteriaBuilder.Operator.EQUALS, value);

                    case "country" ->
                        orFilter.addCondition(ZohoRecruitCandidateSearchField.COUNTRY,
                                ZohoCriteriaBuilder.Operator.EQUALS, value);

                    default -> {
                        log.debug("Unknown search criterion key: {}. Attempting to map...", key);
                        // Try to find matching field in enum by name normalization
                        attemptToAddCondition(orFilter, key, value);
                    }
                }
            }

            // Build final criteria with proper operator precedence
            // OR conditions for skills/designations/locations
            // AND conditions for experience range only
            return buildCriteriaWithPrecedence(orFilter, minExperience, maxExperience, andExpFilter);
        } catch (Exception e) {
            log.error("Error building criteria with ZohoCriteriaBuilder", e);
            throw new IllegalArgumentException("Invalid search criteria: " + e.getMessage(), e);
        }
    }

    /**
     * Build criteria with proper operator precedence:
     * - OR for skills, designations, locations, etc. (all grouped together)
     * - AND exclusively for experience range (min and max must both be satisfied)
     * - Final structure ensures correct precedence:
     *   * If no experience range: ((skill1)or(skill2)or(designation)or(location))
     *   * If experience range: (((skill1)or(skill2)or(designation)or(location)))and(((min_exp>=5)and(max_exp<=10)))
     */
    private String buildCriteriaWithPrecedence(ZohoCriteriaBuilder.CriteriaFilter orFilter,
                                                Integer minExperience, Integer maxExperience,
                                                ZohoCriteriaBuilder.CriteriaFilter andExpFilter) {
        List<String> finalConditions = new ArrayList<>();

        // Build OR conditions (skills, designations, locations, etc.)
        if (!orFilter.isEmpty()) {
            String orCriteria = orFilter.build();  // This produces: ((cond1))or((cond2))or((cond3))
            log.debug("OR criteria (before wrapping): {}", orCriteria);
            // Wrap the entire OR group in parentheses to ensure precedence over AND
            finalConditions.add("(" + orCriteria + ")");
        }

        // Build AND conditions for experience range
        if (minExperience != null && maxExperience != null) {
            log.debug("Adding experience range: min={}, max={}", minExperience, maxExperience);
            String minExpCond = ZohoCriteriaBuilder.buildCondition(
                    ZohoRecruitCandidateSearchField.EXPERIENCE_IN_YEARS,
                    ZohoCriteriaBuilder.Operator.GREATER_EQUAL, minExperience);
            String maxExpCond = ZohoCriteriaBuilder.buildCondition(
                    ZohoRecruitCandidateSearchField.EXPERIENCE_IN_YEARS,
                    ZohoCriteriaBuilder.Operator.LESS_EQUAL, maxExperience);
            // Experience range uses AND - wrap in parentheses
            finalConditions.add("(" + minExpCond + "and" + maxExpCond + ")");
        } else if (minExperience != null) {
            log.debug("Adding minimum experience: {}", minExperience);
            String minExpCond = ZohoCriteriaBuilder.buildCondition(
                    ZohoRecruitCandidateSearchField.EXPERIENCE_IN_YEARS,
                    ZohoCriteriaBuilder.Operator.GREATER_EQUAL, minExperience);
            finalConditions.add(minExpCond);
        } else if (maxExperience != null) {
            log.debug("Adding maximum experience: {}", maxExperience);
            String maxExpCond = ZohoCriteriaBuilder.buildCondition(
                    ZohoRecruitCandidateSearchField.EXPERIENCE_IN_YEARS,
                    ZohoCriteriaBuilder.Operator.LESS_EQUAL, maxExperience);
            finalConditions.add(maxExpCond);
        }

        // Combine final conditions with AND
        if (finalConditions.isEmpty()) {
            throw new IllegalArgumentException("No valid search criteria provided");
        }

        String result;
        if (finalConditions.size() == 1) {
            // Single condition - no need for joining
            result = finalConditions.get(0);
        } else {
            // Multiple conditions (OR group AND experience range) - join with AND
            // Result format: (((OR_GROUP)))and(((EXP_MIN_AND_MAX)))
            result = String.join("and", finalConditions);
        }

        log.info("Final search criteria: {}", result);
        return result;
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


