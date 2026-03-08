INSERT INTO lookups.question_type (code, name)
VALUES ('multiple_choice', 'Multiple Choice')
ON CONFLICT (code) DO NOTHING;
