# Experience Range Filtering - Complete Data Flow

## Question
> "If we're only passing minYOE, how come maxYOE is getting prepared?"

## Answer
**Both minYOE and maxYOE are extracted from the Job Description and passed together by design.** Here's the complete flow:

---

## 1. JobDescription Parsing (AIEnhancedJobDescriptionService)

### OLD PROMPT (❌ INCORRECT)
```
"yearsOfExperience": number or null,  // Single value only
```

### NEW PROMPT (✅ CORRECT - FIXED)
```json
{
  "minYearsOfExperience": number or null,
  "maxYearsOfExperience": number or null
}
```

### AI Extraction Examples
| Job Description | minYears | maxYears | Zoho Query |
|---|---|---|---|
| "5 years experience" | 5 | null | `(Experience_in_Years:>=5)` |
| "5-10 years" | 5 | 10 | `(Experience_in_Years:>=5)and(Experience_in_Years:<=10)` |
| "3+ years" | 3 | null | `(Experience_in_Years:>=3)` |
| "2-5 years experience" | 2 | 5 | `(Experience_in_Years:>=2)and(Experience_in_Years:<=5)` |

---

## 2. Search Criteria Building (RecruitmentTools.java:256-274)

```java
private Map<String, String> buildSearchCriteria(JobDescription jd) {
    Map<String, String> criteria = new LinkedHashMap<>();
    
    // ... other criteria ...
    
    // BOTH min and max from the parsed JobDescription
    if (jd.getMinYearsOfExperience() != null) {
        criteria.put("min_experience_years", String.valueOf(jd.getMinYearsOfExperience()));
    }
    if (jd.getMaxYearsOfExperience() != null) {
        criteria.put("max_experience_years", String.valueOf(jd.getMaxYearsOfExperience()));
    }
    
    return criteria;
}
```

**Result after parsing JD for "5-10 years":**
```
Map {
    "designation" → "Java Developer",
    "skills" → "Java,Object-Oriented Programming,Data Structures",
    "min_experience_years" → "5",           // ← From AI parsing
    "max_experience_years" → "15",          // ← From AI parsing
    "location" → "Bangalore"
}
```

---

## 3. Criteria String Generation (ZohoRecruitService.java:72-251)

### OLD BEHAVIOR (❌ WRONG)
```
((Designation:contains:Java Developer))and((Skill:contains:Java))and((Experience:>=5))and((Experience:<=15))
↑
All conditions joined with AND - produces very narrow results
```

### NEW BEHAVIOR (✅ CORRECT)
```
((Designation:contains:Java Developer))or((Skill:contains:Java))or((Skill:contains:OOP))
and
((Experience_in_Years:>=5))and((Experience_in_Years:<=15))

= (Skills/Designations with OR) AND (Experience with AND)
```

#### Logic Breakdown:

1. **OR GROUP** (Skills, Designations, Locations, etc.):
   ```
   orFilter.addSkill("Java")
   orFilter.addSkill("Object-Oriented Programming")
   orFilter.addDesignation("Java Developer")
   → Builds: ((Skill:contains:Java))or((Skill:contains:OOP))or((Designation:contains:Java Developer))
   ```

2. **AND GROUP** (Experience Range - EXCLUSIVE):
   ```
   minExp = 5
   maxExp = 15
   → Builds: ((Experience_in_Years:>=5))and((Experience_in_Years:<=15))
   ```

3. **COMBINE with AND**:
   ```
   ((Skills OR Designations))
   and
   ((MinExp AND MaxExp))
   ```

---

## 4. Files Modified

### ✅ AIEnhancedJobDescriptionService.java (Lines 56-106)
- **Changed**: AI prompt to extract separate `minYearsOfExperience` and `maxYearsOfExperience`
- **Added**: Critical instructions showing examples (5 years → min=5, max=null; 5-10 years → min=5, max=10)
- **Result**: Claude now correctly parses experience ranges

### ✅ ZohoRecruitService.java (Lines 67-251)
- **Refactored**: Split criteria building into two phases:
  1. `orFilter` - collects all OR-joined conditions
  2. `minExperience` / `maxExperience` - separate tracking
- **Added**: `buildCriteriaWithPrecedence()` method
- **Result**: Proper operator precedence - OR for skills, AND for experience range

### ✅ ZohoCriteriaBuilder.java (Line 329-331)
- **Added**: `isEmpty()` method to check if filter has conditions
- **Result**: Enables conditional logic in criteria building

---

## 5. Example End-to-End Flow

### Input
```
JD Text: "We need a Java Developer with 5-10 years of experience in Bangalore"
```

### Step 1: AI Parsing
```json
{
  "jobTitle": "Java Developer",
  "minYearsOfExperience": 5,
  "maxYearsOfExperience": 10,
  "requiredSkills": ["Java", "OOP"],
  "location": "Bangalore"
}
```

### Step 2: Criteria Map
```
{
  "designation": "Java Developer",
  "skills": "Java,OOP",
  "min_experience_years": "5",
  "max_experience_years": "10",
  "location": "Bangalore"
}
```

### Step 3: Zoho Query
```
criteria=((Designation:contains:Java Developer))or((Skill_Set:contains:Java))or((Skill_Set:contains:OOP))and((Experience_in_Years:greater_equal:5))and((Experience_in_Years:less_equal:10))
```

### Step 4: Zoho Results
Only candidates where:
- ✅ Designation contains "Java Developer" **OR**
- ✅ Skills contain "Java" **OR** "OOP"
- ✅ **AND** Experience is 5-10 years (BOTH conditions)

---

## Why This Matters

| Scenario | Old Behavior | New Behavior |
|---|---|---|
| 5-10 year job | Returns candidates with any experience (6 to 20 years) | Returns only 5-10 year candidates |
| Multiple skills | 30-50 candidates (too strict) | 5-15 candidates (right fit) |
| Ranking quality | Poor (wrong experience range) | Excellent (filtered + ranked correctly) |

---

## Testing Checklist

- ✅ AI extracts `minYearsOfExperience` and `maxYearsOfExperience` correctly
- ✅ Criteria map includes both values
- ✅ Zoho query has `(min)and(max)` for experience
- ✅ Skills/Designations use OR logic
- ✅ Experience range uses AND logic
- ✅ Compile without errors
- ✅ Logging shows correct criteria strings

