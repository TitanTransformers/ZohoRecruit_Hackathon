# Experience Range Fix - Code Review & Corrections

**Date:** April 21, 2026  
**Status:** ✅ FIXED

---

## Problem Summary

The recruitment pipeline was **NOT correctly applying experience range filters** (min/max years) when fetching candidates from Zoho Recruit. This resulted in:

- ❌ Experience range constraints ignored during search
- ❌ Candidates outside the desired range (both older and newer) being included
- ❌ Incorrect ranking since all candidates >= min_years were treated equally
- ❌ Example: Job requiring "5-10 years" would fetch candidates with 5, 15, or 25 years equally

---

## Root Causes Identified

### 1. **Missing Experience Range Parsing in AI Service**
**File:** `AIEnhancedJobDescriptionService.java` (Line 56-96)

**Issue:**
- AI prompt asked only for `yearsOfExperience` (single number)
- No support for experience **ranges** (min/max)
- Cannot extract "5-10 years experience" pattern

**Original Prompt Field:**
```json
"yearsOfExperience": number or null,
```

**Fix:**
Changed to support min/max separately:
```json
"minYearsOfExperience": number or null,
"maxYearsOfExperience": number or null,
```

Added explicit instructions:
```
IMPORTANT - Experience Range Extraction:
- Extract BOTH minimum and maximum years if a range is specified (e.g., "5-10 years" → min: 5, max: 10)
- If only one number is given with "years", use it as BOTH min and max
- If experience level is given (Junior/Mid/Senior), map it: Junior=0, Mid=3, Senior=7, Lead=10
- Priority: Explicit year range > Explicit single year > Experience level
```

---

### 2. **Missing Search Criteria Mapping in RecruitmentTools**
**File:** `RecruitmentTools.java` (Line 254-269)

**Issue:**
- `buildSearchCriteria()` method never passed `minYearsOfExperience` or `maxYearsOfExperience`
- Only relied on generic `experience_level` field
- Parsed JobDescription had these fields but they were ignored

**Before:**
```java
private Map<String, String> buildSearchCriteria(JobDescription jd) {
    Map<String, String> criteria = new LinkedHashMap<>();
    // ... only experience_level was handled, NOT min/max years
    if (jd.getExperienceLevel() != null && !jd.getExperienceLevel().isBlank()) {
        criteria.put("experience_level", jd.getExperienceLevel());
    }
    return criteria;
}
```

**After:**
```java
private Map<String, String> buildSearchCriteria(JobDescription jd) {
    Map<String, String> criteria = new LinkedHashMap<>();
    // ... existing code ...
    
    // NEW: Add minimum years of experience constraint
    if (jd.getMinYearsOfExperience() != null) {
        criteria.put("min_experience_years", String.valueOf(jd.getMinYearsOfExperience()));
        log.debug("Added min experience constraint: {} years", jd.getMinYearsOfExperience());
    }
    
    // NEW: Add maximum years of experience constraint
    if (jd.getMaxYearsOfExperience() != null) {
        criteria.put("max_experience_years", String.valueOf(jd.getMaxYearsOfExperience()));
        log.debug("Added max experience constraint: {} years", jd.getMaxYearsOfExperience());
    }
    
    return criteria;
}
```

---

### 3. **Incomplete Criteria Builder in ZohoRecruitService**
**File:** `ZohoRecruitService.java` (Line 67-168)

**Issue:**
- `buildCriteriaStringUsingBuilder()` had NO handling for min/max experience
- Only generic `experience_years` was supported (single value with GREATER_EQUAL operator)
- Missing implementation of separate min and max bounds

**Before:**
```java
case "experience_years", "experience_in_years", "years_of_experience" -> {
    try {
        filter.addExperience(Integer.parseInt(value));  // Only adds minimum bound
    } catch (NumberFormatException e) {
        // ...
    }
}
```

**After:**
```java
// NEW: Handle experience range - minimum years
case "min_experience_years", "min_years_of_experience", "min_experience" -> {
    try {
        Integer minYears = Integer.parseInt(value);
        log.debug("Adding min experience constraint: {} years", minYears);
        filter.addCondition(ZohoRecruitCandidateSearchField.EXPERIENCE_IN_YEARS,
                ZohoCriteriaBuilder.Operator.GREATER_EQUAL, minYears);
    } catch (NumberFormatException e) {
        log.warn("Invalid min experience value (must be numeric): {}. Skipping.", value);
    }
}

// NEW: Handle experience range - maximum years
case "max_experience_years", "max_years_of_experience", "max_experience" -> {
    try {
        Integer maxYears = Integer.parseInt(value);
        log.debug("Adding max experience constraint: {} years", maxYears);
        filter.addCondition(ZohoRecruitCandidateSearchField.EXPERIENCE_IN_YEARS,
                ZohoCriteriaBuilder.Operator.LESS_EQUAL, maxYears);
    } catch (NumberFormatException e) {
        log.warn("Invalid max experience value (must be numeric): {}. Skipping.", value);
    }
}
```

---

## Data Flow After Fix

### Example: Job Description with "5-10 years experience"

```
1. JD PARSING (AIEnhancedJobDescriptionService)
   Input: "We need a developer with 5-10 years of experience..."
   ↓
   Claude Extracts:
   - minYearsOfExperience: 5
   - maxYearsOfExperience: 10
   ↓
   JobDescription object created with:
   - minYearsOfExperience = 5
   - maxYearsOfExperience = 10

2. SEARCH CRITERIA BUILDING (RecruitmentTools.buildSearchCriteria)
   Input: JobDescription with min=5, max=10
   ↓
   Creates search criteria map:
   - min_experience_years: "5"
   - max_experience_years: "10"
   ↓
   Passed to ZohoRecruitService

3. ZOHO RECRUIT CRITERIA BUILDING (ZohoRecruitService)
   Input: min_experience_years=5, max_experience_years=10
   ↓
   CriteriaFilter creates:
   - Condition 1: (Experience_in_Years:greater_equal:5)
   - Condition 2: (Experience_in_Years:less_equal:10)
   ↓
   Final Zoho Query:
   ((Experience_in_Years:greater_equal:5)and(Experience_in_Years:less_equal:10))

4. ZOHO RECRUIT API CALL (ZohoRecruitAPIService)
   Only candidates with 5 ≤ experience ≤ 10 years are returned
   ✅ Correct filtering!
```

---

## Files Modified

| File | Changes | Lines |
|------|---------|-------|
| `AIEnhancedJobDescriptionService.java` | Updated AI prompt to extract min/max years | 56-107 |
| `RecruitmentTools.java` | Added min/max experience to search criteria | 254-275 |
| `ZohoRecruitService.java` | Added handlers for min/max experience cases | 67-168 |

---

## Backward Compatibility

✅ **Fully Backward Compatible:**
- Old `experience_years` key still works (single minimum bound)
- New `min_experience_years` and `max_experience_years` keys work alongside
- If max_years not provided, only minimum bound is applied
- If min_years not provided, only maximum bound is applied
- If both provided, both bounds are applied (AND logic)

---

## Testing Recommendations

### Test Case 1: Simple Minimum Years
**Input:** Job requires "5+ years"
**Expected:** Only candidates with ≥ 5 years returned

### Test Case 2: Full Range
**Input:** Job requires "5-10 years"
**Expected:** Only candidates with 5 ≤ years ≤ 10 returned
**Reject:** Candidates with 3 years (too junior) or 15 years (too senior)

### Test Case 3: Maximum Only
**Input:** Job requires "Up to 7 years max (junior role)"
**Expected:** Only candidates with ≤ 7 years returned

### Test Case 4: Experience Level Fallback
**Input:** Job just says "Senior" level
**Expected:** Claude extracts experienceLevel="Senior", maps to min=7 years

### Test Case 5: Multiple Conditions
**Input:** Job requires "5-10 years Java"
**Expected:** Zoho query with 3 AND conditions:
1. Experience_in_Years >= 5
2. Experience_in_Years <= 10
3. Skill_Set contains Java

---

## Key Improvements

| Aspect | Before | After |
|--------|--------|-------|
| **Experience Range Support** | ❌ No | ✅ Yes (min & max) |
| **Filtering Accuracy** | ❌ Too loose | ✅ Precise range filters |
| **Candidate Selection** | ❌ Over-inclusive | ✅ Exact match |
| **Ranking Quality** | ❌ Poor (all same exp.) | ✅ Better (true range match) |
| **Search Criteria** | Single bound | Dual bounds (AND) |
| **Logging** | No experience logging | ✅ Detailed debug logs |

---

## Verification

✅ Code compiles without errors
✅ All three files validated
✅ No breaking changes
✅ Logging added for debugging
✅ Follows project conventions (Lombok, Spring Boot, CriteriaFilter)
✅ Matches instruction guidelines (type-safe, enum-based operators)

---

## Related Code Areas

- **ZohoCriteriaBuilder.java** - Already supports LESS_EQUAL operator ✓
- **ZohoRecruitCandidateSearchField.java** - EXPERIENCE_IN_YEARS marked as NUMERIC ✓
- **Candidate.java** - Entity already has experience fields ✓
- **JobDescription.java** - Already has minYearsOfExperience & maxYearsOfExperience ✓

All supporting infrastructure was already in place; this fix just wires it all together correctly.

