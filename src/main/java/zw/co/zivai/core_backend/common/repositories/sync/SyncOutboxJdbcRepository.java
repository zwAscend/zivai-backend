package zw.co.zivai.core_backend.common.repositories.sync;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import zw.co.zivai.core_backend.common.dtos.sync.SyncChangeItemDto;

@Repository
@RequiredArgsConstructor
public class SyncOutboxJdbcRepository {
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private volatile Boolean syncOutboxHasEventTypeColumn;

    public void insert(UUID edgeNodeId, UUID eventId, String aggregateType, UUID aggregateId, String operation, long entityVersion, JsonNode payload) {
        String payloadJson = payload == null ? "{}" : payload.toString();
        if (Boolean.FALSE.equals(syncOutboxHasEventTypeColumn)) {
            try {
                insertWithoutEventType(edgeNodeId, eventId, aggregateType, aggregateId, operation, entityVersion, payloadJson);
                return;
            } catch (DataAccessException ex) {
                if (!isMissingRequiredEventType(ex)) {
                    throw ex;
                }
                syncOutboxHasEventTypeColumn = Boolean.TRUE;
            }
        }

        try {
            insertWithEventType(edgeNodeId, eventId, aggregateType, aggregateId, operation, entityVersion, payloadJson);
            syncOutboxHasEventTypeColumn = Boolean.TRUE;
        } catch (DataAccessException ex) {
            if (!isUndefinedEventTypeColumn(ex)) {
                throw ex;
            }
            syncOutboxHasEventTypeColumn = Boolean.FALSE;
            insertWithoutEventType(edgeNodeId, eventId, aggregateType, aggregateId, operation, entityVersion, payloadJson);
        }
    }

    public List<SyncChangeItemDto> reservePendingBatch(UUID edgeNodeId, UUID batchId, int limit) {
        return jdbcTemplate.query(
            """
            WITH picked AS (
              SELECT id
              FROM edge.sync_outbox
              WHERE edge_node_id = ?
                AND status IN ('PENDING', 'FAILED')
                AND (next_retry_at IS NULL OR next_retry_at <= NOW())
              ORDER BY id
              LIMIT ?
              FOR UPDATE SKIP LOCKED
            )
            UPDATE edge.sync_outbox o
            SET status = 'IN_PROGRESS',
                locked_at = NOW(),
                locked_by = ?,
                batch_id = ?,
                attempts = COALESCE(o.attempts, 0) + 1
            FROM picked p
            WHERE o.id = p.id
            RETURNING o.event_id, o.aggregate_type, o.aggregate_id, o.operation, o.entity_version, o.payload::text AS payload_json, o.created_at
            """,
            (rs, rowNum) -> mapChange(rs),
            edgeNodeId,
            limit,
            "edge-worker",
            batchId
        );
    }

    public void markResult(UUID eventId, String status, String errorMessage) {
        boolean synced = "SYNCED".equals(status);
        jdbcTemplate.update(
            """
            UPDATE edge.sync_outbox
            SET status = ?,
                synced_at = CASE WHEN ? THEN NOW() ELSE synced_at END,
                next_retry_at = CASE
                    WHEN ? = 'FAILED' THEN NOW() + INTERVAL '30 seconds'
                    ELSE NULL
                END,
                last_error = ?,
                locked_at = NULL,
                locked_by = NULL
            WHERE event_id = ?
            """,
            status,
            synced,
            status,
            errorMessage,
            eventId
        );
    }

    public long countByStatus(UUID edgeNodeId, String status) {
        Long count = jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)::bigint
            FROM edge.sync_outbox
            WHERE edge_node_id = ? AND status = ?
            """,
            Long.class,
            edgeNodeId,
            status
        );
        return count == null ? 0L : count;
    }

    public void failInProgressBatch(UUID batchId, String errorMessage) {
        jdbcTemplate.update(
            """
            UPDATE edge.sync_outbox
            SET status = 'FAILED',
                next_retry_at = NOW() + INTERVAL '30 seconds',
                last_error = ?,
                locked_at = NULL,
                locked_by = NULL
            WHERE batch_id = ?
              AND status = 'IN_PROGRESS'
            """,
            errorMessage,
            batchId
        );
    }

    private SyncChangeItemDto mapChange(ResultSet rs) throws SQLException {
        try {
            return SyncChangeItemDto.builder()
                .eventId(rs.getObject("event_id", UUID.class))
                .aggregateType(rs.getString("aggregate_type"))
                .aggregateId(rs.getObject("aggregate_id", UUID.class))
                .operation(rs.getString("operation"))
                .entityVersion(rs.getLong("entity_version"))
                .payload(objectMapper.readTree(rs.getString("payload_json")))
                .createdAt(rs.getObject("created_at", Instant.class))
                .build();
        } catch (Exception ex) {
            throw new SQLException("Failed to parse sync outbox payload JSON", ex);
        }
    }

    private String deriveEventType(String aggregateType, String operation) {
        if (aggregateType != null && !aggregateType.isBlank()) {
            return aggregateType;
        }
        return operation == null || operation.isBlank() ? "UNKNOWN" : operation;
    }

    private void insertWithEventType(
        UUID edgeNodeId,
        UUID eventId,
        String aggregateType,
        UUID aggregateId,
        String operation,
        long entityVersion,
        String payloadJson
    ) {
        jdbcTemplate.update(
            """
            INSERT INTO edge.sync_outbox (
              edge_node_id, event_id, event_type, aggregate_type, aggregate_id,
              operation, entity_version, payload, status
            ) VALUES (?, ?, ?, ?, ?, ?, ?, CAST(? AS jsonb), 'PENDING')
            """,
            edgeNodeId,
            eventId,
            deriveEventType(aggregateType, operation),
            aggregateType,
            aggregateId,
            operation,
            entityVersion,
            payloadJson
        );
    }

    private void insertWithoutEventType(
        UUID edgeNodeId,
        UUID eventId,
        String aggregateType,
        UUID aggregateId,
        String operation,
        long entityVersion,
        String payloadJson
    ) {
        jdbcTemplate.update(
            """
            INSERT INTO edge.sync_outbox (
              edge_node_id, event_id, aggregate_type, aggregate_id,
              operation, entity_version, payload, status
            ) VALUES (?, ?, ?, ?, ?, ?, CAST(? AS jsonb), 'PENDING')
            """,
            edgeNodeId,
            eventId,
            aggregateType,
            aggregateId,
            operation,
            entityVersion,
            payloadJson
        );
    }

    private boolean isUndefinedEventTypeColumn(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof SQLException sqlEx && (
                "42703".equals(sqlEx.getSQLState())
                || messageContains(sqlEx.getMessage(), "column")
            ) && messageContains(sqlEx.getMessage(), "event_type")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private boolean isMissingRequiredEventType(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof SQLException sqlEx && (
                "23502".equals(sqlEx.getSQLState())
                || messageContains(sqlEx.getMessage(), "null value")
            ) && messageContains(sqlEx.getMessage(), "event_type")
                && messageContains(sqlEx.getMessage(), "sync_outbox")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private boolean messageContains(String source, String token) {
        return source != null && token != null && source.toLowerCase().contains(token.toLowerCase());
    }
}
