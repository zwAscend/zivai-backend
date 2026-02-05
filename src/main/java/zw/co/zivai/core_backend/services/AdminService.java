package zw.co.zivai.core_backend.services;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import zw.co.zivai.core_backend.dtos.AdminCreateEdgeNodeRequest;
import zw.co.zivai.core_backend.dtos.AdminEdgeNodeDto;
import zw.co.zivai.core_backend.dtos.AdminSummaryDto;
import zw.co.zivai.core_backend.dtos.AdminSummaryRecentUserDto;
import zw.co.zivai.core_backend.dtos.AdminUpdateEdgeNodeRequest;
import zw.co.zivai.core_backend.exceptions.BadRequestException;
import zw.co.zivai.core_backend.exceptions.NotFoundException;
import zw.co.zivai.core_backend.models.lms.User;
import zw.co.zivai.core_backend.repositories.SchoolRepository;
import zw.co.zivai.core_backend.repositories.UserRepository;

@Service
@Slf4j
@RequiredArgsConstructor
public class AdminService {
    private final UserRepository userRepository;
    private final SchoolRepository schoolRepository;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public AdminSummaryDto getSummary() {
        final String summarySql = """
            SELECT
              (SELECT COUNT(*)::bigint FROM lms.users u WHERE u.deleted_at IS NULL) AS total_users,
              (SELECT COUNT(*)::bigint FROM lms.users u WHERE u.deleted_at IS NULL AND u.is_active = TRUE) AS active_users,
              (SELECT COUNT(*)::bigint
                 FROM lms.users u
                 JOIN lms.user_roles ur ON ur.user_id = u.id
                 JOIN lookups.roles r ON r.id = ur.role_id
                WHERE u.deleted_at IS NULL AND r.code = 'student') AS total_students,
              (SELECT COUNT(*)::bigint
                 FROM lms.users u
                 JOIN lms.user_roles ur ON ur.user_id = u.id
                 JOIN lookups.roles r ON r.id = ur.role_id
                WHERE u.deleted_at IS NULL AND r.code = 'teacher') AS total_teachers,
              (SELECT COUNT(*)::bigint
                 FROM lms.users u
                 JOIN lms.user_roles ur ON ur.user_id = u.id
                 JOIN lookups.roles r ON r.id = ur.role_id
                WHERE u.deleted_at IS NULL AND r.code = 'admin') AS total_admins,
              (SELECT COUNT(*)::bigint FROM lms.subjects s WHERE s.deleted_at IS NULL) AS total_subjects,
              (SELECT COUNT(*)::bigint FROM lms.subjects s WHERE s.deleted_at IS NULL AND s.is_active = TRUE) AS active_subjects,
              (SELECT COUNT(*)::bigint FROM lms.classes c WHERE c.deleted_at IS NULL) AS total_classes,
              (SELECT COUNT(*)::bigint FROM lms.schools s WHERE s.deleted_at IS NULL) AS total_schools,
              (SELECT COUNT(*)::bigint FROM lms.class_subjects cs WHERE cs.deleted_at IS NULL) AS total_class_subject_links
            """;

        AdminSummaryDto baseSummary = jdbcTemplate.queryForObject(summarySql, (rs, rowNum) -> {
            long totalUsers = rs.getLong("total_users");
            long activeUsers = rs.getLong("active_users");
            return AdminSummaryDto.builder()
                .totalUsers(totalUsers)
                .activeUsers(activeUsers)
                .inactiveUsers(Math.max(totalUsers - activeUsers, 0))
                .totalStudents(rs.getLong("total_students"))
                .totalTeachers(rs.getLong("total_teachers"))
                .totalAdmins(rs.getLong("total_admins"))
                .totalSubjects(rs.getLong("total_subjects"))
                .activeSubjects(rs.getLong("active_subjects"))
                .totalClasses(rs.getLong("total_classes"))
                .totalSchools(rs.getLong("total_schools"))
                .totalClassSubjectLinks(rs.getLong("total_class_subject_links"))
                .build();
        });

        List<AdminSummaryRecentUserDto> recentUsers = userRepository.findAllByDeletedAtIsNull().stream()
            .sorted(Comparator.comparing(User::getCreatedAt).reversed())
            .limit(8)
            .map(this::toRecentUserDto)
            .toList();

        if (baseSummary == null) {
            return AdminSummaryDto.builder().recentUsers(recentUsers).build();
        }

        return AdminSummaryDto.builder()
            .totalUsers(baseSummary.getTotalUsers())
            .activeUsers(baseSummary.getActiveUsers())
            .inactiveUsers(baseSummary.getInactiveUsers())
            .totalStudents(baseSummary.getTotalStudents())
            .totalTeachers(baseSummary.getTotalTeachers())
            .totalAdmins(baseSummary.getTotalAdmins())
            .totalSubjects(baseSummary.getTotalSubjects())
            .activeSubjects(baseSummary.getActiveSubjects())
            .totalClasses(baseSummary.getTotalClasses())
            .totalSchools(baseSummary.getTotalSchools())
            .totalClassSubjectLinks(baseSummary.getTotalClassSubjectLinks())
            .recentUsers(recentUsers)
            .build();
    }

    public List<AdminEdgeNodeDto> getEdgeNodes() {
        try {
            return jdbcTemplate.query(edgeNodeSelectSql(null), this::mapEdgeNode);
        } catch (DataAccessException ex) {
            log.warn("Unable to query edge node metrics for admin dashboard: {}", ex.getMessage());
            return List.of();
        }
    }

    public AdminEdgeNodeDto registerEdgeNode(AdminCreateEdgeNodeRequest request) {
        if (request == null || request.getDeviceId() == null || request.getDeviceId().isBlank()) {
            throw new BadRequestException("deviceId is required");
        }

        UUID schoolId = resolveSchoolId(request.getSchoolId());
        String normalizedStatus = normalizeStatus(request.getStatus());
        String metadataJson = serializeMetadata(request.getMetadata());

        String insertSql = """
            INSERT INTO edge.edge_nodes (
              school_id, device_id, status, last_seen_at, software_version, metadata
            ) VALUES (?, ?, ?, ?, ?, CAST(? AS jsonb))
            RETURNING id
            """;

        try {
            UUID edgeNodeId = jdbcTemplate.queryForObject(
                insertSql,
                UUID.class,
                schoolId,
                request.getDeviceId().trim(),
                normalizedStatus,
                request.getLastSeenAt(),
                request.getSoftwareVersion(),
                metadataJson
            );

            if (edgeNodeId == null) {
                throw new BadRequestException("Failed to register edge node");
            }

            List<AdminEdgeNodeDto> nodes = jdbcTemplate.query(edgeNodeSelectSql("AND n.id = ?"), this::mapEdgeNode, edgeNodeId);
            if (nodes.isEmpty()) {
                throw new BadRequestException("Edge node created but could not be loaded");
            }
            return nodes.get(0);
        } catch (DataIntegrityViolationException ex) {
            throw new BadRequestException("Edge node already exists for this school/device or violates constraints");
        } catch (DataAccessException ex) {
            throw new BadRequestException("Failed to register edge node: " + ex.getMostSpecificCause().getMessage());
        }
    }

    public AdminEdgeNodeDto updateEdgeNode(UUID id, AdminUpdateEdgeNodeRequest request) {
        if (request == null) {
            throw new BadRequestException("Request body is required");
        }
        Map<String, Object> existing = jdbcTemplate.queryForList(
            """
            SELECT id, device_id, status, last_seen_at, software_version, metadata::text AS metadata_json
            FROM edge.edge_nodes
            WHERE id = ? AND deleted_at IS NULL
            """,
            id
        ).stream().findFirst().orElse(null);

        if (existing == null) {
            throw new NotFoundException("Edge node not found: " + id);
        }

        String deviceId = request.getDeviceId() != null && !request.getDeviceId().isBlank()
            ? request.getDeviceId().trim()
            : String.valueOf(existing.get("device_id"));
        String status = request.getStatus() != null
            ? normalizeStatus(request.getStatus())
            : String.valueOf(existing.get("status"));
        Object lastSeenValue = request.getLastSeenAt() != null ? request.getLastSeenAt() : existing.get("last_seen_at");
        String softwareVersion = request.getSoftwareVersion() != null
            ? request.getSoftwareVersion().trim()
            : (existing.get("software_version") == null ? null : String.valueOf(existing.get("software_version")));
        String metadataJson = request.getMetadata() != null
            ? serializeMetadata(request.getMetadata())
            : (existing.get("metadata_json") == null ? "{}" : String.valueOf(existing.get("metadata_json")));

        try {
            int updated = jdbcTemplate.update(
                """
                UPDATE edge.edge_nodes
                SET device_id = ?, status = ?, last_seen_at = ?, software_version = ?, metadata = CAST(? AS jsonb)
                WHERE id = ? AND deleted_at IS NULL
                """,
                deviceId,
                status,
                lastSeenValue,
                softwareVersion,
                metadataJson,
                id
            );

            if (updated == 0) {
                throw new NotFoundException("Edge node not found: " + id);
            }

            return getEdgeNodeById(id);
        } catch (DataIntegrityViolationException ex) {
            throw new BadRequestException("Failed to update edge node due to unique/constraint violation");
        } catch (DataAccessException ex) {
            throw new BadRequestException("Failed to update edge node: " + ex.getMostSpecificCause().getMessage());
        }
    }

    public void deleteEdgeNode(UUID id) {
        int updated = jdbcTemplate.update(
            """
            UPDATE edge.edge_nodes
            SET deleted_at = NOW(), status = 'retired'
            WHERE id = ? AND deleted_at IS NULL
            """,
            id
        );

        if (updated == 0) {
            throw new NotFoundException("Edge node not found: " + id);
        }
    }

    private UUID resolveSchoolId(String schoolIdValue) {
        if (schoolIdValue != null && !schoolIdValue.isBlank()) {
            UUID schoolId;
            try {
                schoolId = UUID.fromString(schoolIdValue.trim());
            } catch (IllegalArgumentException ex) {
                throw new BadRequestException("Invalid schoolId");
            }
            schoolRepository.findByIdAndDeletedAtIsNull(schoolId)
                .orElseThrow(() -> new BadRequestException("School not found"));
            return schoolId;
        }

        List<UUID> schoolIds = schoolRepository.findAllByDeletedAtIsNull().stream()
            .map(school -> school.getId())
            .toList();

        if (schoolIds.isEmpty()) {
            throw new BadRequestException("No school found. Create a school before registering edge nodes.");
        }
        if (schoolIds.size() > 1) {
            throw new BadRequestException("Multiple schools detected. Provide schoolId when registering edge nodes.");
        }
        return schoolIds.get(0);
    }

    private String normalizeStatus(String statusValue) {
        if (statusValue == null || statusValue.isBlank()) {
            return "active";
        }
        String normalized = statusValue.trim().toLowerCase(Locale.ROOT);
        if (!Arrays.asList("active", "inactive", "retired").contains(normalized)) {
            throw new BadRequestException("status must be one of: active, inactive, retired");
        }
        return normalized;
    }

    private String serializeMetadata(Object metadata) {
        if (metadata == null) {
            return "{}";
        }
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException ex) {
            throw new BadRequestException("metadata must be valid JSON");
        }
    }

    private String edgeNodeSelectSql(String extraWhereClause) {
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
                AND o.sent_at IS NULL
            ) outbox ON TRUE
            LEFT JOIN LATERAL (
              SELECT COUNT(*)::bigint AS failed_inbox_events
              FROM edge.sync_inbox i
              WHERE i.receiver_edge_node_id = n.id
                AND i.status = 'failed'
            ) inbox ON TRUE
            WHERE n.deleted_at IS NULL
            """;

        if (extraWhereClause != null && !extraWhereClause.isBlank()) {
            sql = sql + "\n" + extraWhereClause.trim();
        }

        return sql + "\nORDER BY COALESCE(n.last_seen_at, n.updated_at, n.created_at) DESC";
    }

    private AdminEdgeNodeDto getEdgeNodeById(UUID id) {
        List<AdminEdgeNodeDto> nodes = jdbcTemplate.query(edgeNodeSelectSql("AND n.id = ?"), this::mapEdgeNode, id);
        if (nodes.isEmpty()) {
            throw new NotFoundException("Edge node not found: " + id);
        }
        return nodes.get(0);
    }

    private AdminEdgeNodeDto mapEdgeNode(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        String metadataJson = rs.getString("metadata_json");
        String location = null;
        String ipAddress = null;
        String hardwareModel = null;
        String serialNumber = null;
        String comments = null;

        if (metadataJson != null && !metadataJson.isBlank()) {
            try {
                JsonNode metadataNode = objectMapper.readTree(metadataJson);
                location = textValue(metadataNode, "location");
                ipAddress = textValue(metadataNode, "ipAddress");
                hardwareModel = textValue(metadataNode, "hardwareModel");
                serialNumber = textValue(metadataNode, "serialNumber");
                comments = textValue(metadataNode, "comments");
            } catch (JsonProcessingException ex) {
                log.debug("Failed to parse edge node metadata for {}: {}", rs.getObject("id"), ex.getMessage());
            }
        }

        return AdminEdgeNodeDto.builder()
            .id(rs.getObject("id", java.util.UUID.class))
            .deviceId(rs.getString("device_id"))
            .status(rs.getString("status"))
            .lastSeenAt(rs.getObject("last_seen_at", java.time.OffsetDateTime.class))
            .softwareVersion(rs.getString("software_version"))
            .activeDeployments(rs.getLong("active_deployments"))
            .pendingOutboxEvents(rs.getLong("pending_outbox_events"))
            .failedInboxEvents(rs.getLong("failed_inbox_events"))
            .location(location)
            .ipAddress(ipAddress)
            .hardwareModel(hardwareModel)
            .serialNumber(serialNumber)
            .comments(comments)
            .build();
    }

    private String textValue(JsonNode node, String field) {
        if (node == null || !node.has(field) || node.get(field).isNull()) {
            return null;
        }
        String value = node.get(field).asText(null);
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }

    private AdminSummaryRecentUserDto toRecentUserDto(User user) {
        Set<String> roles = user.getRoles().stream()
            .map(role -> role.getCode())
            .collect(Collectors.toSet());

        return AdminSummaryRecentUserDto.builder()
            .id(user.getId())
            .firstName(user.getFirstName())
            .lastName(user.getLastName())
            .email(user.getEmail())
            .active(user.isActive())
            .roles(roles)
            .createdAt(user.getCreatedAt())
            .build();
    }
}
