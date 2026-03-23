WITH ranked_attempts AS (
    SELECT
        at.id AS attempt_id,
        ae.id AS enrollment_id,
        aa.id AS assignment_id,
        a.id AS assessment_id,
        ROW_NUMBER() OVER (
            PARTITION BY
                ae.student_id,
                a.subject_id,
                LOWER(COALESCE(NULLIF(BTRIM(aa.title), ''), a.name, '')),
                LOWER(COALESCE(at.submission_type, ''))
            ORDER BY
                COALESCE(at.started_at, at.created_at) DESC,
                at.created_at DESC,
                at.id DESC
        ) AS duplicate_rank
    FROM lms.assessment_attempts at
    JOIN lms.assessment_enrollments ae
      ON ae.id = at.assessment_enrollment_id
     AND ae.deleted_at IS NULL
    JOIN lms.assessment_assignments aa
      ON aa.id = ae.assessment_assignment_id
     AND aa.deleted_at IS NULL
    JOIN lms.assessments a
      ON a.id = aa.assessment_id
     AND a.deleted_at IS NULL
    WHERE at.deleted_at IS NULL
      AND at.submitted_at IS NULL
      AND LOWER(COALESCE(a.assessment_type, '')) = 'practice'
      AND LOWER(COALESCE(a.visibility, '')) = 'private'
      AND LOWER(COALESCE(at.submission_type, '')) IN ('topic_practice', 'topic_challenge', 'subject_challenge')
),
duplicate_attempts AS (
    SELECT attempt_id, enrollment_id, assignment_id, assessment_id
    FROM ranked_attempts
    WHERE duplicate_rank > 1
),
mark_attempt_answers AS (
    UPDATE lms.attempt_answers answer
       SET deleted_at = NOW(),
           updated_at = NOW(),
           sync_version = COALESCE(answer.sync_version, 0) + 1
     WHERE answer.deleted_at IS NULL
       AND answer.assessment_attempt_id IN (SELECT attempt_id FROM duplicate_attempts)
    RETURNING answer.id
),
mark_attempts AS (
    UPDATE lms.assessment_attempts attempt
       SET deleted_at = NOW(),
           updated_at = NOW(),
           sync_version = COALESCE(attempt.sync_version, 0) + 1
     WHERE attempt.deleted_at IS NULL
       AND attempt.id IN (SELECT attempt_id FROM duplicate_attempts)
    RETURNING attempt.id
),
mark_results AS (
    UPDATE lms.assessment_results result
       SET deleted_at = NOW(),
           updated_at = NOW(),
           sync_version = COALESCE(result.sync_version, 0) + 1
     WHERE result.deleted_at IS NULL
       AND result.assessment_assignment_id IN (SELECT assignment_id FROM duplicate_attempts)
    RETURNING result.id
),
mark_enrollments AS (
    UPDATE lms.assessment_enrollments enrollment
       SET deleted_at = NOW(),
           updated_at = NOW(),
           sync_version = COALESCE(enrollment.sync_version, 0) + 1
     WHERE enrollment.deleted_at IS NULL
       AND enrollment.id IN (SELECT enrollment_id FROM duplicate_attempts)
    RETURNING enrollment.id
),
mark_questions AS (
    UPDATE lms.assessment_questions question
       SET deleted_at = NOW(),
           updated_at = NOW(),
           sync_version = COALESCE(question.sync_version, 0) + 1
     WHERE question.deleted_at IS NULL
       AND question.assessment_id IN (SELECT assessment_id FROM duplicate_attempts)
    RETURNING question.id
),
mark_assignments AS (
    UPDATE lms.assessment_assignments assignment
       SET deleted_at = NOW(),
           updated_at = NOW(),
           sync_version = COALESCE(assignment.sync_version, 0) + 1
     WHERE assignment.deleted_at IS NULL
       AND assignment.id IN (SELECT assignment_id FROM duplicate_attempts)
    RETURNING assignment.id
),
mark_assessments AS (
    UPDATE lms.assessments assessment
       SET deleted_at = NOW(),
           updated_at = NOW(),
           sync_version = COALESCE(assessment.sync_version, 0) + 1
     WHERE assessment.deleted_at IS NULL
       AND assessment.id IN (SELECT assessment_id FROM duplicate_attempts)
    RETURNING assessment.id
)
SELECT COUNT(*) FROM duplicate_attempts;
