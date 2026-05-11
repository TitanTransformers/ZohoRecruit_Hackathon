package com.mcp.mcp_server.service;

import com.mcp.mcp_server.util.ZohoRecruitCandidateSearchField;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * Zoho Recruit Criteria Builder - Simple & Efficient with Field & Operator Validation
 *
 * Builds filter expressions for Zoho Recruit API.
 *
 * Format: (Field:operator:value) or ((Field1:op:val1)or(Field2:op:val2))
 * Operators: equals, not_equal, contains, starts_with, greater_than, greater_equal, less_than, less_equal
 *
 * Logical Operators: or (default), and (optional via buildWithOperator)
 *
 * VALIDATION RULES:
 * - Only fields from ZohoRecruitCandidateSearchField enum are allowed
 * - Operators must match field type:
 *   * STRING fields: equals, not_equal, contains, starts_with
 *   * NUMERIC fields: greater_than, greater_equal, less_than, less_equal, equals
 *   * BOOLEAN fields: equals only
 *   * COMPLEX fields: not supported for filtering
 */
@Slf4j
public class ZohoCriteriaBuilder {

    public enum Operator {
        EQUALS("equals"),
        NOT_EQUAL("not_equal"),
        CONTAINS("contains"),
        STARTS_WITH("starts_with"),
        GREATER_THAN("greater_than"),
        GREATER_EQUAL("greater_equal"),
        LESS_THAN("less_than"),
        LESS_EQUAL("less_equal");

        private final String value;

        Operator(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static Operator fromString(String value) {
            for (Operator op : Operator.values()) {
                if (op.value.equalsIgnoreCase(value)) {
                    return op;
                }
            }
            throw new IllegalArgumentException("Unknown operator: " + value);
        }

        /**
         * Check if this operator is valid for the given field type
         */
        public boolean isValidForFieldType(ZohoRecruitCandidateSearchField.FieldType fieldType) {
            return switch (fieldType) {
                case STRING -> this == EQUALS || this == NOT_EQUAL || this == CONTAINS || this == STARTS_WITH;
                case NUMERIC -> this == EQUALS || this == GREATER_THAN || this == GREATER_EQUAL ||
                               this == LESS_THAN || this == LESS_EQUAL;
                case BOOLEAN -> this == EQUALS;
                case COMPLEX -> false; // Complex types don't support filtering
            };
        }
    }

    /**
     * Build single condition: (Field:operator:value)
     * Uses field name directly (string-based)
     */
    public static String buildCondition(String field, Operator operator, String value) {
        validateInput(field, value);
        // Try to find matching ZohoRecruitCandidateSearchField enum
        ZohoRecruitCandidateSearchField zField = findFieldByName(field);
        if (zField != null) {
            validateOperatorForField(operator, zField);
        } else {
            log.warn("Field '{}' not found in ZohoRecruitCandidateSearchField enum. Using as-is.", field);
        }
        return String.format("(%s:%s:%s)", field, operator.getValue(), value);
    }

    /**
     * Build condition with string operator
     */
    public static String buildCondition(String field, String operator, String value) {
        return buildCondition(field, Operator.fromString(operator), value);
    }

    /**
     * Build condition using ZohoRecruitField enum (type-safe)
     * RECOMMENDED: Use this method to ensure field validity
     */
    public static String buildCondition(ZohoRecruitCandidateSearchField field, Operator operator, String value) {
        validateOperatorForField(operator, field);
        validateInput(field.getFieldName(), value);
        return String.format("(%s:%s:%s)", field.getFieldName(), operator.getValue(), value);
    }

    /**
     * Build condition using ZohoRecruitField enum with numeric value
     * RECOMMENDED: Use this method for numeric fields
     */
    public static String buildCondition(ZohoRecruitCandidateSearchField field, Operator operator, Number value) {
        if (!field.isNumeric()) {
            throw new IllegalArgumentException(
                String.format("Field '%s' is not numeric (type: %s). Use String-based method instead.",
                    field.getFieldName(), field.getFieldType())
            );
        }
        validateOperatorForField(operator, field);
        return String.format("(%s:%s:%s)", field.getFieldName(), operator.getValue(), value);
    }

    /**
     * Validate that the operator is compatible with the field type
     */
    private static void validateOperatorForField(Operator operator, ZohoRecruitCandidateSearchField field) {
        if (!operator.isValidForFieldType(field.getFieldType())) {
            throw new IllegalArgumentException(
                String.format("Operator '%s' is not valid for field '%s' (type: %s). Valid operators: %s",
                    operator.getValue(),
                    field.getFieldName(),
                    field.getFieldType(),
                    getValidOperatorsForFieldType(field.getFieldType()))
            );
        }
    }

    /**
     * Find ZohoRecruitCandidateSearchField by field name
     */
    private static ZohoRecruitCandidateSearchField findFieldByName(String fieldName) {
        if (fieldName == null || fieldName.isEmpty()) {
            return null;
        }
        for (ZohoRecruitCandidateSearchField field : ZohoRecruitCandidateSearchField.values()) {
            if (field.getFieldName().equals(fieldName)) {
                return field;
            }
        }
        return null;
    }

    /**
     * Get valid operators for a field type
     */
    private static String getValidOperatorsForFieldType(ZohoRecruitCandidateSearchField.FieldType fieldType) {
        return switch (fieldType) {
            case STRING -> "equals, not_equal, contains, starts_with";
            case NUMERIC -> "equals, greater_than, greater_equal, less_than, less_equal";
            case BOOLEAN -> "equals";
            case COMPLEX -> "N/A (complex fields don't support filtering)";
        };
    }

    /**
     * Build chained AND conditions (max 10)
     * Format: ((Field1:op:val1)and(Field2:op:val2))
     * @deprecated Use CriteriaFilter.build() instead for better validation
     */
    @Deprecated(forRemoval = true)
    public static String buildAndConditions(List<Map<String, String>> conditions) {
        if (conditions == null || conditions.isEmpty()) {
            throw new IllegalArgumentException("Conditions cannot be null or empty");
        }
        if (conditions.size() > 10) {
            throw new IllegalArgumentException("Maximum 10 conditions allowed, got " + conditions.size());
        }

        List<String> builtConditions = new ArrayList<>();
        for (Map<String, String> cond : conditions) {
            builtConditions.add(buildCondition(
                    cond.get("field"),
                    cond.get("operator"),
                    cond.get("value")
            ));
        }

        return "(" + String.join("and", builtConditions) + ")";
    }

    /**
     * Create condition map for buildAndConditions()
     * @deprecated Use CriteriaFilter instead for better type safety and validation
     */
    @Deprecated(forRemoval = true)
    public static Map<String, String> condition(String field, String operator, String value) {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("field", field);
        map.put("operator", operator);
        map.put("value", value);
        return map;
    }

    /**
     * Validate criteria format
     */
    public static boolean validateCriteria(String criteria) {
        if (criteria == null || criteria.isEmpty()) {
            return false;
        }
        if (!criteria.startsWith("(") || !criteria.endsWith(")")) {
            return false;
        }
        return criteria.contains(":equals:") || criteria.contains(":not_equal:") ||
               criteria.contains(":contains:") || criteria.contains(":starts_with:") ||
               criteria.contains(":greater_than:") || criteria.contains(":greater_equal:") ||
               criteria.contains(":less_than:") || criteria.contains(":less_equal:");
    }

    private static void validateInput(String field, String value) {
        if (field == null || field.isEmpty() || value == null || value.isEmpty()) {
            throw new IllegalArgumentException("Field and value cannot be null or empty");
        }
    }

    /**
     * Fluent builder for creating criteria with multiple conditions
     * RECOMMENDED: Use enum-based methods (ZohoRecruitCandidateSearchField) for type-safety
     */
    public static class CriteriaFilter {
        private final List<Map<String, Object>> conditions = new ArrayList<>();

        /**
         * Add condition using string field name (use with caution - no type validation)
         */
        public CriteriaFilter addCondition(String field, Operator op, String value) {
            validateInput(field, value);
            Map<String, Object> cond = new LinkedHashMap<>();
            cond.put("field", field);
            cond.put("operator", op.getValue());
            cond.put("value", value);
            cond.put("validated", false); // Mark as unvalidated
            conditions.add(cond);
            return this;
        }

        /**
         * Add condition using enum field (RECOMMENDED - type-safe with validation)
         */
        public CriteriaFilter addCondition(ZohoRecruitCandidateSearchField field, Operator op, String value) {
            validateOperatorForField(op, field);
            validateInput(field.getFieldName(), value);
            Map<String, Object> cond = new LinkedHashMap<>();
            cond.put("field", field.getFieldName());
            cond.put("operator", op.getValue());
            cond.put("value", value);
            cond.put("validated", true); // Mark as validated
            conditions.add(cond);
            return this;
        }

        /**
         * Add condition using enum field with numeric value (RECOMMENDED for numeric fields)
         */
        public CriteriaFilter addCondition(ZohoRecruitCandidateSearchField field, Operator op, Number value) {
            if (!field.isNumeric()) {
                throw new IllegalArgumentException(
                    String.format("Field '%s' is not numeric. Use String-based method.",
                        field.getFieldName())
                );
            }
            validateOperatorForField(op, field);
            Map<String, Object> cond = new LinkedHashMap<>();
            cond.put("field", field.getFieldName());
            cond.put("operator", op.getValue());
            cond.put("value", value.toString());
            cond.put("validated", true);
            conditions.add(cond);
            return this;
        }

        public CriteriaFilter addSkill(String skill) {
            return addCondition(ZohoRecruitCandidateSearchField.SKILL_SET, Operator.CONTAINS, skill);
        }

        public CriteriaFilter addExperience(Integer years) {
            return addCondition(ZohoRecruitCandidateSearchField.EXPERIENCE_IN_YEARS, Operator.GREATER_EQUAL, years);
        }

        public CriteriaFilter addLocation(String city) {
            return addCondition(ZohoRecruitCandidateSearchField.CITY, Operator.EQUALS, city);
        }

        public CriteriaFilter addStatus(String status) {
            return addCondition(ZohoRecruitCandidateSearchField.CANDIDATE_STATUS, Operator.EQUALS, status);
        }

        public CriteriaFilter addSalary(Integer minSalary, Integer maxSalary) {
            addCondition(ZohoRecruitCandidateSearchField.CURRENT_SALARY, Operator.GREATER_EQUAL, minSalary);
            addCondition(ZohoRecruitCandidateSearchField.CURRENT_SALARY, Operator.LESS_EQUAL, maxSalary);
            return this;
        }

        public CriteriaFilter addQualification(String qualification) {
            return addCondition(ZohoRecruitCandidateSearchField.HIGHEST_QUALIFICATION_HELD, Operator.EQUALS, qualification);
        }

        public CriteriaFilter addJobTitle(String title) {
            return addCondition(ZohoRecruitCandidateSearchField.CURRENT_JOB_TITLE, Operator.CONTAINS, title);
        }

        public CriteriaFilter addEmployer(String employer) {
            return addCondition(ZohoRecruitCandidateSearchField.CURRENT_EMPLOYER, Operator.CONTAINS, employer);
        }

        public CriteriaFilter addEmail(String email) {
            return addCondition(ZohoRecruitCandidateSearchField.EMAIL, Operator.EQUALS, email);
        }

        public CriteriaFilter addMobile(String mobile) {
            return addCondition(ZohoRecruitCandidateSearchField.MOBILE, Operator.EQUALS, mobile);
        }

        public CriteriaFilter addState(String state) {
            return addCondition(ZohoRecruitCandidateSearchField.STATE, Operator.EQUALS, state);
        }

        public CriteriaFilter addCountry(String country) {
            return addCondition(ZohoRecruitCandidateSearchField.COUNTRY, Operator.EQUALS, country);
        }

        public boolean isEmpty() {
            return conditions.isEmpty();
        }

        public String build() {
            return buildWithOperator("or");
        }

        /**
         * Build criteria with specified logical operator (and/or)
         * Default is "or"
         *
         * Format for multiple conditions: ((field1:op1:val1))or((field2:op2:val2))or...
         * Format for single condition: (field:op:val)
         */
        public String buildWithOperator(String logicalOperator) {
            if (conditions.isEmpty()) {
                throw new IllegalArgumentException("At least one condition required");
            }
            if (conditions.size() > 10) {
                throw new IllegalArgumentException("Maximum 10 conditions, got " + conditions.size());
            }
            if (conditions.size() == 1) {
                Map<String, Object> c = conditions.get(0);
                return buildCondition(
                    (String) c.get("field"),
                    Operator.fromString((String) c.get("operator")),
                    (String) c.get("value")
                );
            }

            // Build chained conditions with specified operator
            // Each condition is wrapped with double parentheses: ((field:op:val))
            // Multiple conditions are joined with the operator, not wrapped again
            List<String> builtConditions = new ArrayList<>();
            for (Map<String, Object> cond : conditions) {
                String builtCond = buildCondition(
                    (String) cond.get("field"),
                    Operator.fromString((String) cond.get("operator")),
                    (String) cond.get("value")
                );
                // Wrap each condition with double parentheses for correct Zoho API format
                builtConditions.add("(" + builtCond + ")");
            }
            return String.join(logicalOperator, builtConditions);
        }

        @Override
        public String toString() {
            try {
                return build();
            } catch (Exception e) {
                return "Invalid: " + e.getMessage();
            }
        }
    }
}


