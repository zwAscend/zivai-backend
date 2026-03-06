package zw.co.zivai.core_backend.common.repositories.sync;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import zw.co.zivai.core_backend.common.dtos.sync.SyncPullChangeDto;

@Repository
@RequiredArgsConstructor
public class SyncChangeLogJdbcRepository {
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public Long appendChange(UUID sourceEdgeNodeId, String aggregateType, UUID aggregateId, String operation, long entityVersion, JsonNode payload, UUID schoolId) {
        return jdbcTemplate.queryForObject(
            """
            INSERT INTO edge.sync_change_log (
              source_edge_node_id, aggregate_type, aggregate_id,
              operation, entity_version, payload, school_id
            ) VALUES (?, ?, ?, ?, ?, CAST(? AS jsonb), ?)
            RETURNING change_id
            """,
            Long.class,
            sourceEdgeNodeId,
            aggregateType,
            aggregateId,
            operation,
            entityVersion,
            payload == null ? "{}" : payload.toString(),
            schoolId
        );
    }

    public List<SyncPullChangeDto> findAfter(long afterChangeId, UUID schoolId, UUID requestingEdgeNodeId, int limit) {
        return jdbcTemplate.query(
            """
            SELECT change_id, source_edge_node_id, aggregate_type, aggregate_id,
                   operation, entity_version, payload::text AS payload_json, created_at
            FROM edge.sync_change_log
            WHERE change_id > ?
              AND (school_id = ? OR school_id IS NULL)
              AND source_edge_node_id <> ?
            ORDER BY change_id
            LIMIT ?
            """,
            (rs, rowNum) -> mapPullChange(rs),
            afterChangeId,
            schoolId,
            requestingEdgeNodeId,
            limit
        );
    }

    private SyncPullChangeDto mapPullChange(ResultSet rs) throws SQLException {
        try {
            return SyncPullChangeDto.builder()
                .changeId(rs.getLong("change_id"))
                .sourceEdgeNodeId(rs.getObject("source_edge_node_id", UUID.class))
                .aggregateType(rs.getString("aggregate_type"))
                .aggregateId(rs.getObject("aggregate_id", UUID.class))
                .operation(rs.getString("operation"))
                .entityVersion(rs.getLong("entity_version"))
                .payload(objectMapper.readTree(rs.getString("payload_json")))
                .createdAt(rs.getObject("created_at", Instant.class))
                .build();
        } catch (Exception ex) {
            throw new SQLException("Failed to parse sync change log payload JSON", ex);
        }
    }
}
