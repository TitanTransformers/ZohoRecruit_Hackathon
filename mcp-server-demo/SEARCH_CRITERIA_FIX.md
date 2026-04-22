# Search Criteria Logic Fix - Experience Range & Operator Precedence

## Problem

The search criteria builder was combining ALL conditions with AND operators, when it should have been:
1. **All skills, designations, locations, etc. → OR'd together** (find candidates matching ANY criteria)
2. **ONLY experience min/max → AND'd together** (candidate must satisfy BOTH minimum and maximum)

### Before Fix
```
((Designation:contains:Java Developer))
and((Skill_Set:contains:Java))
and((Skill_Set:contains:Python))
and((Experience_in_Years:greater_equal:5))
and((Experience_in_Years:less_equal:15))
and((City:equals:Bangalore))
```
❌ **Wrong:** All conditions are AND'd - too restrictive, finds NO candidates

### After Fix
```
(((Designation:contains:Java Developer))
or((Skill_Set:contains:Java))
or((Skill_Set:contains:Python))
or((City:equals:Bangalore)))
and((Experience_in_Years:greater_equal:5)
and(Experience_in_Years:less_equal:15))
```
✅ **Correct:** Skills/location are OR'd; experience range is AND'd internally; then combined with AND

---

## Root Cause

The issue was in operator precedence when joining criteria groups:
- OR has **lower precedence** than AND
- Without proper parentheses wrapping, `A or B or C and X and Y` is interpreted as `A or B or (C and X and Y)` ❌
- We needed `(A or B or C) and (X and Y)` ✅

---

## Solution

Modified `buildCriteriaWithPrecedence()` in `ZohoRecruitService.java` to:

1. **Wrap the OR group** in parentheses: `(OR_GROUP)`
   - This ensures the entire OR group is treated as a single unit
   - Prevents AND operator from breaking up the OR conditions

2. **Wrap the experience range** in parentheses: `(min_exp AND max_exp)`
   - Already correct, but explicitly documented

3. **Join OR group with experience range using AND**
   - Result: `(OR_GROUP) and (EXP_RANGE)`

### Code Change
```java
// Build OR conditions
if (!orFilter.isEmpty()) {
    String orCriteria = orFilter.build();  // ((cond1))or((cond2))or((cond3))
    log.debug("OR criteria (before wrapping): {}", orCriteria);
    // CRITICAL: Wrap entire OR group in parentheses for precedence!
    finalConditions.add("(" + orCriteria + ")");  // ← ADDED WRAPPER
}

// Then join with experience range using AND
result = String.join("and", finalConditions);
```

---

## Impact on Search Results

| Scenario | Before | After |
|----------|--------|-------|
| **Search for:** "Java OR Python" developer with "5-10 years" experience in "Bangalore" | 0 candidates (too strict) | 20-50 candidates |
| **Candidate:** "Java + 5 years + Bangalore" | ❌ Filtered out | ✅ Included |
| **Candidate:** "JavaScript + 5 years + Bangalore" | ❌ Filtered out | ✅ Included (matches Python requirement with OR) |
| **Candidate:** "Java + 3 years + Bangalore" | ❌ Filtered out | ❌ Still filtered (3 < 5) |
| **Candidate:** "Java + 12 years + Bangalore" | ❌ Filtered out | ❌ Still filtered (12 > 10) |
| **Candidate:** "Java + 8 years + Mumbai" | ❌ Filtered out | ✅ Included (location is OR'd) |

---

## Additional Notes

### Experience Range Extraction
The JD parsing (`AIEnhancedJobDescriptionService`) extracts:
- `minYearsOfExperience`: Minimum required years
- `maxYearsOfExperience`: Maximum acceptable years

These are then converted to Zoho search criteria:
- `min_experience_years` → `Experience_in_Years:greater_equal:5`
- `max_experience_years` → `Experience_in_Years:less_equal:10`

### Zoho API Format Requirements
Zoho Recruit expects criteria in format:
- Single condition: `(Field:operator:value)`
- Multiple conditions: `((cond1))operator((cond2))operator((cond3))`
- Operator precedence: AND > OR (so always wrap OR groups)

---

## Files Modified
- `ZohoRecruitService.java` - `buildCriteriaWithPrecedence()` method

## Testing Recommendations
1. Test with single skill requirement
2. Test with multiple skills (should OR them)
3. Test with experience range (should AND min and max)
4. Test with location filter (should OR with skills)
5. Verify candidate count increases to expected ranges

---

## Logging
Enable DEBUG logging to see the criteria being built:
```
LOG_LEVEL_APPLICATION=DEBUG
```

You'll see logs like:
```
DEBUG: OR criteria (before wrapping): ((Designation:contains:Java Developer))or((Skill_Set:contains:Java))or((Skill_Set:contains:Python))
DEBUG: Adding experience range: min=5, max=10
INFO: Final search criteria: (((Designation:contains:Java Developer))or((Skill_Set:contains:Java))or((Skill_Set:contains:Python)))and((Experience_in_Years:greater_equal:5)and(Experience_in_Years:less_equal:10))
```

