package zw.co.zivai.core_backend.common.repositories.sync;

import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.databind.JsonNode;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class SyncInboxJdbcRepository {
    private final JdbcTemplate jdbcTemplate;

    public void insertReceived(UUID edgeNodeId, long cloudChangeId, String aggregateType, UUID aggregateId, String operation, long entityVersion, JsonNode payload) {
        jdbcTemplate.update(
            """
            INSERT INTO edge.sync_inbox (
              receiver_edge_node_id, cloud_change_id, aggregate_type,
              aggregate_id, operation, entity_version, payload, status
            ) VALUES (?, ?, ?, ?, ?, ?, CAST(? AS jsonb), 'RECEIVED')
            ON CONFLICT (receiver_edge_node_id, cloud_change_id) DO NOTHING
            """,
            edgeNodeId,
            cloudChangeId,
            aggregateType,
            aggregateId,
            operation,
            entityVersion,
            payload == null ? "{}" : payload.toString()
        );
    }

    public void markResult(UUID edgeNodeId, long cloudChangeId, String status, String errorMessage) {
        jdbcTemplate.update(
            """
            UPDATE edge.sync_inbox
            SET status = ?, error_message = ?, applied_at = CASE WHEN ? IN ('APPLIED', 'SKIPPED') THEN NOW() ELSE applied_at END
            WHERE receiver_edge_node_id = ? AND cloud_change_id = ?
            """,
            status,
            errorMessage,
            status,
            edgeNodeId,
            cloudChangeId
        );
    }
}
