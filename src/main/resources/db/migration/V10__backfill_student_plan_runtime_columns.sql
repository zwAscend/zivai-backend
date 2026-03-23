ALTER TABLE lms.student_plans
  ADD COLUMN IF NOT EXISTS active_step_id uuid REFERENCES lms.plan_steps(id) ON DELETE SET NULL;

ALTER TABLE lms.student_plans
  ADD COLUMN IF NOT EXISTS completed_step_ids jsonb NOT NULL DEFAULT '[]'::jsonb;

UPDATE lms.student_plans
SET completed_step_ids = '[]'::jsonb
WHERE completed_step_ids IS NULL;
