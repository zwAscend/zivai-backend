package zw.co.zivai.core_backend.common.services.sync;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import zw.co.zivai.core_backend.common.repositories.sync.SyncConflictLogJdbcRepository;
import zw.co.zivai.core_backend.common.models.base.BaseEntity;

@Service
@RequiredArgsConstructor
public class SyncApplyService {
    private final EntityManager entityManager;
    private final SyncPayloadMapper syncPayloadMapper;
    private final SyncConflictLogJdbcRepository syncConflictLogJdbcRepository;

    @Transactional
    public SyncApplyOutcome applyIncomingChange(
        UUID edgeNodeId,
        String conflictScope,
        String aggregateType,
        UUID aggregateId,
        String operation,
        long entityVersion,
        JsonNode payload
    ) {
        Class<? extends BaseEntity> entityType = syncPayloadMapper.resolveAggregateType(aggregateType)
            .orElse(null);
        if (entityType == null) {
            return SyncApplyOutcome.builder()
                .status("FAILED")
                .message("Unsupported aggregate type: " + aggregateType)
                .build();
        }

        BaseEntity existing = entityManager.find(entityType, aggregateId);
        long existingVersion = existing == null || existing.getSyncVersion() == null ? 0L : existing.getSyncVersion();

        if (existing != null && entityVersion < existingVersion) {
            logConflict(edgeNodeId, conflictScope, aggregateType, aggregateId, existingVersion, entityVersion, existing, payload, "STALE_VERSION");
            return SyncApplyOutcome.builder()
                .status("CONFLICT")
                .message("Incoming version is older than local version")
                .appliedVersion(existingVersion)
                .build();
        }

        if (existing != null && entityVersion == existingVersion) {
            return SyncApplyOutcome.builder()
                .status("SKIPPED")
                .message("Incoming version already applied")
                .appliedVersion(existingVersion)
                .build();
        }

        BaseEntity incoming = syncPayloadMapper.fromPayload(aggregateType, payload, existing);
        SyncContext.runWithoutOutbox(() -> {
            entityManager.merge(incoming);
            entityManager.flush();
        });

        long appliedVersion = incoming.getSyncVersion() == null ? entityVersion : incoming.getSyncVersion();
        return SyncApplyOutcome.builder()
            .status("APPLIED")
            .message(operation + " applied")
            .appliedVersion(appliedVersion)
            .build();
    }

    private void logConflict(
        UUID edgeNodeId,
        String conflictScope,
        String aggregateType,
        UUID aggregateId,
        long localVersion,
        long incomingVersion,
        BaseEntity existing,
        JsonNode incomingPayload,
        String conflictType
    ) {
        syncConflictLogJdbcRepository.logConflict(
            edgeNodeId,
            conflictScope,
            aggregateType,
            aggregateId,
            localVersion,
            incomingVersion,
            conflictType,
            syncPayloadMapper.toPayload(existing),
            incomingPayload,
            "MANUAL_REVIEW"
        );
    }
}
