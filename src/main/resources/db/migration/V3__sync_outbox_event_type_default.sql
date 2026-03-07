ALTER TABLE edge.sync_outbox
  ADD COLUMN IF NOT EXISTS event_type varchar(255);

UPDATE edge.sync_outbox
SET event_type = COALESCE(NULLIF(BTRIM(event_type), ''), NULLIF(BTRIM(aggregate_type), ''), NULLIF(BTRIM(operation), ''), 'UNKNOWN')
WHERE event_type IS NULL OR BTRIM(event_type) = '';

ALTER TABLE edge.sync_outbox
  ALTER COLUMN event_type SET DEFAULT 'UNKNOWN';

ALTER TABLE edge.sync_outbox
  ALTER COLUMN event_type SET NOT NULL;
