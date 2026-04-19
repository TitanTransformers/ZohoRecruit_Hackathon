# API Response Examples for Zoho Recruit Sourcing Tool

## Example 1: Array Response
The most straightforward format - array of candidate objects.

```json
[
  {
    "candidateId": "C001",
    "firstName": "John",
    "lastName": "Doe",
    "email": "john.doe@example.com",
    "phone": "+1-555-0123",
    "skills": "Java, Spring Boot, Microservices",
    "experience": "5 years",
    "matchScore": "92%"
  },
  {
    "candidateId": "C002",
    "firstName": "Jane",
    "lastName": "Smith",
    "email": "jane.smith@example.com",
    "phone": "+1-555-0456",
    "skills": "React, TypeScript, Node.js",
    "experience": "4 years",
    "matchScore": "88%"
  },
  {
    "candidateId": "C003",
    "firstName": "Michael",
    "lastName": "Johnson",
    "email": "michael.j@example.com",
    "phone": "+1-555-0789",
    "skills": "Java, SQL, AWS",
    "experience": "7 years",
    "matchScore": "85%"
  }
]
```

## Example 2: Nested Results Response
Results wrapped in a `results` property.

```json
{
  "results": [
    {
      "id": 1,
      "name": "Alice Brown",
      "designation": "Senior Developer",
      "location": "San Francisco, CA",
      "salary_expectation": "$120,000 - $150,000",
      "availability": "2 weeks",
      "match_percentage": "95%"
    },
    {
      "id": 2,
      "name": "Bob Wilson",
      "designation": "Full Stack Developer",
      "location": "New York, NY",
      "salary_expectation": "$100,000 - $130,000",
      "availability": "Immediate",
      "match_percentage": "90%"
    }
  ]
}
```

## Example 3: Nested Candidates Response
Results wrapped in a `candidates` property.

```json
{
  "candidates": [
    {
      "candidateId": "ZRC-001",
      "name": "Sarah Lee",
      "title": "Product Manager",
      "yearsOfExperience": 6,
      "currentCompany": "Tech Corp",
      "skills": ["Product Strategy", "Analytics", "Leadership"],
      "relevanceScore": 92
    },
    {
      "candidateId": "ZRC-002",
      "name": "David Kumar",
      "title": "Software Architect",
      "yearsOfExperience": 10,
      "currentCompany": "Innovation Labs",
      "skills": ["System Design", "Java", "AWS"],
      "relevanceScore": 89
    }
  ]
}
```

## Example 4: Complex Candidate Profile
With detailed information - all columns will be displayed.

```json
[
  {
    "zohoCandidateId": "Z-123456",
    "firstName": "Elizabeth",
    "lastName": "Martinez",
    "email": "elizabeth.martinez@email.com",
    "phone": "555-0000",
    "alternatePhone": "555-0001",
    "currentLocation": "Austin, TX",
    "preferredLocations": "Austin, TX; San Antonio, TX",
    "currentEmployer": "Global Tech Solutions",
    "currentJobTitle": "Senior Backend Engineer",
    "yearsInCurrentRole": 3,
    "totalWorkExperience": 8,
    "education": "BS Computer Science - University of Texas",
    "certifications": "AWS Certified Solutions Architect",
    "technicalSkills": "Java, Python, AWS, Docker, Kubernetes",
    "softSkills": "Leadership, Problem Solving, Communication",
    "matchingSkills": 12,
    "totalSkillsRequired": 13,
    "matchPercentage": 92.3,
    "availability": "2 weeks notice",
    "noticePeriodDays": 14,
    "salaryExpectation": 150000,
    "salaryExpectationCurrency": "USD",
    "openToRemote": true,
    "openToRelocation": false,
    "linkedInProfile": "linkedin.com/in/elizabeth-martinez",
    "resume": "elizabeth_martinez_resume.pdf",
    "summary": "Highly skilled backend engineer with strong experience in microservices and cloud architecture"
  }
]
```

## How the Table Works

### Column Extraction
The component automatically:
1. Takes the first result object
2. Extracts all keys: `["firstName", "lastName", "email", ...]`
3. Formats them in the header: "First Name", "Last Name", "Email", etc.

### Data Formatting
- Column names: `snake_case` → "Snake Case", `camelCase` → "Camel Case"
- Values: Truncated to 100 characters to prevent table overflow
- Empty values: Shown as "-"

### Example Table Output
```
| Candidate ID | First Name | Last Name | Email                    | Phone        | Skills                           | Experience | Match Score |
|--------------|------------|-----------|--------------------------|--------------|----------------------------------|------------|-------------|
| C001         | John       | Doe       | john.doe@example.com     | +1-555-0123  | Java, Spring Boot, Micros...     | 5 years    | 92%         |
| C002         | Jane       | Smith     | jane.smith@example.com   | +1-555-0456  | React, TypeScript, Node.js       | 4 years    | 88%         |
| C003         | Michael    | Johnson   | michael.j@example.com    | +1-555-0789  | Java, SQL, AWS                   | 7 years    | 85%         |
```

## No Results Response
If the API returns an empty array or empty results property:

```json
[]
```

Or:

```json
{
  "results": []
}
```

The UI will display: "No candidate profiles found matching the job description"

## Error Response
If something goes wrong:

```json
{
  "error": "Invalid job description",
  "message": "Unable to search profiles"
}
```

The UI will show the error message from the error alert component.

## Testing the Integration

### Using cURL for Testing
```bash
# Test with JSON payload
curl -X POST http://localhost:8081/api/documents/process \
  -H "Content-Type: application/json" \
  -d '{"message":"Senior Java Developer needed for microservices project. 5+ years experience required."}'

# Test with FormData (PDF)
curl -X POST http://localhost:8081/api/documents/process \
  -F "pdf=@job_description.pdf"

# Test with both text and PDF
curl -X POST http://localhost:8081/api/documents/process \
  -F "message=Senior Java Developer" \
  -F "pdf=@job_description.pdf"
```

## Implementation Notes

✅ **Dynamic Columns**: The table works with ANY JSON structure returned from the API
✅ **Flexible Response**: Accepts direct array, nested in "results", or nested in "candidates"
✅ **Data Safety**: Long values are truncated to prevent UI overflow
✅ **User-Friendly**: Column headers are automatically formatted from key names
✅ **Type-Safe**: TypeScript Record<string, any>[] ensures type safety while allowing flexibility
