# Development APIs

## Student Plans

### List student plans with filters + pagination
`GET /api/development/plans`

Query params:
- `subjectId` (optional) UUID
- `classId` (optional) UUID
- `classSubjectId` (optional) UUID
- `status` (optional) `active | completed | on_hold | cancelled`
- `page` (optional, default `0`)
- `size` (optional, default `20`, max `200`)

Response (200):
```json
{
  "items": [
    {
      "id": "...",
      "student": "...",
      "plan": {
        "id": "...",
        "name": "...",
        "description": "...",
        "progress": 0,
        "potentialOverall": 0,
        "eta": 30,
        "performance": "Average",
        "skills": [],
        "steps": [],
        "subjectId": "...",
        "createdAt": "2026-02-09T10:00:00Z",
        "updatedAt": "2026-02-09T10:00:00Z"
      },
      "startDate": "2026-02-09T10:00:00Z",
      "currentProgress": 0,
      "status": "Active",
      "completionDate": null,
      "skillProgress": [],
      "createdAt": "2026-02-09T10:00:00Z",
      "updatedAt": "2026-02-09T10:00:00Z"
    }
  ],
  "page": 0,
  "size": 20,
  "totalItems": 1,
  "totalPages": 1
}
```

### Get a student plan by id
`GET /api/development/plans/student-plan/{studentPlanId}`

### Update a student plan
`PUT /api/development/plans/student-plan/{studentPlanId}`

Body:
```json
{
  "planId": "...",
  "subjectId": "...",
  "currentProgress": 40,
  "status": "active",
  "current": true,
  "startDate": "2026-02-01T00:00:00Z",
  "completionDate": null
}
```

### Delete a student plan (soft delete)
`DELETE /api/development/plans/student-plan/{studentPlanId}`

## Mastery Signals

### Summary by subject/class
`GET /api/development/mastery-signals`

Query params:
- `subjectId` (optional)
- `classId` (optional)
- `classSubjectId` (optional)

Response (200):
```json
{
  "totalStudents": 25,
  "excellent": 3,
  "good": 8,
  "average": 9,
  "needsImprovement": 5,
  "averageOverall": 61.2
}
```

## Re-teach Cards

### Create
`POST /api/reteach-cards`

Body:
```json
{
  "subjectId": "...",
  "topicId": "...",
  "title": "Quadratics remediation",
  "issueSummary": "Students struggling with factorisation",
  "recommendedActions": "Re-teach factorisation and run exit ticket",
  "priority": "high",
  "status": "active",
  "affectedStudentIds": ["...", "..."],
  "createdBy": "..."
}
```

### List
`GET /api/reteach-cards`

Query params:
- `subjectId` (optional)
- `topicId` (optional)
- `priority` (optional) `low | medium | high`
- `status` (optional) `draft | active | resolved | archived`

### Detail (includes student names)
`GET /api/reteach-cards/{id}`

Response (200):
```json
{
  "id": "...",
  "subjectId": "...",
  "subjectName": "Mathematics",
  "topicId": "...",
  "topicName": "Quadratic Equations",
  "title": "Quadratics remediation",
  "issueSummary": "Students struggling with factorisation",
  "recommendedActions": "Re-teach factorisation and run exit ticket",
  "priority": "high",
  "status": "active",
  "affectedStudentIds": ["...", "..."],
  "affectedStudents": [
    { "id": "...", "firstName": "Tinashe", "lastName": "Dube" }
  ],
  "affectedStudentsCount": 2,
  "createdAt": "2026-02-09T10:00:00Z",
  "updatedAt": "2026-02-09T10:00:00Z"
}
```

### Update
`PUT /api/reteach-cards/{id}`

### Delete (soft delete)
`DELETE /api/reteach-cards/{id}`
