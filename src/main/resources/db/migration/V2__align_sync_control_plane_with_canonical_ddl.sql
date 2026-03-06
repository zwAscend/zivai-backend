CREATE OR REPLACE FUNCTION util.tg_touch_row()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        NEW.created_at := COALESCE(NEW.created_at, NOW());
        NEW.updated_at := NOW();
        NEW.sync_version := COALESCE(NULLIF(NEW.sync_version, 0), 1);
    ELSE
        NEW.updated_at := NOW();
        NEW.sync_version := COALESCE(OLD.sync_version, 0) + 1;
    END IF;
    RETURN NEW;
END;
$$;

ALTER TABLE edge.edge_nodes
    ALTER COLUMN device_id TYPE varchar(128),
    ALTER COLUMN status TYPE varchar(16),
    ALTER COLUMN software_version TYPE varchar(64),
    ALTER COLUMN sync_version SET DEFAULT 1;

ALTER TABLE edge.edge_nodes
    ALTER COLUMN registered_at SET DEFAULT NOW(),
    ALTER COLUMN sync_enabled SET DEFAULT TRUE;

UPDATE edge.edge_nodes
SET registered_at = COALESCE(registered_at, created_at, NOW()),
    sync_enabled = COALESCE(sync_enabled, TRUE),
    sync_version = COALESCE(NULLIF(sync_version, 0), 1),
    created_at = COALESCE(created_at, NOW()),
    updated_at = COALESCE(updated_at, NOW());

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'chk_edge_nodes_status'
          AND connamespace = 'edge'::regnamespace
    ) THEN
        ALTER TABLE edge.edge_nodes
            ADD CONSTRAINT chk_edge_nodes_status
            CHECK (status IN ('active', 'inactive', 'retired'));
    END IF;
END $$;

ALTER TABLE edge.edge_model_deployments
    ALTER COLUMN sync_version SET DEFAULT 1;

UPDATE edge.edge_model_deployments
SET sync_version = COALESCE(NULLIF(sync_version, 0), 1),
    created_at = COALESCE(created_at, NOW()),
    updated_at = COALESCE(updated_at, NOW());

ALTER TABLE edge.sync_outbox
    ALTER COLUMN sync_version SET DEFAULT 1;

UPDATE edge.sync_outbox
SET sync_version = COALESCE(NULLIF(sync_version, 0), 1),
    created_at = COALESCE(created_at, NOW()),
    updated_at = COALESCE(updated_at, NOW());

ALTER TABLE edge.sync_inbox
    ADD COLUMN IF NOT EXISTS processed_at timestamptz;

ALTER TABLE edge.sync_inbox
    ALTER COLUMN sync_version SET DEFAULT 1;

UPDATE edge.sync_inbox
SET sync_version = COALESCE(NULLIF(sync_version, 0), 1),
    created_at = COALESCE(created_at, NOW()),
    updated_at = COALESCE(updated_at, NOW());

ALTER TABLE edge.sync_checkpoint
    ALTER COLUMN sync_version SET DEFAULT 1;

UPDATE edge.sync_checkpoint
SET sync_version = COALESCE(NULLIF(sync_version, 0), 1),
    created_at = COALESCE(created_at, NOW()),
    updated_at = COALESCE(updated_at, NOW());

ALTER TABLE edge.sync_conflict_log
    ALTER COLUMN sync_version SET DEFAULT 1;

UPDATE edge.sync_conflict_log
SET sync_version = COALESCE(NULLIF(sync_version, 0), 1),
    created_at = COALESCE(created_at, NOW()),
    updated_at = COALESCE(updated_at, NOW());
