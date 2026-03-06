CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE SCHEMA IF NOT EXISTS util;
CREATE SCHEMA IF NOT EXISTS edge;

CREATE OR REPLACE FUNCTION util.tg_touch_row()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        NEW.created_at := COALESCE(NEW.created_at, NOW());
        NEW.updated_at := NOW();
        NEW.sync_version := COALESCE(NEW.sync_version, 1);
    ELSE
        NEW.updated_at := NOW();
        NEW.sync_version := COALESCE(OLD.sync_version, 0) + 1;
    END IF;
    RETURN NEW;
END;
$$;

CREATE TABLE IF NOT EXISTS edge.edge_nodes (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id uuid,
    device_id varchar(255) NOT NULL,
    node_code varchar(100),
    status varchar(32) NOT NULL DEFAULT 'active',
    last_seen_at timestamptz,
    registered_at timestamptz NOT NULL DEFAULT NOW(),
    last_sync_at timestamptz,
    last_pull_at timestamptz,
    last_push_at timestamptz,
    software_version varchar(128),
    metadata jsonb NOT NULL DEFAULT '{}'::jsonb,
    auth_key_hash varchar(255),
    sync_enabled boolean NOT NULL DEFAULT TRUE,
    tenant_code varchar(64),
    origin_node_id uuid,
    sync_version bigint NOT NULL DEFAULT 1,
    deleted_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT NOW(),
    updated_at timestamptz NOT NULL DEFAULT NOW()
);

ALTER TABLE edge.edge_nodes ADD COLUMN IF NOT EXISTS school_id uuid;
ALTER TABLE edge.edge_nodes ADD COLUMN IF NOT EXISTS device_id varchar(255);
ALTER TABLE edge.edge_nodes ADD COLUMN IF NOT EXISTS node_code varchar(100);
ALTER TABLE edge.edge_nodes ADD COLUMN IF NOT EXISTS status varchar(32);
ALTER TABLE edge.edge_nodes ADD COLUMN IF NOT EXISTS last_seen_at timestamptz;
ALTER TABLE edge.edge_nodes ADD COLUMN IF NOT EXISTS registered_at timestamptz;
ALTER TABLE edge.edge_nodes ADD COLUMN IF NOT EXISTS last_sync_at timestamptz;
ALTER TABLE edge.edge_nodes ADD COLUMN IF NOT EXISTS last_pull_at timestamptz;
ALTER TABLE edge.edge_nodes ADD COLUMN IF NOT EXISTS last_push_at timestamptz;
ALTER TABLE edge.edge_nodes ADD COLUMN IF NOT EXISTS software_version varchar(128);
ALTER TABLE edge.edge_nodes ADD COLUMN IF NOT EXISTS metadata jsonb;
ALTER TABLE edge.edge_nodes ADD COLUMN IF NOT EXISTS auth_key_hash varchar(255);
ALTER TABLE edge.edge_nodes ADD COLUMN IF NOT EXISTS sync_enabled boolean;
ALTER TABLE edge.edge_nodes ADD COLUMN IF NOT EXISTS tenant_code varchar(64);
ALTER TABLE edge.edge_nodes ADD COLUMN IF NOT EXISTS origin_node_id uuid;
ALTER TABLE edge.edge_nodes ADD COLUMN IF NOT EXISTS sync_version bigint;
ALTER TABLE edge.edge_nodes ADD COLUMN IF NOT EXISTS deleted_at timestamptz;
ALTER TABLE edge.edge_nodes ADD COLUMN IF NOT EXISTS created_at timestamptz;
ALTER TABLE edge.edge_nodes ADD COLUMN IF NOT EXISTS updated_at timestamptz;

UPDATE edge.edge_nodes
SET metadata = '{}'::jsonb
WHERE metadata IS NULL;

UPDATE edge.edge_nodes
SET status = COALESCE(NULLIF(status, ''), 'active'),
    registered_at = COALESCE(registered_at, created_at, NOW()),
    sync_enabled = COALESCE(sync_enabled, TRUE),
    sync_version = COALESCE(sync_version, 1),
    created_at = COALESCE(created_at, NOW()),
    updated_at = COALESCE(updated_at, NOW());

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_indexes
        WHERE schemaname = 'edge'
          AND indexname = 'ux_edge_nodes_school_device'
    ) THEN
        CREATE UNIQUE INDEX ux_edge_nodes_school_device
            ON edge.edge_nodes (school_id, device_id)
            WHERE deleted_at IS NULL;
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_indexes
        WHERE schemaname = 'edge'
          AND indexname = 'ux_edge_nodes_node_code'
    ) THEN
        CREATE UNIQUE INDEX ux_edge_nodes_node_code
            ON edge.edge_nodes (node_code)
            WHERE node_code IS NOT NULL AND deleted_at IS NULL;
    END IF;
END $$;

CREATE TABLE IF NOT EXISTS edge.sync_outbox (
    id bigserial PRIMARY KEY,
    edge_node_id uuid NOT NULL REFERENCES edge.edge_nodes(id),
    event_id uuid NOT NULL,
    aggregate_type varchar(255) NOT NULL,
    aggregate_id uuid NOT NULL,
    operation varchar(16) NOT NULL,
    entity_version bigint NOT NULL,
    payload jsonb NOT NULL,
    status varchar(16) NOT NULL DEFAULT 'PENDING',
    attempts integer NOT NULL DEFAULT 0,
    max_attempts integer NOT NULL DEFAULT 10,
    batch_id uuid,
    locked_at timestamptz,
    locked_by varchar(128),
    next_retry_at timestamptz,
    last_error text,
    synced_at timestamptz,
    origin_node_id uuid,
    sync_version bigint NOT NULL DEFAULT 1,
    deleted_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT NOW(),
    updated_at timestamptz NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_sync_outbox_operation CHECK (operation IN ('INSERT', 'UPDATE', 'DELETE')),
    CONSTRAINT chk_sync_outbox_status CHECK (status IN ('PENDING', 'IN_PROGRESS', 'SYNCED', 'FAILED', 'CONFLICT'))
);

ALTER TABLE edge.sync_outbox ADD COLUMN IF NOT EXISTS edge_node_id uuid;
ALTER TABLE edge.sync_outbox ADD COLUMN IF NOT EXISTS event_id uuid;
ALTER TABLE edge.sync_outbox ADD COLUMN IF NOT EXISTS aggregate_type varchar(255);
ALTER TABLE edge.sync_outbox ADD COLUMN IF NOT EXISTS aggregate_id uuid;
ALTER TABLE edge.sync_outbox ADD COLUMN IF NOT EXISTS operation varchar(16);
ALTER TABLE edge.sync_outbox ADD COLUMN IF NOT EXISTS entity_version bigint;
ALTER TABLE edge.sync_outbox ADD COLUMN IF NOT EXISTS payload jsonb;
ALTER TABLE edge.sync_outbox ADD COLUMN IF NOT EXISTS status varchar(16);
ALTER TABLE edge.sync_outbox ADD COLUMN IF NOT EXISTS attempts integer;
ALTER TABLE edge.sync_outbox ADD COLUMN IF NOT EXISTS max_attempts integer;
ALTER TABLE edge.sync_outbox ADD COLUMN IF NOT EXISTS batch_id uuid;
ALTER TABLE edge.sync_outbox ADD COLUMN IF NOT EXISTS locked_at timestamptz;
ALTER TABLE edge.sync_outbox ADD COLUMN IF NOT EXISTS locked_by varchar(128);
ALTER TABLE edge.sync_outbox ADD COLUMN IF NOT EXISTS next_retry_at timestamptz;
ALTER TABLE edge.sync_outbox ADD COLUMN IF NOT EXISTS last_error text;
ALTER TABLE edge.sync_outbox ADD COLUMN IF NOT EXISTS synced_at timestamptz;
ALTER TABLE edge.sync_outbox ADD COLUMN IF NOT EXISTS sent_at timestamptz;
ALTER TABLE edge.sync_outbox ADD COLUMN IF NOT EXISTS origin_node_id uuid;
ALTER TABLE edge.sync_outbox ADD COLUMN IF NOT EXISTS sync_version bigint;
ALTER TABLE edge.sync_outbox ADD COLUMN IF NOT EXISTS deleted_at timestamptz;
ALTER TABLE edge.sync_outbox ADD COLUMN IF NOT EXISTS created_at timestamptz;
ALTER TABLE edge.sync_outbox ADD COLUMN IF NOT EXISTS updated_at timestamptz;

UPDATE edge.sync_outbox
SET status = CASE
        WHEN COALESCE(sent_at IS NOT NULL, FALSE) THEN 'SYNCED'
        ELSE COALESCE(NULLIF(UPPER(status), ''), 'PENDING')
    END,
    attempts = COALESCE(attempts, 0),
    max_attempts = COALESCE(max_attempts, 10),
    payload = COALESCE(payload, '{}'::jsonb),
    sync_version = COALESCE(sync_version, 1),
    created_at = COALESCE(created_at, NOW()),
    updated_at = COALESCE(updated_at, NOW())
WHERE TRUE;

CREATE UNIQUE INDEX IF NOT EXISTS ux_sync_outbox_event_id ON edge.sync_outbox(event_id);
CREATE INDEX IF NOT EXISTS ix_sync_outbox_pending ON edge.sync_outbox(edge_node_id, status, next_retry_at, id);

CREATE TABLE IF NOT EXISTS edge.sync_inbox (
    id bigserial PRIMARY KEY,
    receiver_edge_node_id uuid NOT NULL REFERENCES edge.edge_nodes(id),
    cloud_change_id bigint NOT NULL,
    aggregate_type varchar(255) NOT NULL,
    aggregate_id uuid NOT NULL,
    operation varchar(16) NOT NULL,
    entity_version bigint NOT NULL,
    payload jsonb NOT NULL,
    status varchar(16) NOT NULL DEFAULT 'RECEIVED',
    attempts integer NOT NULL DEFAULT 0,
    error_message text,
    conflict_details jsonb,
    applied_at timestamptz,
    origin_node_id uuid,
    sync_version bigint NOT NULL DEFAULT 1,
    deleted_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT NOW(),
    updated_at timestamptz NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_sync_inbox_operation CHECK (operation IN ('INSERT', 'UPDATE', 'DELETE')),
    CONSTRAINT chk_sync_inbox_status CHECK (status IN ('RECEIVED', 'APPLIED', 'SKIPPED', 'FAILED', 'CONFLICT'))
);

ALTER TABLE edge.sync_inbox ADD COLUMN IF NOT EXISTS receiver_edge_node_id uuid;
ALTER TABLE edge.sync_inbox ADD COLUMN IF NOT EXISTS cloud_change_id bigint;
ALTER TABLE edge.sync_inbox ADD COLUMN IF NOT EXISTS aggregate_type varchar(255);
ALTER TABLE edge.sync_inbox ADD COLUMN IF NOT EXISTS aggregate_id uuid;
ALTER TABLE edge.sync_inbox ADD COLUMN IF NOT EXISTS operation varchar(16);
ALTER TABLE edge.sync_inbox ADD COLUMN IF NOT EXISTS entity_version bigint;
ALTER TABLE edge.sync_inbox ADD COLUMN IF NOT EXISTS payload jsonb;
ALTER TABLE edge.sync_inbox ADD COLUMN IF NOT EXISTS status varchar(16);
ALTER TABLE edge.sync_inbox ADD COLUMN IF NOT EXISTS attempts integer;
ALTER TABLE edge.sync_inbox ADD COLUMN IF NOT EXISTS error_message text;
ALTER TABLE edge.sync_inbox ADD COLUMN IF NOT EXISTS conflict_details jsonb;
ALTER TABLE edge.sync_inbox ADD COLUMN IF NOT EXISTS applied_at timestamptz;
ALTER TABLE edge.sync_inbox ADD COLUMN IF NOT EXISTS origin_node_id uuid;
ALTER TABLE edge.sync_inbox ADD COLUMN IF NOT EXISTS sync_version bigint;
ALTER TABLE edge.sync_inbox ADD COLUMN IF NOT EXISTS deleted_at timestamptz;
ALTER TABLE edge.sync_inbox ADD COLUMN IF NOT EXISTS created_at timestamptz;
ALTER TABLE edge.sync_inbox ADD COLUMN IF NOT EXISTS updated_at timestamptz;

UPDATE edge.sync_inbox
SET status = COALESCE(NULLIF(UPPER(status), ''), 'RECEIVED'),
    attempts = COALESCE(attempts, 0),
    payload = COALESCE(payload, '{}'::jsonb),
    sync_version = COALESCE(sync_version, 1),
    created_at = COALESCE(created_at, NOW()),
    updated_at = COALESCE(updated_at, NOW());

CREATE UNIQUE INDEX IF NOT EXISTS ux_sync_inbox_edge_change
    ON edge.sync_inbox(receiver_edge_node_id, cloud_change_id);

CREATE TABLE IF NOT EXISTS edge.sync_checkpoint (
    edge_node_id uuid PRIMARY KEY REFERENCES edge.edge_nodes(id),
    last_pulled_change_id bigint NOT NULL DEFAULT 0,
    last_successful_push_at timestamptz,
    last_successful_pull_at timestamptz,
    last_push_batch_id uuid,
    last_pull_batch_id uuid,
    origin_node_id uuid,
    sync_version bigint NOT NULL DEFAULT 1,
    deleted_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT NOW(),
    updated_at timestamptz NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS edge.sync_conflict_log (
    id bigserial PRIMARY KEY,
    edge_node_id uuid NOT NULL REFERENCES edge.edge_nodes(id),
    conflict_scope varchar(64) NOT NULL,
    aggregate_type varchar(255) NOT NULL,
    aggregate_id uuid NOT NULL,
    local_version bigint,
    incoming_version bigint,
    resolved_version bigint,
    conflict_type varchar(64) NOT NULL,
    local_payload jsonb,
    incoming_payload jsonb,
    resolution_strategy varchar(64),
    resolved boolean NOT NULL DEFAULT FALSE,
    notes text,
    origin_node_id uuid,
    sync_version bigint NOT NULL DEFAULT 1,
    deleted_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT NOW(),
    updated_at timestamptz NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS ix_sync_conflict_edge_aggregate
    ON edge.sync_conflict_log(edge_node_id, aggregate_type, aggregate_id, created_at DESC);

CREATE TABLE IF NOT EXISTS edge.sync_change_log (
    change_id bigserial PRIMARY KEY,
    source_edge_node_id uuid NOT NULL REFERENCES edge.edge_nodes(id),
    aggregate_type varchar(255) NOT NULL,
    aggregate_id uuid NOT NULL,
    operation varchar(16) NOT NULL,
    entity_version bigint NOT NULL,
    payload jsonb NOT NULL,
    school_id uuid,
    created_at timestamptz NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_sync_change_log_operation CHECK (operation IN ('INSERT', 'UPDATE', 'DELETE'))
);

CREATE INDEX IF NOT EXISTS ix_sync_change_log_school_change
    ON edge.sync_change_log(school_id, change_id);
CREATE INDEX IF NOT EXISTS ix_sync_change_log_edge_change
    ON edge.sync_change_log(source_edge_node_id, change_id);

DROP TRIGGER IF EXISTS trg_touch_row_edge_nodes ON edge.edge_nodes;
CREATE TRIGGER trg_touch_row_edge_nodes
    BEFORE INSERT OR UPDATE ON edge.edge_nodes
    FOR EACH ROW EXECUTE FUNCTION util.tg_touch_row();

DROP TRIGGER IF EXISTS trg_touch_row_sync_outbox ON edge.sync_outbox;
CREATE TRIGGER trg_touch_row_sync_outbox
    BEFORE INSERT OR UPDATE ON edge.sync_outbox
    FOR EACH ROW EXECUTE FUNCTION util.tg_touch_row();

DROP TRIGGER IF EXISTS trg_touch_row_sync_inbox ON edge.sync_inbox;
CREATE TRIGGER trg_touch_row_sync_inbox
    BEFORE INSERT OR UPDATE ON edge.sync_inbox
    FOR EACH ROW EXECUTE FUNCTION util.tg_touch_row();

DROP TRIGGER IF EXISTS trg_touch_row_sync_checkpoint ON edge.sync_checkpoint;
CREATE TRIGGER trg_touch_row_sync_checkpoint
    BEFORE INSERT OR UPDATE ON edge.sync_checkpoint
    FOR EACH ROW EXECUTE FUNCTION util.tg_touch_row();

DROP TRIGGER IF EXISTS trg_touch_row_sync_conflict_log ON edge.sync_conflict_log;
CREATE TRIGGER trg_touch_row_sync_conflict_log
    BEFORE INSERT OR UPDATE ON edge.sync_conflict_log
    FOR EACH ROW EXECUTE FUNCTION util.tg_touch_row();
