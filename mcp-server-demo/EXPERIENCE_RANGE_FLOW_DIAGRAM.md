# Experience Range Filtering - Data Flow Diagram

## Complete End-to-End Flow

```
┌─────────────────────────────────────────────────────────────────────────────┐
│ INPUT: Raw Job Description Text                                             │
│ "Java Developer | 5-10 years | Bangalore | Skills: Java, OOP, Data Struct" │
└────────────────────────────────┬────────────────────────────────────────────┘
                                ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│ STAGE 1: AI PARSING (AIEnhancedJobDescriptionService)                        │
│                                                                               │
│ Claude Haiku receives prompt with:                                           │
│   ✅ minYearsOfExperience: number or null                                   │
│   ✅ maxYearsOfExperience: number or null                                   │
│   ✅ Instructions: "5-10 years" → min=5, max=10                            │
│                                                                               │
│ OUTPUT: JobDescription Entity                                                │
│ ┌──────────────────────────────────────────────────────────────────────────┐ │
│ │ jobTitle: "Java Developer"                                               │ │
│ │ minYearsOfExperience: 5    ← EXTRACTED FROM "5-10 years"               │ │
│ │ maxYearsOfExperience: 10   ← EXTRACTED FROM "5-10 years"               │ │
│ │ requiredSkills: ["Java", "OOP", "Data Structures"]                      │ │
│ │ location: "Bangalore"                                                    │ │
│ └──────────────────────────────────────────────────────────────────────────┘ │
└────────────────────────────────┬────────────────────────────────────────────┘
                                ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│ STAGE 2: CRITERIA MAP BUILDING (RecruitmentTools)                            │
│                                                                               │
│ buildSearchCriteria(JobDescription jd) reads:                               │
│   • jd.getJobTitle()  → "designation"                                       │
│   • jd.getRequiredSkills() → "skills"                                       │
│   • jd.getMinYearsOfExperience() → "min_experience_years"  ← KEY!          │
│   • jd.getMaxYearsOfExperience() → "max_experience_years"  ← KEY!          │
│   • jd.getLocation() → "location"                                           │
│                                                                               │
│ OUTPUT: Search Criteria Map                                                  │
│ ┌──────────────────────────────────────────────────────────────────────────┐ │
│ │ Map<String, String> {                                                    │ │
│ │   "designation": "Java Developer",                                       │ │
│ │   "skills": "Java,OOP,Data Structures",                                 │ │
│ │   "min_experience_years": "5",        ← PASSED: minYOE                 │ │
│ │   "max_experience_years": "10",       ← PASSED: maxYOE                 │ │
│ │   "location": "Bangalore"                                                │ │
│ │ }                                                                         │ │
│ └──────────────────────────────────────────────────────────────────────────┘ │
└────────────────────────────────┬────────────────────────────────────────────┘
                                ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│ STAGE 3: CRITERIA STRING BUILDING (ZohoRecruitService)                       │
│                                                                               │
│ buildCriteriaStringUsingBuilder(Map searchCriteria):                        │
│                                                                               │
│ Phase A: Build OR Filter                                                    │
│ ┌────────────────────────────────────────────────────────────────────────┐ │
│ │ orFilter.addCondition("Designation", "contains", "Java Developer")    │ │
│ │ orFilter.addSkill("Java")               ← Skills added here            │ │
│ │ orFilter.addSkill("OOP")                                               │ │
│ │ orFilter.addSkill("Data Structures")                                   │ │
│ │ orFilter.addLocation("Bangalore")                                      │ │
│ │                                                                         │ │
│ │ Result: ((Designation:contains:Java Developer))or                      │ │
│ │         ((Skill_Set:contains:Java))or                                  │ │
│ │         ((Skill_Set:contains:OOP))or                                   │ │
│ │         ((Skill_Set:contains:Data Structures))or                       │ │
│ │         ((City:equals:Bangalore))                                       │ │
│ └────────────────────────────────────────────────────────────────────────┘ │
│                                                                               │
│ Phase B: Extract Experience Range                                            │
│ ┌────────────────────────────────────────────────────────────────────────┐ │
│ │ minExperience = 5                                                       │ │
│ │ maxExperience = 10                                                      │ │
│ └────────────────────────────────────────────────────────────────────────┘ │
│                                                                               │
│ Phase C: Build Criteria with Precedence                                     │
│ ┌────────────────────────────────────────────────────────────────────────┐ │
│ │ buildCriteriaWithPrecedence(orFilter, minExp=5, maxExp=10):           │ │
│ │                                                                         │ │
│ │ Step 1: Build OR criteria from orFilter.build()                       │ │
│ │ Step 2: Build AND criteria for experience range:                      │ │
│ │         minExpCond = "(Experience_in_Years:>=5)"                      │ │
│ │         maxExpCond = "(Experience_in_Years:<=10)"                     │ │
│ │         Combined: "((Experience_in_Years:>=5)and(...<=10))"           │ │
│ │                                                                         │ │
│ │ Step 3: Join all with AND:                                            │ │
│ │         (OR_RESULT) and (EXPERIENCE_RANGE)                            │ │
│ └────────────────────────────────────────────────────────────────────────┘ │
│                                                                               │
│ FINAL OUTPUT:                                                                │
│ ((Designation:contains:Java Developer))or((Skill_Set:contains:Java))or      │
│ ((Skill_Set:contains:OOP))or((Skill_Set:contains:Data Structures))or        │
│ ((City:equals:Bangalore))                                                    │
│ and                                                                           │
│ ((Experience_in_Years:>=5)and(Experience_in_Years:<=10))                    │
└────────────────────────────────┬────────────────────────────────────────────┘
                                ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│ STAGE 4: ZOHO RECRUIT API CALL (ZohoRecruitAPIService)                       │
│                                                                               │
│ GET /recruit/v2/Candidates/search?criteria=<ABOVE>&page=1&per_page=15       │
│                                                                               │
│ FILTER LOGIC (Zoho Interpretation):                                          │
│                                                                               │
│ Candidate passes if:                                                         │
│   (Designation has "Java Developer"  OR                                      │
│    Skills have "Java"  OR                                                    │
│    Skills have "OOP"  OR                                                     │
│    Skills have "Data Structures"  OR                                         │
│    City is "Bangalore")                                                      │
│   AND                                                                         │
│   (Experience >= 5  AND  Experience <= 10)  ← BOTH conditions REQUIRED      │
│                                                                               │
│ BEFORE FIX: All conditions with AND → Very narrow (~5 results)              │
│ AFTER FIX:  OR for skills + AND for experience → Better results (~15)       │
└────────────────────────────────┬────────────────────────────────────────────┘
                                ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│ OUTPUT: Candidate List                                                        │
│                                                                               │
│ 1. John Doe          | 7 years | Java Developer    | Match: 92%             │
│ 2. Jane Smith        | 5 years | Senior Java Dev   | Match: 88%             │
│ 3. Bob Johnson       | 10 years| Java Architect    | Match: 85%             │
│ 4. Alice Williams    | 8 years | Java Developer    | Match: 83%             │
│ 5. Charlie Brown     | 6 years | Software Engineer | Match: 79%             │
│                                                                               │
│ ✅ All candidates have 5-10 years of experience                             │
│ ✅ All candidates match at least one skill/designation/location             │
│ ✅ Ranking is based on AI semantic matching, not just keywords              │
└──────────────────────────────────────────────────────────────────────────────┘
```

---

## Key Insight: Why Both Values Are Always Passed

```
┌─────────────────────────────────────────────────────────────────┐
│ When JD says "5-10 years experience"                             │
│                                                                  │
│   Claude PARSES:                                                │
│   minYearsOfExperience = 5                                      │
│   maxYearsOfExperience = 10                                     │ 
│                                                                  │
│   Both values extracted in same AI call                          │
│   Both values stored in JobDescription entity                   │
│   Both values retrieved in buildSearchCriteria()                │
│   Both values passed to Zoho query                              │
│                                                                  │
│   Result: BOTH appear in search criteria - this is CORRECT! ✅ │
└─────────────────────────────────────────────────────────────────┘
```

---

## Operator Precedence Comparison

### ❌ OLD (WRONG)
```
((Designation))and((Skill))and((Skill))and((Experience>=5))and((Experience<=10))

All AND = 0 results (too restrictive)
Only candidates with EXACT designation AND ALL skills
```

### ✅ NEW (CORRECT)
```
((Designation))or((Skill))or((Skill))
and
((Experience>=5))and((Experience<=10))

= Find candidates with ANY skill/designation
  THEN filter to 5-10 years experience
  Result: 10-20 candidates (perfect!)
```

---

## Files Modified Summary

| File | Change | Impact |
|------|--------|--------|
| `AIEnhancedJobDescriptionService.java` | AI prompt now asks for `minYearsOfExperience` AND `maxYearsOfExperience` | Claude extracts both values from experience ranges |
| `RecruitmentTools.java` | `buildSearchCriteria()` passes both min/max to search | Both values sent to criteria builder |
| `ZohoRecruitService.java` | New `buildCriteriaWithPrecedence()` method | OR for skills, AND for experience range |
| `ZohoCriteriaBuilder.java` | Added `isEmpty()` method to `CriteriaFilter` | Enables conditional logic |


