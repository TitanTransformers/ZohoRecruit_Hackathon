# Summary: Why maxYOE Appears with minYOE

## The Short Answer

Both `minYearsOfExperience` and `maxYearsOfExperience` are **extracted together by Claude AI** from the job description in a single parsing step. That's why you see both values appearing in the search criteria.

---

## Root Cause: Prompt Mismatch (FIXED ✅)

### Before Fix ❌
```java
// IN: AIEnhancedJobDescriptionService.java (WRONG)
"yearsOfExperience": number or null,  // Only ONE field

// OUT: ZohoRecruitService.java (WRONG)
.minYearsOfExperience(getIntValue(jsonNode, "minYearsOfExperience"))  // ← Reading non-existent field!
.maxYearsOfExperience(getIntValue(jsonNode, "maxYearsOfExperience"))  // ← Reading non-existent field!
```

**Problem**: The code was trying to read `min` and `max` that Claude was never asked to extract!

### After Fix ✅
```java
// IN: AIEnhancedJobDescriptionService.java (CORRECT)
"minYearsOfExperience": number or null,
"maxYearsOfExperience": number or null,

// OUT: ZohoRecruitService.java (CORRECT)
.minYearsOfExperience(getIntValue(jsonNode, "minYearsOfExperience"))  // ✅ Claude provides this
.maxYearsOfExperience(getIntValue(jsonNode, "maxYearsOfExperience"))  // ✅ Claude provides this
```

---

## The Fix (1 File Modified)

### AIEnhancedJobDescriptionService.java (Lines 56-106)

**Changed:**
```diff
- "yearsOfExperience": number or null,
+ "minYearsOfExperience": number or null,
+ "maxYearsOfExperience": number or null,
```

**Added Instructions:**
```
CRITICAL INSTRUCTIONS FOR EXPERIENCE EXTRACTION:
- If JD says "5 years experience": minYearsOfExperience=5, maxYearsOfExperience=null
- If JD says "5-10 years": minYearsOfExperience=5, maxYearsOfExperience=10
- If JD says "3+ years": minYearsOfExperience=3, maxYearsOfExperience=null
- If JD says "2-5 years or equivalent": minYearsOfExperience=2, maxYearsOfExperience=5
```

---

## Data Flow Now Works Correctly

```
JD Text: "5-10 years"
    ↓
Claude AI extracts (because we asked):
    minYearsOfExperience = 5
    maxYearsOfExperience = 10
    ↓
buildSearchCriteria() reads both:
    criteria.put("min_experience_years", "5")
    criteria.put("max_experience_years", "10")
    ↓
buildCriteriaWithPrecedence() builds both:
    "(Experience_in_Years:>=5)and(Experience_in_Years:<=10)"
    ↓
Zoho API query filters correctly:
    Only candidates with 5-10 years experience ✅
```

---

## Examples Now Working

| JD Says | minYOE | maxYOE | Zoho Query | Result |
|---------|--------|--------|-----------|--------|
| "5 years" | 5 | null | `>=5` | Candidates with 5+ years |
| "5-10 years" | 5 | 10 | `>=5 AND <=10` | Candidates with 5-10 years ✅ |
| "3+ years" | 3 | null | `>=3` | Candidates with 3+ years |
| "2-5 years" | 2 | 5 | `>=2 AND <=5` | Candidates with 2-5 years ✅ |

---

## Verification

Now when you see:
```
search?criteria=((Skill_Set:contains:Java))and((Experience_in_Years:greater_equal:5))and((Experience_in_Years:less_equal:10))&page=1
```

You understand:
- ✅ `Expression_in_Years:greater_equal:5` comes from `minYearsOfExperience: 5` (extracted by AI)
- ✅ `Experience_in_Years:less_equal:10` comes from `maxYearsOfExperience: 10` (extracted by AI)
- ✅ Both extracted in the SAME AI parsing call as the job description
- ✅ Both passed through the search criteria map
- ✅ Both used to build the Zoho query

---

## Why This is Better

### Before
- AI only extracted single `yearsOfExperience` value (or null)
- Missing "5-10 years" ranges entirely
- Zoho returned candidates across ALL experience levels
- Ranking included wrong experience candidates (noise)

### After
- AI extracts BOTH min and max from ranges
- Handles all experience definitions: "5 years", "5-10 years", "3+ years", "2-5 years"
- Zoho filters to exact range: `min <= experience <= max`
- Ranking only processes candidates in correct experience band (signal)


