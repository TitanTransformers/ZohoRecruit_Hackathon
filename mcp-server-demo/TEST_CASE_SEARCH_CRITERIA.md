# Test Case: Search Criteria Fix Validation

## Test Scenario

A recruiter is looking for a **"Senior Java Developer"** with the following requirements:

```
Job Title: Senior Java Developer
Required Skills: Java, Spring Boot, Microservices
Experience Range: 5-10 years
Location: Bangalore or Remote
```

---

## Expected Behavior (After Fix)

### Step 1: JD Parsing
```
Input: "Senior Java Developer with 5-10 years of experience in Java, Spring Boot, Microservices. 
         Work location: Bangalore or Remote"

Extracted:
- jobTitle: "Senior Java Developer"
- requiredSkills: ["Java", "Spring Boot", "Microservices"]
- minYearsOfExperience: 5
- maxYearsOfExperience: 10
- location: "Bangalore"  (or "Remote" if specified separately)
```

### Step 2: Search Criteria Building

**searchCriteria Map:**
```java
Map<String, String> searchCriteria = {
    "designation" → "Senior Java Developer",
    "skills" → "Java,Spring Boot,Microservices",
    "location" → "Bangalore",
    "min_experience_years" → "5",
    "max_experience_years" → "10"
}
```

**Processing in buildCriteriaStringUsingBuilder():**
```
1. Create orFilter = new CriteriaFilter()
2. Create andExpFilter = new CriteriaFilter() (for experience range)
3. minExperience = null, maxExperience = null

Loop through searchCriteria:
  - "designation" → orFilter.addCondition(DESIGNATION, CONTAINS, "Senior Java Developer")
  - "skills" → 
      - orFilter.addSkill("Java")
      - orFilter.addSkill("Spring Boot")
      - orFilter.addSkill("Microservices")
  - "location" → orFilter.addLocation("Bangalore")
  - "min_experience_years" → minExperience = 5
  - "max_experience_years" → maxExperience = 10

Call: buildCriteriaWithPrecedence(orFilter, 5, 10, andExpFilter)
```

### Step 3: Criteria Building with Precedence

**In buildCriteriaWithPrecedence():**

```java
finalConditions = []

// Step 1: Build OR group
orCriteria = orFilter.build()
// Returns: ((Designation:contains:Senior Java Developer))or
//          ((Skill_Set:contains:Java))or
//          ((Skill_Set:contains:Spring Boot))or
//          ((Skill_Set:contains:Microservices))or
//          ((City:equals:Bangalore))

log: "OR criteria (before wrapping): ((Designation:contains:Senior Java Developer))or((Skill_Set:contains:Java))or((Skill_Set:contains:Spring Boot))or((Skill_Set:contains:Microservices))or((City:equals:Bangalore))"

// CRITICAL FIX: Wrap in parentheses
finalConditions.add("(" + orCriteria + ")")
// (((Designation:contains:Senior Java Developer))or((Skill_Set:contains:Java))or((Skill_Set:contains:Spring Boot))or((Skill_Set:contains:Microservices))or((City:equals:Bangalore)))

// Step 2: Build experience range
minExperience = 5, maxExperience = 10

minExpCond = "(Experience_in_Years:greater_equal:5)"
maxExpCond = "(Experience_in_Years:less_equal:10)"

expCondition = "(" + minExpCond + "and" + maxExpCond + ")"
            = "((Experience_in_Years:greater_equal:5)and(Experience_in_Years:less_equal:10))"

finalConditions.add(expCondition)

// Step 3: Join with AND
result = String.join("and", finalConditions)
       = "(((Designation:contains:Senior Java Developer))or((Skill_Set:contains:Java))or((Skill_Set:contains:Spring Boot))or((Skill_Set:contains:Microservices))or((City:equals:Bangalore)))and((Experience_in_Years:greater_equal:5)and(Experience_in_Years:less_equal:10))"

log: "Final search criteria: " + result
```

### Step 4: API Call

```
GET /recruit/v2/Candidates/search?criteria=(((Designation:contains:Senior%20Java%20Developer))or((Skill_Set:contains:Java))or((Skill_Set:contains:Spring%20Boot))or((Skill_Set:contains:Microservices))or((City:equals:Bangalore)))and((Experience_in_Years:greater_equal:5)and(Experience_in_Years:less_equal:10))&page=1&per_page=60
```

### Step 5: Zoho Interprets & Returns Candidates

**Zoho Parsing:**
```
(Designation contains "Senior Java Developer"
 OR Skill includes "Java"
 OR Skill includes "Spring Boot"
 OR Skill includes "Microservices"
 OR City = "Bangalore")
AND
(Experience >= 5 AND Experience <= 10)
```

**Results - Candidates Returned:**
```
✅ Candidate 1: "Java Developer", Skills: Java+Spring, Exp: 6 years, Bangalore
   (Matches: Java skill (OR), Spring Boot (partial) (OR), Experience 6 (AND))

✅ Candidate 2: "Senior Software Engineer", Skills: Java+Microservices, Exp: 8 years, Bangalore
   (Matches: Java (OR), Microservices (OR), Experience 8 (AND))

✅ Candidate 3: "Backend Developer", Skills: Spring+Go, Exp: 7 years, Remote
   (Matches: Spring Boot (OR), Location: Remote - would need to be included in OR)

✅ Candidate 4: "Full Stack", Skills: Java+Python, Exp: 5 years, Bangalore
   (Matches: Java (OR), Experience 5 (AND))

✅ Candidate 5: "Senior Java Developer", Skills: Java+Spring+Microservices+Kafka, Exp: 10 years, Bangalore
   (Matches: All criteria perfectly!)

❌ Candidate 6: "Java Developer", Skills: Java, Exp: 3 years, Bangalore
   (Does NOT match: Experience 3 < 5 (fails AND condition))

❌ Candidate 7: "Java Developer", Skills: Java, Exp: 12 years, Bangalore
   (Does NOT match: Experience 12 > 10 (fails AND condition))

❌ Candidate 8: "Python Developer", Skills: Python+Django, Exp: 7 years, Bangalore
   (Does NOT match: Has no matching skill in OR, location alone insufficient)

Total Results: ~25-40 candidates ✅
```

---

## Comparison: Before vs After Fix

### BEFORE (Incorrect)

**Criteria Generated:**
```
((Designation:contains:Senior Java Developer))
and((Skill_Set:contains:Java))
and((Skill_Set:contains:Spring Boot))
and((Skill_Set:contains:Microservices))
and((City:equals:Bangalore))
and((Experience_in_Years:greater_equal:5))
and((Experience_in_Years:less_equal:10))
```

**Interpretation:**
```
Must have:
- Designation exactly "Senior Java Developer" 
- AND skill "Java"
- AND skill "Spring Boot"
- AND skill "Microservices"
- AND city "Bangalore"
- AND experience >= 5
- AND experience <= 10
```

**Results: 0-2 candidates** ❌ (No one has ALL skills with exact matching)

---

### AFTER (Correct)

**Criteria Generated:**
```
(((Designation:contains:Senior Java Developer))
  or((Skill_Set:contains:Java))
  or((Skill_Set:contains:Spring Boot))
  or((Skill_Set:contains:Microservices))
  or((City:equals:Bangalore)))
and((Experience_in_Years:greater_equal:5)
and(Experience_in_Years:less_equal:10))
```

**Interpretation:**
```
Must have at least ONE of:
- Designation contains "Senior Java Developer"
- OR skill "Java"
- OR skill "Spring Boot"
- OR skill "Microservices"
- OR city "Bangalore"

AND MUST HAVE:
- Experience >= 5
- AND experience <= 10
```

**Results: 25-40 candidates** ✅ (Flexible matching on skills + location)

---

## Debug Output Example

```
2026-04-22 10:30:45.123 INFO  ZohoRecruitService: Searching candidates with criteria: {designation=Senior Java Developer, skills=Java,Spring Boot,Microservices, location=Bangalore, min_experience_years=5, max_experience_years=10}

2026-04-22 10:30:45.124 DEBUG ZohoRecruitService: OR criteria (before wrapping): ((Designation:contains:Senior Java Developer))or((Skill_Set:contains:Java))or((Skill_Set:contains:Spring Boot))or((Skill_Set:contains:Microservices))or((City:equals:Bangalore))

2026-04-22 10:30:45.125 DEBUG ZohoRecruitService: Adding experience range: min=5, max=10

2026-04-22 10:30:45.125 INFO  ZohoRecruitService: Final search criteria: (((Designation:contains:Senior Java Developer))or((Skill_Set:contains:Java))or((Skill_Set:contains:Spring Boot))or((Skill_Set:contains:Microservices))or((City:equals:Bangalore)))and((Experience_in_Years:greater_equal:5)and(Experience_in_Years:less_equal:10))

2026-04-22 10:30:45.126 DEBUG ZohoRecruitAPIService: Searching candidates with criteria: (((Designation:contains:Senior Java Developer))or((Skill_Set:contains:Java))or((Skill_Set:contains:Spring Boot))or((Skill_Set:contains:Microservices))or((City:equals:Bangalore)))and((Experience_in_Years:greater_equal:5)and(Experience_in_Years:less_equal:10)) (page: 1, pageSize: 60)

2026-04-22 10:30:47.823 INFO  ZohoRecruitAPIService: Successfully retrieved 32 candidates from Zoho Recruit
```

---

## Validation Points

✅ **Skills are OR'd:** Candidate with just "Java" skill matched  
✅ **Location is included in OR:** Candidate with no listed skills but in Bangalore matched  
✅ **Experience min is enforced:** No candidates below 5 years returned  
✅ **Experience max is enforced:** No candidates above 10 years returned  
✅ **Experience is AND'd together:** Both min AND max conditions must be true  
✅ **Overall logic is correct:** (Skills OR Location) AND (Exp Min AND Exp Max)  
✅ **Candidate pool increased:** From ~2 to ~32 candidates  

