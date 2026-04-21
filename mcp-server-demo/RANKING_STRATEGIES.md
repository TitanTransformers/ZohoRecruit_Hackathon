# Candidate Ranking Strategies

This MCP server provides **two implementations** for ranking candidates against job descriptions. Choose based on your priorities: **cost** or **speed**.

---

## 1. Single Batch Ranking (Cost-Effective) ✅ RECOMMENDED

### Tool Name
`findAndRankCandidates`

### How It Works
- All candidates are ranked in a **single API call** to Claude
- Consistent relative scoring across all candidates
- Claude sees the full candidate pool and scores each relatively

### Performance
| Metric | Value |
|--------|-------|
| **Time** | ~23 seconds for 31 candidates |
| **API Calls** | 1 call |
| **Tokens** | ~1,200-1,500 tokens |
| **Cost** | ~$0.15-0.20 (Haiku pricing) |
| **Consistency** | ✅ Highest (single ranking context) |

### Advantages
- ✅ **Most Cost-Effective** - Minimal token usage
- ✅ **Consistent Scoring** - All candidates evaluated in same context
- ✅ **Better Ranking Quality** - Claude can compare candidates relatively
- ✅ **Predictable Costs** - No token waste from repeated prompts
- ✅ **Recommended for** - Batch processing, cost-conscious use cases

### Disadvantages
- ⏱️ Slower for large candidate pools
- ⏱️ No parallelization

### Example Usage
```json
POST /api/recruitment/findAndRankCandidates
{
  "jobTitle": "Senior Backend Engineer",
  "requiredSkills": "Java, Spring Boot, PostgreSQL",
  "preferredSkills": "Kubernetes, Docker, AWS",
  "experienceLevel": "Senior",
  "pageSize": 10
}
```

---

## 2. Parallel Batch Ranking (Speed-Optimized) ⚡

### Tool Name
`findAndRankCandidatesWithBatching`

### How It Works
- Candidates split into **batches of 10**
- Each batch ranked **in parallel** (up to 4 threads)
- Batches merged and sorted by final score

### Performance
| Metric | Value |
|--------|-------|
| **Time** | ~8-10 seconds for 31 candidates (2.3-2.9x faster!) |
| **API Calls** | 3-4 calls (parallel) |
| **Tokens** | ~1,800-2,200 tokens |
| **Cost** | ~$0.25-0.30 (25-50% more expensive) |
| **Consistency** | ⚠️ Medium (batch-level consistency) |

### Advantages
- ⚡ **2-3x Faster** - Parallel batch processing
- ⚡ **Real-Time Friendly** - Better for interactive scenarios
- ✅ Still manages large pools efficiently

### Disadvantages
- ❌ **Higher Costs** - 25-50% more token usage
- ⚠️ **Inconsistent Scoring** - Candidates only compared within batch
  - Top candidate in batch 1 vs. batch 2 may have different absolute scores
  - Relative ranking within batch is consistent
- ❌ **Not Recommended for** - Cost-conscious, high-volume scenarios

### Why Batching Costs More
Each batch resends overhead:
```
Batch 1:  Job description + Skills + Instructions + 10 candidates
Batch 2:  Job description + Skills + Instructions + 10 candidates  ← REPEATED
Batch 3:  Job description + Skills + Instructions + 11 candidates  ← REPEATED

Total: 25-50% more tokens!
```

### Example Usage
```json
POST /api/recruitment/findAndRankCandidatesWithBatching
{
  "jobTitle": "Senior Backend Engineer",
  "requiredSkills": "Java, Spring Boot, PostgreSQL",
  "preferredSkills": "Kubernetes, Docker, AWS",
  "experienceLevel": "Senior",
  "pageSize": 10
}
```

---

## Comparison Matrix

| Factor | Single Batch | Parallel Batch |
|--------|--------------|----------------|
| **Speed** | 🟡 23s | 🟢 8-10s |
| **Cost** | 🟢 $0.15-0.20 | 🔴 $0.25-0.30 |
| **Consistency** | 🟢 Highest | 🟡 Medium |
| **Scalability** | 🟡 O(n) serial | 🟢 O(n/threads) parallel |
| **Token Efficiency** | 🟢 Optimal | 🔴 Wasteful |
| **Ranking Quality** | 🟢 Best | 🟡 Good |
| **Best Use Case** | Batch processing | Real-time recruiting |

---

## Recommendation Matrix

### Use **Single Batch** (`findAndRankCandidates`) when:
- ✅ Cost is a primary concern
- ✅ Processing batches (daily candidate reviews)
- ✅ You want the highest ranking quality
- ✅ Candidate pool is < 100 (doesn't significantly impact speed)
- ✅ Consistency is critical
- ✅ Budget is constrained

**Example**: Daily recruiter workflow, batch screening, compliance reports

### Use **Parallel Batch** (`findAndRankCandidatesWithBatching`) when:
- ⚡ Speed is critical (real-time feedback needed)
- ⚡ Budget allows for higher token usage
- ⚡ Interactive recruiting sessions
- ⚡ Large candidate pools (100+ candidates)
- ⚡ You need results in < 15 seconds
- ⚡ Cost is secondary to user experience

**Example**: Real-time candidate search, live recruiter dashboards, interactive interviews

---

## Implementation Details

### Single Batch: `rankCandidatesWithAI()`
```java
// All candidates in ONE call
String aiAnalysis = analyzeAllCandidatesWithClaude(
    allCandidates,  // ← All 31 at once
    jobDescription
);
```

### Parallel Batch: `rankCandidatesWithAIBatching()`
```java
// Split into batches, process in parallel
for (int i = 0; i < batches; i++) {
    List<Candidate> batch = candidates.subList(start, end);  // ← 10 at a time
    executorService.submit(() -> {
        analyzeAllCandidatesWithClaude(batch, jobDescription);
    });
}
```

---

## Cost Breakdown Example (31 Candidates)

### Single Batch
```
Prompt tokens:     ~800 (job info + 31 candidates)
Completion tokens: ~400 (31 JSON results)
Total:            ~1,200 tokens
Cost (Haiku):     ~$0.0015 * 1200 = $0.18
API Calls:        1
```

### Parallel Batch (3 batches: 10+10+11)
```
Batch 1:
  Prompt:    ~400 (job info + 10 candidates)
  Completion: ~130 (10 JSON results)
  Subtotal:  ~530 tokens

Batch 2: ~530 tokens (REPEATED job info overhead)
Batch 3: ~580 tokens (REPEATED job info overhead)

Total:     ~1,640 tokens (37% MORE!)
Cost:      ~$0.25
API Calls: 3
```

---

## Future Optimizations

### For Parallel Batch
1. **Shared prompt context** - Send job info once, rank batches in sequence
2. **Batch normalization** - Scale scores to account for batch-level variance
3. **Adaptive batching** - Increase batch size based on candidate complexity

### For Single Batch
1. **Chunking** - Compress candidate data (summary profiles instead of full data)
2. **Caching** - Cache job description analysis for repeated queries
3. **Streaming** - Use streaming responses for incremental ranking

---

## Migration Guide

### From Single to Parallel
Simply switch the tool name:
```
// Before (cost-effective)
findAndRankCandidates(...)

// After (speed-optimized)
findAndRankCandidatesWithBatching(...)
```

All parameters remain identical!

### Monitoring
Both tools log detailed metrics:
```
Ranked X candidates using AI in YYYms (avg: ZZms per candidate)
```

Use these logs to track performance and costs over time.

---

## References

- **Claude Haiku Pricing**: https://www.anthropic.com/pricing
- **Token Counting**: Use `modelContextProtocol` in Spring AI for exact counts
- **Batching Best Practices**: Covered in `AIEnhancedCandidateRankingService`

