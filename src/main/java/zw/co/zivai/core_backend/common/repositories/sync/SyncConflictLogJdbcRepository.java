package zw.co.zivai.core_backend.common.repositories.sync;

import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.databind.JsonNode;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class SyncConflictLogJdbcRepository {
    private final JdbcTemplate jdbcTemplate;

    public void logConflict(
        UUID edgeNodeId,
        String conflictScope,
        String aggregateType,
        UUID aggregateId,
        long localVersion,
        long incomingVersion,
        String conflictType,
        JsonNode localPayload,
        JsonNode incomingPayload,
        String resolutionStrategy
    ) {
        jdbcTemplate.update(
            """
            INSERT INTO edge.sync_conflict_log (
              edge_node_id, conflict_scope, aggregate_type, aggregate_id,
              local_version, incoming_version, conflict_type,
              local_payload, incoming_payload, resolution_strategy, resolved
            ) VALUES (?, ?, ?, ?, ?, ?, ?, CAST(? AS jsonb), CAST(? AS jsonb), ?, FALSE)
            """,
            edgeNodeId,
            conflictScope,
            aggregateType,
            aggregateId,
            localVersion,
            incomingVersion,
            conflictType,
            localPayload == null ? "{}" : localPayload.toString(),
            incomingPayload == null ? "{}" : incomingPayload.toString(),
            resolutionStrategy
        );
    }
}
