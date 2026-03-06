# Sync Aggregate Coverage (Cloud/Edge)

This file maps sync-enabled aggregates to the underlying LMS tables used by demo flows.

## Currently sync-enabled aggregates and tables

- `Subject` -> `lms.subjects`
- `Plan` -> `lms.plans`
- `PlanStep` -> `lms.plan_steps`
- `StudentPlan` -> `lms.student_plans`
- `Assessment` -> `lms.assessments`
- `AssessmentAssignment` -> `lms.assessment_assignments`
- `Resource` -> `lms.resources`
- `Question` -> `lms.questions`
- `AssessmentQuestion` -> `lms.assessment_questions`
- `AssessmentEnrollment` -> `lms.assessment_enrollments`
- `AssessmentAttempt` -> `lms.assessment_attempts`
- `AttemptAnswer` -> `lms.attempt_answers`
- `AssessmentResult` -> `lms.assessment_results`
- `TopicResource` -> `lms.topic_resources`

## Demo flow to table mapping

- Generate lesson plans:
  - `lms.plans`
  - `lms.plan_steps`
  - `lms.student_plans` (when assigned to students)

- Manage resources:
  - `lms.resources`
  - `lms.topic_resources` (resource-topic links)

- Create assessments and link questions:
  - `lms.assessments`
  - `lms.assessment_assignments`
  - `lms.questions`
  - `lms.assessment_questions`

- Mark/grade assessments:
  - `lms.assessment_enrollments`
  - `lms.assessment_attempts`
  - `lms.attempt_answers`
  - `lms.assessment_results`

## Sync transport/control-plane tables

- Edge outbox: `edge.sync_outbox`
- Edge inbox: `edge.sync_inbox`
- Pull checkpoint: `edge.sync_checkpoint`
- Cloud change feed: `edge.sync_change_log`
- Conflict log: `edge.sync_conflict_log`
