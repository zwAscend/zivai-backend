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
          AND rel.relname = 'notifications'
          AND con.contype = 'c'
          AND pg_get_constraintdef(con.oid) ILIKE '%notif_type%'
    LOOP
        EXECUTE format('ALTER TABLE lms.notifications DROP CONSTRAINT %I', constraint_name);
    END LOOP;

    ALTER TABLE lms.notifications
        ADD CONSTRAINT chk_notifications_notif_type
        CHECK (notif_type IN (
            'assignment_graded',
            'assignment_submitted',
            'plan_assigned',
            'message_received',
            'assessment_assigned',
            'assessment_published',
            'assessment_deadline_changed',
            'assessment_submitted',
            'development_plan_assigned',
            'development_plan_updated',
            'development_plan_published',
            'development_plan_unpublished',
            'resource_published'
        ));
END $$;
