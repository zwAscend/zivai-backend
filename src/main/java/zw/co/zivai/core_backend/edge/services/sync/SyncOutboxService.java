package zw.co.zivai.core_backend.edge.services.sync;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;

import lombok.RequiredArgsConstructor;
import zw.co.zivai.core_backend.common.configs.SyncProperties;
import zw.co.zivai.core_backend.common.dtos.sync.SyncChangeItemDto;
import zw.co.zivai.core_backend.common.repositories.sync.SyncOutboxJdbcRepository;
import zw.co.zivai.core_backend.common.services.sync.SyncContext;
import zw.co.zivai.core_backend.common.services.sync.SyncNodeIdentityService;
import zw.co.zivai.core_backend.common.services.sync.SyncPayloadMapper;
import zw.co.zivai.core_backend.common.models.base.BaseEntity;

@Service
@Profile("edge")
@RequiredArgsConstructor
public class SyncOutboxService {
    private final SyncProperties syncProperties;
    private final SyncNodeIdentityService syncNodeIdentityService;
    private final SyncPayloadMapper syncPayloadMapper;
    private final SyncOutboxJdbcRepository syncOutboxJdbcRepository;

    public void enqueue(BaseEntity entity) {
        if (!syncProperties.getEdge().isCaptureEnabled() || SyncContext.isOutboxSuppressed() || !syncPayloadMapper.supportsAggregate(entity)) {
            return;
        }

        Optional<UUID> edgeNodeId = syncNodeIdentityService.resolveEdgeNodeId();
        if (edgeNodeId.isEmpty() || entity.getId() == null) {
            return;
        }

        String operation = resolveOperation(entity);
        JsonNode payload = syncPayloadMapper.toPayload(entity);
        syncOutboxJdbcRepository.insert(
            edgeNodeId.get(),
            UUID.randomUUID(),
            syncPayloadMapper.aggregateTypeOf(entity),
            entity.getId(),
            operation,
            entity.getSyncVersion() == null ? 1L : entity.getSyncVersion(),
            payload
        );
    }

    public void enqueueAll(Collection<? extends BaseEntity> entities) {
        entities.forEach(this::enqueue);
    }

    public List<SyncChangeItemDto> reservePendingBatch(UUID edgeNodeId, UUID batchId, int limit) {
        return syncOutboxJdbcRepository.reservePendingBatch(edgeNodeId, batchId, limit);
    }

    public void markResult(UUID eventId, String status, String errorMessage) {
        syncOutboxJdbcRepository.markResult(eventId, status, errorMessage);
    }

    public long countByStatus(UUID edgeNodeId, String status) {
        return syncOutboxJdbcRepository.countByStatus(edgeNodeId, status);
    }

    public void failInProgressBatch(UUID batchId, String errorMessage) {
        syncOutboxJdbcRepository.failInProgressBatch(batchId, errorMessage);
    }

    private String resolveOperation(BaseEntity entity) {
        if (entity.getDeletedAt() != null) {
            return "DELETE";
        }
        Long version = entity.getSyncVersion();
        return version == null || version <= 1L ? "INSERT" : "UPDATE";
    }
}
