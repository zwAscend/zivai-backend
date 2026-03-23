ALTER TABLE lms.assessments
DROP CONSTRAINT IF EXISTS assessments_assessment_type_check;

ALTER TABLE lms.assessments
ADD CONSTRAINT assessments_assessment_type_check
CHECK (assessment_type IN ('quiz', 'test', 'assignment', 'project', 'exam', 'practice'));
