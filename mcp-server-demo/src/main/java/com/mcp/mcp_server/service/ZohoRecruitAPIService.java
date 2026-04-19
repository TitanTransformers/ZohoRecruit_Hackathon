package com.mcp.mcp_server.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mcp.mcp_server.config.ZohoRecruitPortalConnectionConfig;
import com.mcp.mcp_server.entity.Candidate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Zoho Recruit API Service
 * Handles interactions with Zoho Recruit API for candidate searches and job operations
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ZohoRecruitAPIService {

    private final ZohoRecruitPortalConnectionConfig zohoRecruitPortalConnectionConfig;
    private final ZohoRecruitOAuthService oauthService;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Search for candidates based on criteria
     *
     * @param criteria Search criteria (e.g., "(Skill_Set:contains:Python)and(Experience_in_Years:greater_equal:5)")
     * @param page Page number (default 1)
     * @param pageSize Results per page (default 200)
     * @return List of candidates matching criteria
     */
    public List<Candidate> searchCandidates(String criteria, Integer page, Integer pageSize) {
        try {
            int pageNum = page != null ? page : zohoRecruitPortalConnectionConfig.getDefaultPage();
            int pageLen = pageSize != null ? pageSize : zohoRecruitPortalConnectionConfig.getPageSize();

            log.info("Searching candidates with criteria: {} (page: {}, size: {})", criteria, pageNum, pageLen);

            // Build API URL with search parameters
            String url = UriComponentsBuilder.fromUriString(zohoRecruitPortalConnectionConfig.getApiBaseUrl())
                    .path(zohoRecruitPortalConnectionConfig.getCandidatesEndpoint())
                    .queryParam("criteria", criteria)
                    .queryParam("page", pageNum)
                    .queryParam("per_page", pageLen)
                    .build()
                    .toUriString();

            log.debug("Zoho Recruit API URL: {}", url);

            // Make authenticated API request
            ResponseEntity<String> response = makeAuthenticatedRequest(url, HttpMethod.GET);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return parseCandidatesResponse(response.getBody());
            } else {
                log.error("API request failed with status: {}", response.getStatusCode());
                return new ArrayList<>();
            }
        } catch (Exception e) {
            log.error("Failed to search candidates", e);
            throw new RuntimeException("Failed to search candidates in Zoho Recruit", e);
        }
    }

    /**
     * Search for candidates by skill
     * Example: searchCandidatesBySkill("Python", 5) for Python developers with 5+ years experience
     *
     * @param skill Required skill
     * @param yearsOfExperience Minimum years of experience
     * @return List of matching candidates
     */
    public List<Candidate> searchCandidatesBySkill(String skill, Integer yearsOfExperience) {
        String criteria = buildSkillCriteria(skill, yearsOfExperience);
        return searchCandidates(criteria, 1, null);
    }


    /**
     * Build complex search criteria with multiple field conditions (ORed together)
     *
     * Format: ((field:operator:value))or((field:operator:value))or((field:operator:value))...
     *
     * Example usage:
     * <pre>
     *   Map<String, List<String>> fieldConditions = new LinkedHashMap<>();
     *   fieldConditions.put("Designation", List.of("Java Developer"));
     *   fieldConditions.put("Skill_Set", List.of("Java", "Object-Oriented Programming", "Data Structures"));
     *   fieldConditions.put("City", List.of("Bangalore"));
     *
     *   String criteria = buildMultiFieldCriteriaWithOR(fieldConditions, "contains");
     *   // For "Designation", use operator "contains"
     *   // For "City", use operator "equals"
     * </pre>
     *
     * Generated criteria format (same as Zoho Recruit API):
     * ((Designation:contains:Java Developer))or((Skill_Set:contains:Java))or((Skill_Set:contains:Object-Oriented Programming))or((Skill_Set:contains:Data Structures))or((City:equals:Bangalore))
     *
     * @param fieldConditions Map of field name to list of values (use LinkedHashMap to preserve order)
     * @param operator Operator type (contains, equals, greater_equal, etc.)
     * @return Search criteria string in exact Zoho Recruit API format
     */
    public String buildMultiFieldCriteriaWithOR(Map<String, List<String>> fieldConditions, String operator) {
        if (fieldConditions == null || fieldConditions.isEmpty()) {
            return "";
        }

        List<String> conditions = new ArrayList<>();

        for (Map.Entry<String, List<String>> entry : fieldConditions.entrySet()) {
            String fieldName = entry.getKey();
            List<String> values = entry.getValue();

            for (String value : values) {
                conditions.add(String.format("((%s:%s:%s))", fieldName, operator, value));
            }
        }

        return String.join("or", conditions);
    }

    /**
     * Build search criteria with multiple fields and custom operators per field
     *
     * This method allows different operators for different fields, useful when you need:
     * - "contains" for Designation and Skill_Set
     * - "equals" for City
     *
     * Format: ((field1:op1:value))or((field2:op2:value))or...
     *
     * Example usage to match your URL:
     * <pre>
     *   Map<String, Map<String, List<String>>> fieldOperatorValues = new LinkedHashMap<>();
     *
     *   Map<String, List<String>> designations = new LinkedHashMap<>();
     *   designations.put("contains", List.of("Java Developer"));
     *   fieldOperatorValues.put("Designation", designations);
     *
     *   Map<String, List<String>> skills = new LinkedHashMap<>();
     *   skills.put("contains", List.of("Java", "Object-Oriented Programming", "Data Structures"));
     *   fieldOperatorValues.put("Skill_Set", skills);
     *
     *   Map<String, List<String>> cities = new LinkedHashMap<>();
     *   cities.put("equals", List.of("Bangalore"));
     *   fieldOperatorValues.put("City", cities);
     *
     *   String criteria = buildMultiFieldCriteriaWithOperators(fieldOperatorValues);
     * </pre>
     *
     * @param fieldOperatorValues Map of field name to (operator to values list) map
     * @return Search criteria string in exact Zoho Recruit API format
     */
    public String buildMultiFieldCriteriaWithOperators(Map<String, Map<String, List<String>>> fieldOperatorValues) {
        if (fieldOperatorValues == null || fieldOperatorValues.isEmpty()) {
            return "";
        }

        List<String> conditions = new ArrayList<>();

        for (Map.Entry<String, Map<String, List<String>>> fieldEntry : fieldOperatorValues.entrySet()) {
            String fieldName = fieldEntry.getKey();
            Map<String, List<String>> operatorValues = fieldEntry.getValue();

            for (Map.Entry<String, List<String>> opEntry : operatorValues.entrySet()) {
                String operator = opEntry.getKey();
                List<String> values = opEntry.getValue();

                for (String value : values) {
                    conditions.add(String.format("((%s:%s:%s))", fieldName, operator, value));
                }
            }
        }

        return String.join("or", conditions);
    }

    /**
     * Build search criteria for skill-based search
     */
    private String buildSkillCriteria(String skill, Integer yearsOfExperience) {
        if (yearsOfExperience != null && yearsOfExperience > 0) {
            return String.format("((Skill_Set:contains:%s)and(Experience_in_Years:greater_equal:%d))", skill, yearsOfExperience);
        } else {
            return String.format("(Skill_Set:contains:%s)", skill);
        }
    }

    /**
     * Fetch detailed candidate information
     *
     * @param candidateId Zoho Recruit candidate ID
     * @return Candidate details
     */
    public Candidate getCandidateDetails(String candidateId) {
        try {
            log.info("Fetching candidate details for ID: {}", candidateId);

            String url = UriComponentsBuilder.fromUriString(zohoRecruitPortalConnectionConfig.getApiBaseUrl())
                    .path(zohoRecruitPortalConnectionConfig.getCandidatesEndpoint() + "/" + candidateId)
                    .build()
                    .toUriString();

            ResponseEntity<String> response = makeAuthenticatedRequest(url, HttpMethod.GET);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return parseSingleCandidateResponse(response.getBody());
            } else {
                log.error("Failed to fetch candidate details: {}", response.getStatusCode());
                return null;
            }
        } catch (Exception e) {
            log.error("Failed to get candidate details", e);
            throw new RuntimeException("Failed to fetch candidate details from Zoho Recruit", e);
        }
    }

    /**
     * Parse candidates from API response
     */
    private List<Candidate> parseCandidatesResponse(String responseBody) {
        List<Candidate> candidates = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(responseBody);

            // Handle both direct array response and paginated response format
            JsonNode dataNode = root.has("data") ? root.get("data") : root;

            if (dataNode.isArray()) {
                for (JsonNode candidateNode : dataNode) {
                    Candidate candidate = parseCandidateFromNode(candidateNode);
                    if (candidate != null) {
                        candidates.add(candidate);
                    }
                }
            }

            log.info("Parsed {} candidates from API response", candidates.size());
        } catch (Exception e) {
            log.error("Failed to parse candidates response", e);
        }
        return candidates;
    }

    /**
     * Parse single candidate from API response
     */
    private Candidate parseSingleCandidateResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode dataNode = root.has("data") ? root.get("data") : root;
            return parseCandidateFromNode(dataNode);
        } catch (Exception e) {
            log.error("Failed to parse single candidate response", e);
            return null;
        }
    }

    /**
     * Convert JsonNode to Candidate entity
     * Maps all fields from Zoho Recruit API response to Candidate object
     *
     * Handles field name variations:
     * - API uses snake_case in some contexts but PascalCase in JSON response
     * - Some fields have multiple sources (e.g., name from Full_Name, First_Name, Last_Name)
     * - Special fields like $approved need specific handling
     */
    private Candidate parseCandidateFromNode(JsonNode node) {
        try {
            // ─────────────────────────────────────────────────────────────────────────
            // Parse Name Fields
            // ─────────────────────────────────────────────────────────────────────────
            String firstName = getTextValue(node, "First_Name");
            String lastName = getTextValue(node, "Last_Name");
            String fullName = getTextValue(node, "Full_Name");

            // Fallback to constructing full name if not provided
            if (fullName == null) {
                fullName = firstName != null && lastName != null ? firstName + " " + lastName :
                          firstName != null ? firstName :
                          lastName != null ? lastName : null;
            }

            // ─────────────────────────────────────────────────────────────────────────
            // Parse Skills (comma-separated string)
            // ─────────────────────────────────────────────────────────────────────────
            String skillsString = getTextValue(node, "Skill_Set");
            List<String> skills = new ArrayList<>();
            if (skillsString != null && !skillsString.isEmpty()) {
                skills = List.of(skillsString.split(",\\s*"));
            }

            // ─────────────────────────────────────────────────────────────────────────
            // Parse Location (try City first, then full location)
            // ─────────────────────────────────────────────────────────────────────────
            String city = getTextValue(node, "City");
            String state = getTextValue(node, "State");
            String country = getTextValue(node, "Country");

            String location = city;
            if (location == null && state != null) {
                location = state;
            }
            if (location == null && country != null) {
                location = country;
            }

            // ─────────────────────────────────────────────────────────────────────────
            // Build Candidate object with all fields
            // ─────────────────────────────────────────────────────────────────────────
            return Candidate.builder()
                    // Identifier Fields
                    .id(getTextValue(node, "id"))
                    .candidateId(getTextValue(node, "Candidate_ID"))

                    // Name Fields
                    .firstName(firstName)
                    .lastName(lastName)
                    .name(fullName)

                    // Contact Fields
                    .email(getTextValue(node, "Email"))
                    .phone(getTextValue(node, "Phone"))
                    .mobile(getTextValue(node, "Mobile"))
                    .emailOptOut(getBooleanValue(node, "Email_Opt_Out"))

                    // Job Profile Fields
                    .currentCompany(getTextValue(node, "Current_Employer"))
                    .currentDesignation(getTextValue(node, "Current_Job_Title"))
                    .designation(getTextValue(node, "Designation"))
                    .currentPosition(getTextValue(node, "Current_Job_Title"))

                    // Location Fields
                    .city(city)
                    .state(state)
                    .country(country)
                    .location(location)

                    // Skills & Experience Fields
                    .skillsString(skillsString)
                    .skills(skills)
                    .yearsOfExperience(getIntValue(node, "Experience_in_Years"))
                    .experience(getTextValue(node, "experience"))
                    .experienceDetails(getListOfMapsValue(node, "Experience_Details"))

                    // Education Fields
                    .highestQualification(getTextValue(node, "Highest_Qualification_Held"))
                    .qualifications(new ArrayList<>())
                    .educationalDetails(getListOfMapsValue(node, "Educational_Details"))

                    // Salary Fields
                    .currentSalary(getLongValue(node, "Current_Salary"))
                    .expectedSalary(getLongValue(node, "Expected_Salary"))
                    .salary(getLongValue(node, "Salary"))

                    // Document Fields
                    .resumeUrl(getTextValue(node, "resume_url"))
                    .isAttachmentPresent(getBooleanValue(node, "Is_Attachment_Present"))

                    // Status & Classification Fields
                    .status(getTextValue(node, "Candidate_Status"))
                    .isUnqualified(getBooleanValue(node, "Is_Unqualified"))
                    .approved(getBooleanValue(node, "$approved"))
                    .isLocked(getBooleanValue(node, "Is_Locked"))

                    // Source & Tracking Fields
                    .source(getTextValue(node, "Source"))
                    .associatedTags(getListOfMapsValue(node, "Associated_Tags"))
                    .rating(getMapValue(node, "Rating"))

                    // Timestamps
                    .createdTime(getTextValue(node, "Created_Time"))
                    .updatedOn(getTextValue(node, "Updated_On"))
                    .createdAt(LocalDateTime.now())

                    // User References
                    .createdBy(getMapValue(node, "Created_By"))
                    .leadOwner(getMapValue(node, "Lead_Owner"))

                    // API Response Storage
                    .zohoProfile(node)

                    .build();
        } catch (Exception e) {
            log.warn("Failed to parse candidate node", e);
            return null;
        }
    }

    /**
     * Make authenticated HTTP request to Zoho Recruit API
     */
    private ResponseEntity<String> makeAuthenticatedRequest(String url, HttpMethod method) {
        String accessToken = oauthService.getAccessToken();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.set("Accept", "application/json");

        HttpEntity<String> entity = new HttpEntity<>(headers);

        return restTemplate.exchange(url, method, entity, String.class);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // JSON parsing helper methods
    // ─────────────────────────────────────────────────────────────────────────

    private String getTextValue(JsonNode node, String field) {
        if (node.has(field) && !node.get(field).isNull()) {
            return node.get(field).asText();
        }
        return null;
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

    /**
     * Get boolean value from JsonNode
     * Safely extracts boolean values, returning null if field doesn't exist or is null
     */
    private Boolean getBooleanValue(JsonNode node, String field) {
        if (node.has(field) && !node.get(field).isNull()) {
            return node.get(field).asBoolean();
        }
        return null;
    }

    /**
     * Get long value from JsonNode
     * Safely extracts numeric values as Long, for salary and other large numbers
     */
    private Long getLongValue(JsonNode node, String field) {
        if (node.has(field) && !node.get(field).isNull()) {
            JsonNode valueNode = node.get(field);
            if (valueNode.isNumber()) {
                return valueNode.asLong();
            }
        }
        return null;
    }

    /**
     * Get map/object value from JsonNode
     * Safely extracts nested objects and converts them to Map<String, Object>
     */
    private Map<String, Object> getMapValue(JsonNode node, String field) {
        if (node.has(field) && !node.get(field).isNull() && node.get(field).isObject()) {
            try {
                return objectMapper.convertValue(node.get(field),
                    objectMapper.getTypeFactory().constructMapType(Map.class, String.class, Object.class));
            } catch (Exception e) {
                log.debug("Failed to parse map value for field {}: {}", field, e.getMessage());
                return null;
            }
        }
        return null;
    }

    /**
     * Get list of maps from JsonNode
     * Safely extracts arrays of objects and converts them to List<Map<String, Object>>
     */
    private List<Map<String, Object>> getListOfMapsValue(JsonNode node, String field) {
        List<Map<String, Object>> list = new ArrayList<>();
        if (node.has(field) && node.get(field).isArray()) {
            try {
                for (JsonNode item : node.get(field)) {
                    if (item.isObject()) {
                        Map<String, Object> map = objectMapper.convertValue(item,
                            objectMapper.getTypeFactory().constructMapType(Map.class, String.class, Object.class));
                        list.add(map);
                    }
                }
            } catch (Exception e) {
                log.debug("Failed to parse list of maps for field {}: {}", field, e.getMessage());
            }
        }
        return list;
    }
}


