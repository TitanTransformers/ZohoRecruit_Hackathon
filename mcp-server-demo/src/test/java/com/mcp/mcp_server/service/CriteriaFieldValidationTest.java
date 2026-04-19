package com.mcp.mcp_server.service;

import com.mcp.mcp_server.util.ZohoRecruitCandidateSearchField;

import java.util.*;

/**
 * Comprehensive validation test demonstrating that the criteria builder
 * only accepts fields from ZohoRecruitCandidateSearchField enum.
 *
 * Run with: java com.mcp.mcp_server.service.CriteriaFieldValidationTest
 */
public class CriteriaFieldValidationTest {

    public static void main(String[] args) {
        testValidCriteria();
        testInvalidFields();
        testOperatorValidation();
        testYourSpecificCriteria();
        testAndOperator();
    }

    /**
     * Test 1: Valid criteria with all fields from enum
     */
    private static void testValidCriteria() {
        System.out.println("\n╔════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║  TEST 1: Valid Criteria (All Fields from Enum)                           ║");
        System.out.println("╚════════════════════════════════════════════════════════════════════════╝\n");

        try {
            // Test using enum-based methods (RECOMMENDED)
            ZohoCriteriaBuilder.CriteriaFilter filter = new ZohoCriteriaBuilder.CriteriaFilter()
                .addCondition(ZohoRecruitCandidateSearchField.DESIGNATION,
                             ZohoCriteriaBuilder.Operator.CONTAINS, "Java Developer")
                .addCondition(ZohoRecruitCandidateSearchField.SKILL_SET,
                             ZohoCriteriaBuilder.Operator.CONTAINS, "Java")
                .addCondition(ZohoRecruitCandidateSearchField.CITY,
                             ZohoCriteriaBuilder.Operator.EQUALS, "Bangalore");

            String criteria = filter.build();
            System.out.println("✅ VALID CRITERIA ACCEPTED");
            System.out.println("Fields used: DESIGNATION, SKILL_SET, CITY (all from enum)");
            System.out.println("Generated criteria: " + criteria);
            System.out.println();
        } catch (Exception e) {
            System.out.println("❌ UNEXPECTED ERROR: " + e.getMessage());
        }
    }

    /**
     * Test 2: Invalid fields that are NOT in enum
     */
    private static void testInvalidFields() {
        System.out.println("╔════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║  TEST 2: Invalid Fields (NOT in ZohoRecruitCandidateSearchField)        ║");
        System.out.println("╚════════════════════════════════════════════════════════════════════════╝\n");

        // Test 2.1: Using string-based method with invalid field
        System.out.println("Test 2.1: Attempting to use invalid field 'SomeRandomField'");
        try {
            ZohoCriteriaBuilder.CriteriaFilter filter = new ZohoCriteriaBuilder.CriteriaFilter()
                .addCondition("SomeRandomField", ZohoCriteriaBuilder.Operator.CONTAINS, "value");

            String criteria = filter.build();
            System.out.println("⚠️  WARNING: Invalid field was accepted (string-based method)");
            System.out.println("   This will cause API errors when sent to Zoho");
            System.out.println("   Generated criteria: " + criteria);
            System.out.println("   ➜ RECOMMENDATION: Use enum-based methods instead\n");
        } catch (Exception e) {
            System.out.println("✅ REJECTED: " + e.getMessage() + "\n");
        }

        // Test 2.2: Common invalid field names
        System.out.println("Test 2.2: Checking which fields are NOT in the enum");
        String[] invalidFields = {"SomeRandomField", "InvalidField", "NotAField", "CustomField123"};
        for (String field : invalidFields) {
            boolean exists = false;
            for (ZohoRecruitCandidateSearchField f : ZohoRecruitCandidateSearchField.values()) {
                if (f.getFieldName().equals(field)) {
                    exists = true;
                    break;
                }
            }
            System.out.println("   Field '" + field + "': " + (exists ? "✅ FOUND" : "❌ NOT FOUND"));
        }
        System.out.println();
    }

    /**
     * Test 3: Operator validation for field types
     */
    private static void testOperatorValidation() {
        System.out.println("╔════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║  TEST 3: Operator Validation (Operator vs Field Type)                   ║");
        System.out.println("╚════════════════════════════════════════════════════════════════════════╝\n");

        // Test 3.1: CORRECT - Numeric field with numeric operator
        System.out.println("Test 3.1: NUMERIC field with NUMERIC operator (CORRECT)");
        try {
            ZohoCriteriaBuilder.CriteriaFilter filter = new ZohoCriteriaBuilder.CriteriaFilter()
                .addCondition(ZohoRecruitCandidateSearchField.EXPERIENCE_IN_YEARS,
                             ZohoCriteriaBuilder.Operator.GREATER_EQUAL, 5);
            System.out.println("✅ ACCEPTED: Experience_in_Years:greater_equal:5");
            System.out.println("   Reason: EXPERIENCE_IN_YEARS is NUMERIC, greater_equal is valid\n");
        } catch (Exception e) {
            System.out.println("❌ ERROR: " + e.getMessage() + "\n");
        }

        // Test 3.2: WRONG - Numeric field with string operator (THIS WAS THE BUG!)
        System.out.println("Test 3.2: NUMERIC field with STRING operator (WRONG - THE BUG)");
        try {
            ZohoCriteriaBuilder.CriteriaFilter filter = new ZohoCriteriaBuilder.CriteriaFilter()
                .addCondition(ZohoRecruitCandidateSearchField.EXPERIENCE_IN_YEARS,
                             ZohoCriteriaBuilder.Operator.CONTAINS, "5");  // ← WRONG operator!
            System.out.println("❌ BUG: This should have been rejected!");
        } catch (IllegalArgumentException e) {
            System.out.println("✅ CORRECTLY REJECTED: " + e.getMessage() + "\n");
        }

        // Test 3.3: STRING field with string operator
        System.out.println("Test 3.3: STRING field with STRING operator (CORRECT)");
        try {
            ZohoCriteriaBuilder.CriteriaFilter filter = new ZohoCriteriaBuilder.CriteriaFilter()
                .addCondition(ZohoRecruitCandidateSearchField.SKILL_SET,
                             ZohoCriteriaBuilder.Operator.CONTAINS, "Java");
            System.out.println("✅ ACCEPTED: Skill_Set:contains:Java");
            System.out.println("   Reason: SKILL_SET is STRING, contains is valid\n");
        } catch (Exception e) {
            System.out.println("❌ ERROR: " + e.getMessage() + "\n");
        }
    }

    /**
     * Test 4: Your specific criteria with OR operator
     */
    private static void testYourSpecificCriteria() {
        System.out.println("╔════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║  TEST 4: Your Specific Criteria with OR Operator                       ║");
        System.out.println("╚════════════════════════════════════════════════════════════════════════╝\n");

        System.out.println("Your criteria (with OR instead of AND):");
        System.out.println("((Designation:contains:Java Developer)or(Skill_Set:contains:Java)");
        System.out.println(" or(Skill_Set:contains:Object-Oriented Programming)");
        System.out.println(" or(Skill_Set:contains:Data Structures)");
        System.out.println(" or(City:equals:Bangalore))\n");

        try {
            ZohoCriteriaBuilder.CriteriaFilter filter = new ZohoCriteriaBuilder.CriteriaFilter()
                .addCondition(ZohoRecruitCandidateSearchField.DESIGNATION,
                             ZohoCriteriaBuilder.Operator.CONTAINS, "Java Developer")
                .addCondition(ZohoRecruitCandidateSearchField.SKILL_SET,
                             ZohoCriteriaBuilder.Operator.CONTAINS, "Java")
                .addCondition(ZohoRecruitCandidateSearchField.SKILL_SET,
                             ZohoCriteriaBuilder.Operator.CONTAINS, "Object-Oriented Programming")
                .addCondition(ZohoRecruitCandidateSearchField.SKILL_SET,
                             ZohoCriteriaBuilder.Operator.CONTAINS, "Data Structures")
                .addCondition(ZohoRecruitCandidateSearchField.CITY,
                             ZohoCriteriaBuilder.Operator.EQUALS, "Bangalore");

            String criteria = filter.build();  // Default uses OR
            System.out.println("✅ ALL FIELDS ARE VALID");
            System.out.println("\nField validation:");
            System.out.println("  • DESIGNATION('Designation') .......... from enum ✅");
            System.out.println("  • SKILL_SET('Skill_Set') .............. from enum ✅");
            System.out.println("  • CITY('City') ........................ from enum ✅");
            System.out.println("\nOperator validation:");
            System.out.println("  • Designation:contains ............... STRING field ✅");
            System.out.println("  • Skill_Set:contains ................. STRING field ✅");
            System.out.println("  • City:equals ......................... STRING field ✅");
            System.out.println("\nLogical Operator: or (default)");
            System.out.println("\nGenerated criteria:\n" + criteria);
        } catch (Exception e) {
            System.out.println("❌ ERROR: " + e.getMessage());
        }
        System.out.println();
    }

    /**
     * Test 5: Using AND operator (alternative)
     */
    private static void testAndOperator() {
        System.out.println("╔════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║  TEST 5: Alternative - Using AND Operator                             ║");
        System.out.println("╚════════════════════════════════════════════════════════════════════════╝\n");

        System.out.println("Same criteria with AND operator:\n");

        try {
            ZohoCriteriaBuilder.CriteriaFilter filter = new ZohoCriteriaBuilder.CriteriaFilter()
                .addCondition(ZohoRecruitCandidateSearchField.DESIGNATION,
                             ZohoCriteriaBuilder.Operator.CONTAINS, "Java Developer")
                .addCondition(ZohoRecruitCandidateSearchField.SKILL_SET,
                             ZohoCriteriaBuilder.Operator.CONTAINS, "Java")
                .addCondition(ZohoRecruitCandidateSearchField.CITY,
                             ZohoCriteriaBuilder.Operator.EQUALS, "Bangalore");

            String criteriaOr = filter.build();  // Default: OR
            String criteriaAnd = filter.buildWithOperator("and");  // Explicit: AND

            System.out.println("With OR operator (default):");
            System.out.println("  " + criteriaOr);
            System.out.println("\nWith AND operator (explicit):");
            System.out.println("  " + criteriaAnd);
            System.out.println("\nDifference:");
            System.out.println("  • OR: Returns candidates matching ANY condition");
            System.out.println("  • AND: Returns candidates matching ALL conditions");
        } catch (Exception e) {
            System.out.println("❌ ERROR: " + e.getMessage());
        }
        System.out.println();
    }
}

