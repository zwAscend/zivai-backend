ALTER TABLE IF EXISTS lms.users
    ADD COLUMN IF NOT EXISTS password_hash varchar(255);

ALTER TABLE IF EXISTS lms.plan_steps
    ADD COLUMN IF NOT EXISTS content text;
