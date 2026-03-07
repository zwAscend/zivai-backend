DO $$
DECLARE
    constraint_name text;
BEGIN
    FOR constraint_name IN
        SELECT con.conname
        FROM pg_constraint con
        JOIN pg_class rel ON rel.oid = con.conrelid
        JOIN pg_namespace nsp ON nsp.oid = rel.relnamespace
        WHERE nsp.nspname = 'lms'
          AND rel.relname = 'resources'
          AND con.contype = 'c'
          AND pg_get_constraintdef(con.oid) ILIKE '%status%'
    LOOP
        EXECUTE format('ALTER TABLE lms.resources DROP CONSTRAINT %I', constraint_name);
    END LOOP;

    ALTER TABLE lms.resources
        ADD CONSTRAINT chk_resources_status
        CHECK (status IN ('active', 'draft', 'archived', 'published'));
END $$;
