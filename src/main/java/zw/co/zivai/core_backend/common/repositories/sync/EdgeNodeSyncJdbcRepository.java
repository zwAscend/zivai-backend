package zw.co.zivai.core_backend.common.repositories.sync;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class EdgeNodeSyncJdbcRepository {
    private final JdbcTemplate jdbcTemplate;

    public Optional<UUID> findFirstSyncEnabledEdgeNodeId() {
        List<UUID> nodeIds = jdbcTemplate.query(
            """
            SELECT id
            FROM edge.edge_nodes
            WHERE deleted_at IS NULL
              AND sync_enabled = TRUE
            ORDER BY COALESCE(registered_at, created_at, NOW()), id
            LIMIT 1
            """,
            (rs, rowNum) -> rs.getObject("id", UUID.class)
        );
        return nodeIds.stream().findFirst();
    }

    public EdgeNodeAuthRecord findEdgeNodeAuthInfo(UUID edgeNodeId) {
        return jdbcTemplate.queryForObject(
            """
            SELECT id, school_id, auth_key_hash, sync_enabled
            FROM edge.edge_nodes
            WHERE id = ? AND deleted_at IS NULL
            """,
            (rs, rowNum) -> mapAuthInfo(rs),
            edgeNodeId
        );
    }

    public List<AdminEdgeNodeRow> findAdminEdgeNodes() {
        return jdbcTemplate.query(selectSql(null), this::mapAdminEdgeNode);
    }

    public Optional<AdminEdgeNodeRow> findAdminEdgeNodeById(UUID id) {
        return jdbcTemplate.query(selectSql("AND n.id = ?"), this::mapAdminEdgeNode, id)
            .stream()
            .findFirst();
    }

    public void touchPush(UUID edgeNodeId, Instant when) {
        jdbcTemplate.update(
            """
            UPDATE edge.edge_nodes
            SET last_push_at = ?, last_sync_at = ?
            WHERE id = ?
            """,
            when,
            when,
            edgeNodeId
        );
    }

    public void touchPull(UUID edgeNodeId, Instant when) {
        jdbcTemplate.update(
            """
            UPDATE edge.edge_nodes
            SET last_pull_at = ?, last_sync_at = ?
            WHERE id = ?
            """,
            when,
            when,
            edgeNodeId
        );
    }

    private String selectSql(String extraWhereClause) {
        String sql = """
            SELECT
              n.id,
              n.device_id,
              n.status,
              n.last_seen_at,
              n.software_version,
              n.metadata::text AS metadata_json,
              COALESCE(deployments.active_deployments, 0) AS active_deployments,
              COALESCE(outbox.pending_outbox_events, 0) AS pending_outbox_events,
              COALESCE(inbox.failed_inbox_events, 0) AS failed_inbox_events
            FROM edge.edge_nodes n
            LEFT JOIN LATERAL (
              SELECT COUNT(*)::bigint AS active_deployments
              FROM edge.edge_model_deployments d
              WHERE d.edge_node_id = n.id
                AND d.deleted_at IS NULL
                AND d.is_active = TRUE
            ) deployments ON TRUE
            LEFT JOIN LATERAL (
              SELECT COUNT(*)::bigint AS pending_outbox_events
              FROM edge.sync_outbox o
              WHERE o.edge_node_id = n.id
                AND o.status IN ('PENDING', 'IN_PROGRESS', 'FAILED', 'CONFLICT')
            ) outbox ON TRUE
            LEFT JOIN LATERAL (
              SELECT COUNT(*)::bigint AS failed_inbox_events
              FROM edge.sync_inbox i
              WHERE i.receiver_edge_node_id = n.id
                AND i.status = 'FAILED'
            ) inbox ON TRUE
            WHERE n.deleted_at IS NULL
            """;
        if (extraWhereClause != null && !extraWhereClause.isBlank()) {
            sql += "\n" + extraWhereClause.trim();
        }
        return sql + "\nORDER BY COALESCE(n.last_seen_at, n.updated_at, n.created_at) DESC";
    }

    private AdminEdgeNodeRow mapAdminEdgeNode(ResultSet rs, int rowNum) throws java.sql.SQLException {
        return new AdminEdgeNodeRow(
            rs.getObject("id", UUID.class),
            rs.getString("device_id"),
            rs.getString("status"),
            rs.getObject("last_seen_at", java.time.OffsetDateTime.class),
            rs.getString("software_version"),
            rs.getLong("active_deployments"),
            rs.getLong("pending_outbox_events"),
            rs.getLong("failed_inbox_events"),
            rs.getString("metadata_json")
        );
    }

    private EdgeNodeAuthRecord mapAuthInfo(ResultSet rs) throws SQLException {
        return new EdgeNodeAuthRecord(
            rs.getObject("id", UUID.class),
            rs.getObject("school_id", UUID.class),
            rs.getString("auth_key_hash"),
            rs.getBoolean("sync_enabled")
        );
    }

    public record EdgeNodeAuthRecord(UUID edgeNodeId, UUID schoolId, String authKeyHash, boolean syncEnabled) {
    }

    public record AdminEdgeNodeRow(
        UUID id,
        String deviceId,
        String status,
        java.time.OffsetDateTime lastSeenAt,
        String softwareVersion,
        long activeDeployments,
        long pendingOutboxEvents,
        long failedInboxEvents,
        String metadataJson
    ) {
    }
}
