# Search Criteria Builder - Flow Diagram

## High-Level Flow

```
Job Description Input
    ↓
    ├─ Skills: [Java, Python, Spring]
    ├─ Designation: Senior Developer  
    ├─ Location: Bangalore
    ├─ Min Experience: 5 years
    └─ Max Experience: 10 years
    
    ↓ (Parse & Extract)
    
buildCriteriaStringUsingBuilder()
    ↓
    ├─ Create orFilter (for OR conditions)
    ├─ Create experience variables (min, max)
    ↓
    For each search criterion:
    ├─ "skills" → orFilter.addSkill("Java") ┐
    ├─ "skills" → orFilter.addSkill("Python") ├─ Added to orFilter
    ├─ "skills" → orFilter.addSkill("Spring") ┤ (will be OR'd)
    ├─ "designation" → orFilter.addCondition(...) │
    ├─ "location" → orFilter.addLocation(...) ┘
    ├─ "min_experience_years" → minExperience = 5  (separate)
    └─ "max_experience_years" → maxExperience = 10 (separate)
    
    ↓ (Call buildCriteriaWithPrecedence)
    
buildCriteriaWithPrecedence(orFilter, minExperience, maxExperience)
    ↓
    ├─ Step 1: orFilter.build()
    │   └─ Returns: ((Senior Developer:contains:...))or((Skill_Set:contains:Java))or((Skill_Set:contains:Python))or((Skill_Set:contains:Spring))or((City:equals:Bangalore))
    │
    ├─ Step 2: Wrap OR group in parentheses
    │   └─ Added to finalConditions: (((Senior Developer:contains:...))or((Skill_Set:contains:Java))or(...))
    │
    ├─ Step 3: Build experience range
    │   ├─ minExpCond: (Experience_in_Years:greater_equal:5)
    │   ├─ maxExpCond: (Experience_in_Years:less_equal:10)
    │   └─ Added to finalConditions: ((Experience_in_Years:greater_equal:5)and(Experience_in_Years:less_equal:10))
    │
    └─ Step 4: Join finalConditions with "and"
        └─ Result: (((Senior Developer:contains:...))or((Skill_Set:contains:Java))or(...))and((Experience_in_Years:greater_equal:5)and(Experience_in_Years:less_equal:10))
        
    ↓ (Send to Zoho API)
    
Zoho Recruit API
    ↓
    Parse criteria with proper operator precedence
    ↓
    Return candidates matching:
    • (Designation like "Senior Developer" 
       OR Skill includes "Java"
       OR Skill includes "Python"
       OR Skill includes "Spring"
       OR City = "Bangalore")
    AND
    • (Experience >= 5 AND Experience <= 10)
    
    ↓
    
Results: 20-50 candidates ✅
```

---

## Key Difference - Before vs After

### BEFORE (Incorrect)

```
Structure: Cond1 AND Cond2 AND Cond3 AND Cond4 AND Cond5 AND Cond6

Final Criteria: ((Designation:contains:Senior Developer))
                AND ((Skill_Set:contains:Java))
                AND ((Skill_Set:contains:Python))
                AND ((Skill_Set:contains:Spring))
                AND ((Experience_in_Years:greater_equal:5))
                AND ((Experience_in_Years:less_equal:10))
                AND ((City:equals:Bangalore))

Interpretation: 
Must have "Senior Developer" (exact designation)
AND must have exactly "Java" skill
AND must have exactly "Python" skill
AND must have exactly "Spring" skill
AND must have >= 5 years
AND must have <= 10 years
AND must be in Bangalore

Result: 0 candidates (no one has ALL these skills in exact form)
```

### AFTER (Correct)

```
Structure: (OR_GROUP) AND (EXP_GROUP)

Final Criteria: (((Designation:contains:Senior Developer))
                  OR ((Skill_Set:contains:Java))
                  OR ((Skill_Set:contains:Python))
                  OR ((Skill_Set:contains:Spring))
                  OR ((City:equals:Bangalore)))
                AND
                ((Experience_in_Years:greater_equal:5)
                 AND (Experience_in_Years:less_equal:10))

Interpretation: 
Must have at least ONE of:
  • Designation contains "Senior Developer"
  • Skill includes "Java"
  • Skill includes "Python"
  • Skill includes "Spring"
  • City is "Bangalore"
AND
Must satisfy BOTH:
  • Experience >= 5
  • Experience <= 10

Result: 20-50 candidates ✅
```

---

## Operator Precedence in Zoho API

```
Zoho Recruit: AND has HIGHER precedence than OR

WRONG: A or B or C and X and Y
Interpreted as: A or B or (C and X and Y) ❌

CORRECT: (A or B or C) and (X and Y) ✅
```

---

## Code Implementation Details

### CriteriaFilter.build() Method

```java
public String build() {
    return buildWithOperator("or");
}

public String buildWithOperator(String logicalOperator) {
    List<String> builtConditions = new ArrayList<>();
    
    for (Map<String, Object> cond : conditions) {
        String builtCond = buildCondition(...);
        // Each condition wrapped: ((field:op:val))
        builtConditions.add("(" + builtCond + ")");
    }
    
    // Join with operator (or/and)
    return String.join(logicalOperator, builtConditions);
    
    // Result for OR: ((cond1))or((cond2))or((cond3))
}
```

### buildCriteriaWithPrecedence() - THE FIX

```java
private String buildCriteriaWithPrecedence(...) {
    List<String> finalConditions = new ArrayList<>();
    
    // Build OR conditions
    if (!orFilter.isEmpty()) {
        String orCriteria = orFilter.build();  
        // ((cond1))or((cond2))or((cond3))
        
        // ✅ CRITICAL FIX: Wrap in parentheses for precedence
        finalConditions.add("(" + orCriteria + ")");
        // (((cond1))or((cond2))or((cond3)))
    }
    
    // Build experience range (already wrapped)
    if (minExperience != null && maxExperience != null) {
        String expCond = "(" + minExpCond + "and" + maxExpCond + ")";
        finalConditions.add(expCond);
        // ((exp>=5)and(exp<=10))
    }
    
    // Join all groups with AND
    String result = String.join("and", finalConditions);
    
    // Final: (((OR_GROUP)))and((EXP_GROUP))
    return result;
}
```

---

## Examples

### Example 1: Multiple Skills + Experience Range + Location

**Input:**
- Skills: Java, Python, Spring
- Min Experience: 5 years
- Max Experience: 10 years
- Location: Bangalore

**Output Criteria:**
```
(((Skill_Set:contains:Java))
  or((Skill_Set:contains:Python))
  or((Skill_Set:contains:Spring))
  or((City:equals:Bangalore)))
and((Experience_in_Years:greater_equal:5)
and(Experience_in_Years:less_equal:10))
```

**Matches:**
- ✅ Java developer, 7 years, Bangalore
- ✅ Python developer, 6 years, Mumbai (matches location via OR)
- ✅ Spring developer, 8 years, Bangalore
- ✅ NodeJS developer, 9 years, Bangalore (matches location via OR)
- ❌ Java developer, 3 years, Bangalore (experience < 5)
- ❌ Java developer, 12 years, Bangalore (experience > 10)

### Example 2: Single Skill + Experience Range

**Input:**
- Skills: Java
- Min Experience: 5 years
- Max Experience: 10 years

**Output Criteria:**
```
(Skill_Set:contains:Java)
and((Experience_in_Years:greater_equal:5)
and(Experience_in_Years:less_equal:10))
```

**Matches:**
- ✅ Java developer, 6 years
- ✅ Java developer, 7 years
- ❌ Java developer, 3 years
- ❌ Python developer, 7 years

---

## Testing Scenarios

| Scenario | Expected Result |
|----------|---|
| 1 skill + experience range | Uses single skill condition AND experience range |
| 3+ skills + experience range | All skills OR'd together AND experience range |
| No skills, only experience range | Just experience range |
| Multiple locations | Locations OR'd with skills |
| Experience min only | Min >= condition |
| Experience max only | Max <= condition |
| Both min and max | Min >= AND Max <= |

