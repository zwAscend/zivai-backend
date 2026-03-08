INSERT INTO lookups.enrolment_status (code, name) VALUES
  ('active', 'Active'),
  ('dropped', 'Dropped'),
  ('completed', 'Completed')
ON CONFLICT (code) DO NOTHING;

INSERT INTO lookups.roles (code, name) VALUES
  ('student', 'Student'),
  ('teacher', 'Teacher'),
  ('admin', 'Admin'),
  ('parent', 'Parent')
ON CONFLICT (code) DO NOTHING;

INSERT INTO lookups.question_type (code, name) VALUES
  ('short_answer', 'Short Answer'),
  ('structured', 'Structured'),
  ('mcq', 'Multiple Choice'),
  ('multiple_choice', 'Multiple Choice'),
  ('true_false', 'True or False'),
  ('essay', 'Essay')
ON CONFLICT (code) DO NOTHING;

INSERT INTO lookups.exam_style (code, name) VALUES
  ('past_paper', 'Past Paper'),
  ('teacher_created', 'Teacher Created')
ON CONFLICT (code) DO NOTHING;

INSERT INTO lookups.grading_status (code, name) VALUES
  ('pending', 'Pending'),
  ('auto_graded', 'Auto Graded'),
  ('reviewed', 'Reviewed')
ON CONFLICT (code) DO NOTHING;

INSERT INTO lookups.risk_level (code, name) VALUES
  ('low', 'Low'),
  ('medium', 'Medium'),
  ('high', 'High')
ON CONFLICT (code) DO NOTHING;

INSERT INTO lookups.exam_board (code, name) VALUES
  ('zimsec', 'ZIMSEC'),
  ('cambridge', 'Cambridge')
ON CONFLICT (code) DO NOTHING;
