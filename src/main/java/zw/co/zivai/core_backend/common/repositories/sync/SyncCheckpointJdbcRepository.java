package zw.co.zivai.core_backend.common.repositories.sync;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class SyncCheckpointJdbcRepository {
    private final JdbcTemplate jdbcTemplate;

    public Optional<CheckpointRecord> findByEdgeNodeId(UUID edgeNodeId) {
        return jdbcTemplate.query(
            """
            SELECT last_pulled_change_id, last_successful_push_at, last_successful_pull_at
            FROM edge.sync_checkpoint
            WHERE edge_node_id = ?
            """,
            (rs, rowNum) -> new CheckpointRecord(
                rs.getLong("last_pulled_change_id"),
                rs.getObject("last_successful_push_at", Instant.class),
                rs.getObject("last_successful_pull_at", Instant.class)
            ),
            edgeNodeId
        ).stream().findFirst();
    }

    public void touchPush(UUID edgeNodeId, Instant when, UUID batchId) {
        jdbcTemplate.update(
            """
            INSERT INTO edge.sync_checkpoint(edge_node_id, last_push_batch_id, last_successful_push_at)
            VALUES (?, ?, ?)
            ON CONFLICT (edge_node_id) DO UPDATE
            SET last_push_batch_id = EXCLUDED.last_push_batch_id,
                last_successful_push_at = EXCLUDED.last_successful_push_at
            """,
            edgeNodeId,
            batchId,
            when
        );
    }

    public void touchPull(UUID edgeNodeId, Instant when, UUID batchId, long lastPulledChangeId) {
        jdbcTemplate.update(
            """
            INSERT INTO edge.sync_checkpoint(edge_node_id, last_pull_batch_id, last_successful_pull_at, last_pulled_change_id)
            VALUES (?, ?, ?, ?)
            ON CONFLICT (edge_node_id) DO UPDATE
            SET last_pull_batch_id = EXCLUDED.last_pull_batch_id,
                last_successful_pull_at = EXCLUDED.last_successful_pull_at,
                last_pulled_change_id = GREATEST(edge.sync_checkpoint.last_pulled_change_id, EXCLUDED.last_pulled_change_id)
            """,
            edgeNodeId,
            batchId,
            when,
            lastPulledChangeId
        );
    }

    public record CheckpointRecord(long lastPulledChangeId, Instant lastSuccessfulPushAt, Instant lastSuccessfulPullAt) {
    }
}
