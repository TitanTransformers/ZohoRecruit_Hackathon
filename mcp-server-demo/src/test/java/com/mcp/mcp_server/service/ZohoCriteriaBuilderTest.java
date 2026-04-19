package com.mcp.mcp_server.service;

import com.mcp.mcp_server.util.ZohoRecruitCandidateSearchField;
import java.util.*;

/**
 * Test suite for ZohoCriteriaBuilder.
 * Validates all builder methods and ensures output conforms to Zoho Recruit API specification
 */
public class ZohoCriteriaBuilderTest {

    public static void main(String[] args) {
        runAllTests();
    }

    public static void runAllTests() {
        System.out.println("\n╔════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║         ZOHO RECRUIT CRITERIA BUILDER - TEST SUITE                     ║");
        System.out.println("╚════════════════════════════════════════════════════════════════════════╝\n");

        testSingleConditions();
        testCombinedConditions();
        testFluentBuilder();
        testOperators();
        testValidation();
        testEdgeCases();
        testExamples();

        System.out.println("\n✅ All tests completed!\n");
    }

    private static void testSingleConditions() {
        System.out.println("▶ TEST 1: Single Conditions");
        System.out.println("─────────────────────────────────────────────────────────────────────────\n");

        // Test 1.1: Skill condition using fluent builder
        String skillCriteria = new ZohoCriteriaBuilder.CriteriaFilter()
            .addSkill("Java")
            .build();
        assert skillCriteria.equals("(Skill_Set:contains:Java)") :
            "Skill condition failed: " + skillCriteria;
        System.out.println("✓ Test 1.1: Skill condition");
        System.out.println("  Output: " + skillCriteria);

        // Test 1.2: Status condition using fluent builder
        String statusCriteria = new ZohoCriteriaBuilder.CriteriaFilter()
            .addStatus("Shortlisted")
            .build();
        assert statusCriteria.equals("(Candidate_Status:equals:Shortlisted)") :
            "Status condition failed: " + statusCriteria;
        System.out.println("✓ Test 1.2: Status condition");
        System.out.println("  Output: " + statusCriteria);

        // Test 1.3: Experience condition using fluent builder
        String experienceCriteria = new ZohoCriteriaBuilder.CriteriaFilter()
            .addExperience(5)
            .build();
        assert experienceCriteria.equals("(Experience_in_Years:greater_equal:5)") :
            "Experience condition failed: " + experienceCriteria;
        System.out.println("✓ Test 1.3: Experience condition");
        System.out.println("  Output: " + experienceCriteria);

        // Test 1.4: Location condition using fluent builder
        String locationCriteria = new ZohoCriteriaBuilder.CriteriaFilter()
            .addLocation("Bengaluru")
            .build();
        assert locationCriteria.equals("(City:equals:Bengaluru)") :
            "Location condition failed: " + locationCriteria;
        System.out.println("✓ Test 1.4: Location condition");
        System.out.println("  Output: " + locationCriteria);

        System.out.println("\n");
    }

    private static void testCombinedConditions() {
        System.out.println("▶ TEST 2: Combined Conditions");
        System.out.println("─────────────────────────────────────────────────────────────────────────\n");

        // Test 2.1: Skill and Status
        String skillStatusCriteria = new ZohoCriteriaBuilder.CriteriaFilter()
            .addSkill("Python")
            .addStatus("New")
            .build();
        assert skillStatusCriteria.contains("Skill_Set:contains:Python") &&
               skillStatusCriteria.contains("Candidate_Status:equals:New") :
            "Skill and Status condition failed: " + skillStatusCriteria;
        System.out.println("✓ Test 2.1: Skill and Status");
        System.out.println("  Output: " + skillStatusCriteria);

        // Test 2.2: Skill and Experience
        String skillExpCriteria = new ZohoCriteriaBuilder.CriteriaFilter()
            .addSkill("Python")
            .addExperience(5)
            .build();
        assert skillExpCriteria.contains("Skill_Set:contains:Python") &&
               skillExpCriteria.contains("Experience_in_Years:greater_equal:5") :
            "Skill and Experience condition failed: " + skillExpCriteria;
        System.out.println("✓ Test 2.2: Skill and Experience");
        System.out.println("  Output: " + skillExpCriteria);

        // Test 2.3: City and Status
        String cityStatusCriteria = new ZohoCriteriaBuilder.CriteriaFilter()
            .addLocation("Bengaluru")
            .addStatus("New")
            .build();
        assert cityStatusCriteria.equals("((City:equals:Bengaluru)or(Candidate_Status:equals:New))") :
            "City and Status condition failed: " + cityStatusCriteria;
        System.out.println("✓ Test 2.3: City and Status");
        System.out.println("  Output: " + cityStatusCriteria);

        // Test 2.4: Skill, Experience, and City
        String complexCriteria = new ZohoCriteriaBuilder.CriteriaFilter()
            .addSkill("Java")
            .addExperience(3)
            .addLocation("Delhi")
            .build();
        assert complexCriteria.contains("Skill_Set:contains:Java") &&
               complexCriteria.contains("Experience_in_Years:greater_equal:3") &&
               complexCriteria.contains("City:equals:Delhi") :
            "Complex condition failed: " + complexCriteria;
        System.out.println("✓ Test 2.4: Skill, Experience, and City");
        System.out.println("  Output: " + complexCriteria);

        System.out.println("\n");
    }

    private static void testFluentBuilder() {
        System.out.println("▶ TEST 3: Fluent Builder");
        System.out.println("─────────────────────────────────────────────────────────────────────────\n");

        // Test 3.1: Simple fluent builder
        String criteria1 = new ZohoCriteriaBuilder.CriteriaFilter()
            .addSkill("Java")
            .build();
        assert criteria1.equals("(Skill_Set:contains:Java)") :
            "Simple fluent builder failed: " + criteria1;
        System.out.println("✓ Test 3.1: Simple fluent builder");
        System.out.println("  Output: " + criteria1);

        // Test 3.2: Multiple conditions with fluent builder
        String criteria2 = new ZohoCriteriaBuilder.CriteriaFilter()
            .addSkill("Python")
            .addExperience(5)
            .addLocation("Bengaluru")
            .build();
        assert criteria2.contains("Skill_Set:contains:Python") &&
               criteria2.contains("Experience_in_Years:greater_equal:5") &&
               criteria2.contains("City:equals:Bengaluru") :
            "Multiple conditions fluent builder failed: " + criteria2;
        System.out.println("✓ Test 3.2: Multiple conditions with fluent builder");
        System.out.println("  Output: " + criteria2);

        // Test 3.3: With status
        String criteria3 = new ZohoCriteriaBuilder.CriteriaFilter()
            .addSkill("Java")
            .addStatus("Shortlisted")
            .build();
        assert criteria3.contains("Skill_Set:contains:Java") &&
               criteria3.contains("Candidate_Status:equals:Shortlisted") :
            "Fluent builder with status failed: " + criteria3;
        System.out.println("✓ Test 3.3: Fluent builder with status");
        System.out.println("  Output: " + criteria3);

        System.out.println("\n");
    }

    private static void testOperators() {
        System.out.println("▶ TEST 4: Operators");
        System.out.println("─────────────────────────────────────────────────────────────────────────\n");

        // Test 4.1: Contains operator
        String contains = ZohoCriteriaBuilder.buildCondition("Skill_Set",
            ZohoCriteriaBuilder.Operator.CONTAINS, "Java");
        assert contains.equals("(Skill_Set:contains:Java)") :
            "Contains operator failed: " + contains;
        System.out.println("✓ Test 4.1: Contains operator");
        System.out.println("  Output: " + contains);

        // Test 4.2: Equals operator
        String equals = ZohoCriteriaBuilder.buildCondition("Candidate_Status",
            ZohoCriteriaBuilder.Operator.EQUALS, "New");
        assert equals.equals("(Candidate_Status:equals:New)") :
            "Equals operator failed: " + equals;
        System.out.println("✓ Test 4.2: Equals operator");
        System.out.println("  Output: " + equals);

        // Test 4.3: Not_equal operator
        String notEqual = ZohoCriteriaBuilder.buildCondition("Candidate_Status",
            ZohoCriteriaBuilder.Operator.NOT_EQUAL, "Rejected");
        assert notEqual.equals("(Candidate_Status:not_equal:Rejected)") :
            "Not_equal operator failed: " + notEqual;
        System.out.println("✓ Test 4.3: Not_equal operator");
        System.out.println("  Output: " + notEqual);

        // Test 4.4: Greater_equal operator (numeric)
        String greaterEqual = ZohoCriteriaBuilder.buildCondition("Experience_in_Years",
            ZohoCriteriaBuilder.Operator.GREATER_EQUAL, "5");
        assert greaterEqual.equals("(Experience_in_Years:greater_equal:5)") :
            "Greater_equal operator failed: " + greaterEqual;
        System.out.println("✓ Test 4.4: Greater_equal operator");
        System.out.println("  Output: " + greaterEqual);

        // Test 4.5: Less_equal operator (numeric)
        String lessEqual = ZohoCriteriaBuilder.buildCondition("Current_Salary",
            ZohoCriteriaBuilder.Operator.LESS_EQUAL, "500000");
        assert lessEqual.equals("(Current_Salary:less_equal:500000)") :
            "Less_equal operator failed: " + lessEqual;
        System.out.println("✓ Test 4.5: Less_equal operator");
        System.out.println("  Output: " + lessEqual);

        // Test 4.6: Starts_with operator
        String startsWith = ZohoCriteriaBuilder.buildCondition("Skill_Set",
            ZohoCriteriaBuilder.Operator.STARTS_WITH, "Cloud");
        assert startsWith.equals("(Skill_Set:starts_with:Cloud)") :
            "Starts_with operator failed: " + startsWith;
        System.out.println("✓ Test 4.6: Starts_with operator");
        System.out.println("  Output: " + startsWith);

        System.out.println("\n");
    }

    private static void testValidation() {
        System.out.println("▶ TEST 5: Validation");
        System.out.println("─────────────────────────────────────────────────────────────────────────\n");

        // Test 5.1: Valid single condition
        String validSingle = "(Skill_Set:contains:Java)";
        assert ZohoCriteriaBuilder.validateCriteria(validSingle) :
            "Validation failed for valid single condition";
        System.out.println("✓ Test 5.1: Valid single condition");
        System.out.println("  Input: " + validSingle);

        // Test 5.2: Valid chained conditions with OR (default)
        String validChained = "((Skill_Set:contains:Python)or(Experience_in_Years:greater_equal:5))";
        assert ZohoCriteriaBuilder.validateCriteria(validChained) :
            "Validation failed for valid chained conditions";
        System.out.println("✓ Test 5.2: Valid chained conditions (with OR)");
        System.out.println("  Input: " + validChained);

        // Test 5.3: Invalid format (missing parentheses)
        String invalid1 = "Skill_Set:contains:Java";
        assert !ZohoCriteriaBuilder.validateCriteria(invalid1) :
            "Validation should fail for missing parentheses";
        System.out.println("✓ Test 5.3: Invalid format detection (missing parentheses)");

        // Test 5.4: Empty criteria
        String invalid2 = "";
        assert !ZohoCriteriaBuilder.validateCriteria(invalid2) :
            "Validation should fail for empty criteria";
        System.out.println("✓ Test 5.4: Invalid format detection (empty criteria)");

        System.out.println("\n");
    }

    private static void testEdgeCases() {
        System.out.println("▶ TEST 6: Edge Cases");
        System.out.println("─────────────────────────────────────────────────────────────────────────\n");

        // Test 6.1: Null field should throw exception
        try {
            ZohoCriteriaBuilder.buildCondition(null, "contains", "value");
            System.out.println("✗ Test 6.1: Should have thrown exception for null field");
            assert false;
        } catch (IllegalArgumentException e) {
            System.out.println("✓ Test 6.1: Null field validation");
            System.out.println("  Expected exception: " + e.getMessage());
        }

        // Test 6.2: Empty value should throw exception
        try {
            ZohoCriteriaBuilder.buildCondition("Field", "equals", "");
            System.out.println("✗ Test 6.2: Should have thrown exception for empty value");
            assert false;
        } catch (IllegalArgumentException e) {
            System.out.println("✓ Test 6.2: Empty value validation");
            System.out.println("  Expected exception: " + e.getMessage());
        }

        // Test 6.3: Too many conditions (>10)
        try {
            ZohoCriteriaBuilder.CriteriaFilter filterWithTooMany = new ZohoCriteriaBuilder.CriteriaFilter();
            for (int i = 0; i < 11; i++) {
                filterWithTooMany.addCondition("Skill_Set", ZohoCriteriaBuilder.Operator.CONTAINS, "Skill" + i);
            }
            filterWithTooMany.build();
            System.out.println("✗ Test 6.3: Should have thrown exception for >10 conditions");
            assert false;
        } catch (IllegalArgumentException e) {
            System.out.println("✓ Test 6.3: Too many conditions validation");
            System.out.println("  Expected exception: " + e.getMessage());
        }

        // Test 6.4: Invalid operator
        try {
            ZohoCriteriaBuilder.Operator.fromString("invalid_op");
            System.out.println("✗ Test 6.4: Should have thrown exception for invalid operator");
            assert false;
        } catch (IllegalArgumentException e) {
            System.out.println("✓ Test 6.4: Invalid operator validation");
            System.out.println("  Expected exception: " + e.getMessage());
        }

        System.out.println("\n");
    }

    private static void testExamples() {
        System.out.println("▶ TEST 7: Real-World Examples");
        System.out.println("─────────────────────────────────────────────────────────────────────────\n");

        // Example 1: Simple skill search
        System.out.println("✓ Example 1: Simple skill search");
        String ex1 = new ZohoCriteriaBuilder.CriteriaFilter()
            .addSkill("Java")
            .build();
        System.out.println("  Criteria: " + ex1);
        System.out.println("  Use case: Find Java developers");

        // Example 2: Skill with experience
        System.out.println("\n✓ Example 2: Skill with experience requirement");
        String ex2 = new ZohoCriteriaBuilder.CriteriaFilter()
            .addSkill("Python")
            .addExperience(5)
            .build();
        System.out.println("  Criteria: " + ex2);
        System.out.println("  Use case: Find Python developers with 5+ years");

        // Example 3: City and status (with OR)
        System.out.println("\n✓ Example 3: City and status (with OR - default)");
        String ex3 = new ZohoCriteriaBuilder.CriteriaFilter()
            .addLocation("Bengaluru")
            .addStatus("New")
            .build();
        System.out.println("  Criteria: " + ex3);
        System.out.println("  Use case: Find candidates in Bengaluru OR with status 'New'");

        // Example 4: Advanced query with fluent builder
        System.out.println("\n✓ Example 4: Advanced query (with OR)");
        String ex4 = new ZohoCriteriaBuilder.CriteriaFilter()
            .addSkill("Kubernetes")
            .addExperience(7)
            .addLocation("San Francisco")
            .addCondition(ZohoRecruitCandidateSearchField.CURRENT_SALARY, ZohoCriteriaBuilder.Operator.GREATER_EQUAL, 150000)
            .build();
        System.out.println("  Criteria: " + ex4);
        System.out.println("  Use case: K8s engineers OR 7+ yrs exp OR in SF OR $150K+ salary");

        // Example 5: Salary range filtering (with AND)
        System.out.println("\n✓ Example 5: Salary range filtering (with AND)");
        String ex5 = new ZohoCriteriaBuilder.CriteriaFilter()
            .addSalary(300000, 800000)
            .buildWithOperator("and");  // Use AND for range filtering
        System.out.println("  Criteria: " + ex5);
        System.out.println("  Use case: Find candidates with salary between 300K-800K (must match BOTH conditions)");

        // Example 6: Show difference between OR and AND
        System.out.println("\n✓ Example 6: Difference between OR and AND operators");
        ZohoCriteriaBuilder.CriteriaFilter filterEx6 = new ZohoCriteriaBuilder.CriteriaFilter()
            .addSkill("Java")
            .addLocation("Bengaluru");
        String ex6Or = filterEx6.build();  // Default OR
        String ex6And = filterEx6.buildWithOperator("and");  // Explicit AND
        System.out.println("  With OR (default):  " + ex6Or);
        System.out.println("    → Returns: Java developers OR located in Bengaluru");
        System.out.println("  With AND (explicit): " + ex6And);
        System.out.println("    → Returns: Java developers located in Bengaluru (both conditions)");

        System.out.println("\n");
    }
}

