package zw.co.zivai.core_backend.edge.services.sync;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import zw.co.zivai.core_backend.common.configs.SyncProperties;
import zw.co.zivai.core_backend.common.dtos.sync.EdgeSyncStatusDto;
import zw.co.zivai.core_backend.common.dtos.sync.SyncChangeItemDto;
import zw.co.zivai.core_backend.common.dtos.sync.SyncPullResponse;
import zw.co.zivai.core_backend.common.dtos.sync.SyncPushRequest;
import zw.co.zivai.core_backend.common.dtos.sync.SyncPushResponse;
import zw.co.zivai.core_backend.common.repositories.sync.SyncCheckpointJdbcRepository;
import zw.co.zivai.core_backend.common.repositories.sync.SyncInboxJdbcRepository;
import zw.co.zivai.core_backend.common.services.sync.SyncApplyOutcome;
import zw.co.zivai.core_backend.common.services.sync.SyncApplyService;
import zw.co.zivai.core_backend.common.services.sync.SyncNodeIdentityService;

@Service
@Slf4j
@Profile("edge")
@RequiredArgsConstructor
public class EdgeSyncService {
    private final SyncProperties syncProperties;
    private final SyncNodeIdentityService syncNodeIdentityService;
    private final SyncOutboxService syncOutboxService;
    private final SyncApplyService syncApplyService;
    private final SyncCheckpointJdbcRepository syncCheckpointJdbcRepository;
    private final SyncInboxJdbcRepository syncInboxJdbcRepository;
    private final RestClient restClient;

    @Scheduled(fixedDelayString = "${app.sync.edge.poll-interval-ms:30000}")
    public void scheduledSync() {
        if (!syncProperties.getEdge().isWorkerEnabled()) {
            return;
        }
        runSyncCycle();
    }

    public void runSyncCycle() {
        if (syncProperties.getEdge().getCloudBaseUrl() == null || syncProperties.getEdge().getCloudBaseUrl().isBlank()) {
            return;
        }
        UUID edgeNodeId = syncNodeIdentityService.getRequiredEdgeNodeId();
        push(edgeNodeId);
        pull(edgeNodeId);
    }

    public EdgeSyncStatusDto getStatus() {
        UUID edgeNodeId = syncNodeIdentityService.resolveEdgeNodeId().orElse(null);
        if (edgeNodeId == null) {
            return EdgeSyncStatusDto.builder()
                .role("edge")
                .cloudBaseUrl(syncProperties.getEdge().getCloudBaseUrl())
                .pendingOutbox(0L)
                .failedOutbox(0L)
                .lastPulledChangeId(0L)
                .build();
        }

        Checkpoint checkpoint = syncCheckpointJdbcRepository.findByEdgeNodeId(edgeNodeId)
            .map(record -> new Checkpoint(
                record.lastPulledChangeId(),
                record.lastSuccessfulPushAt(),
                record.lastSuccessfulPullAt()
            ))
            .orElse(new Checkpoint(0L, null, null));

        return EdgeSyncStatusDto.builder()
            .role("edge")
            .edgeNodeId(edgeNodeId)
            .pendingOutbox(syncOutboxService.countByStatus(edgeNodeId, "PENDING"))
            .failedOutbox(syncOutboxService.countByStatus(edgeNodeId, "FAILED"))
            .lastPulledChangeId(checkpoint.lastPulledChangeId())
            .lastSuccessfulPushAt(checkpoint.lastSuccessfulPushAt())
            .lastSuccessfulPullAt(checkpoint.lastSuccessfulPullAt())
            .cloudBaseUrl(syncProperties.getEdge().getCloudBaseUrl())
            .build();
    }

    private void push(UUID edgeNodeId) {
        UUID batchId = UUID.randomUUID();
        List<SyncChangeItemDto> batch = syncOutboxService.reservePendingBatch(
            edgeNodeId,
            batchId,
            syncProperties.getEdge().getBatchSize()
        );
        if (batch.isEmpty()) {
            return;
        }

        SyncPushResponse response = restClient.post()
            .uri(syncProperties.getEdge().getCloudBaseUrl() + "/api/sync/push")
            .header("X-Edge-Node-Id", edgeNodeId.toString())
            .header("X-Edge-Auth-Key", valueOrEmpty(syncProperties.getEdge().getAuthKey()))
            .body(buildPushRequest(edgeNodeId, batchId, batch))
            .retrieve()
            .body(SyncPushResponse.class);

        if (response == null || response.getResults() == null) {
            log.warn("Cloud sync push returned no response items for batch {}", batchId);
            return;
        }

        response.getResults().forEach(result ->
            syncOutboxService.markResult(result.getEventId(), result.getStatus(), result.getMessage()));

        syncNodeIdentityService.touchPush(edgeNodeId, Instant.now(), batchId);
    }

    private void pull(UUID edgeNodeId) {
        long lastPulledChangeId = syncCheckpointJdbcRepository.findByEdgeNodeId(edgeNodeId)
            .map(SyncCheckpointJdbcRepository.CheckpointRecord::lastPulledChangeId)
            .orElse(0L);

        SyncPullResponse response = restClient.get()
            .uri(syncProperties.getEdge().getCloudBaseUrl() + "/api/sync/pull?afterChangeId={afterChangeId}",
                lastPulledChangeId)
            .header("X-Edge-Node-Id", edgeNodeId.toString())
            .header("X-Edge-Auth-Key", valueOrEmpty(syncProperties.getEdge().getAuthKey()))
            .retrieve()
            .body(SyncPullResponse.class);

        if (response == null || response.getChanges() == null || response.getChanges().isEmpty()) {
            return;
        }

        UUID batchId = UUID.randomUUID();
        long maxChangeId = lastPulledChangeId;
        for (var change : response.getChanges()) {
            syncInboxJdbcRepository.insertReceived(
                edgeNodeId,
                change.getChangeId(),
                change.getAggregateType(),
                change.getAggregateId(),
                change.getOperation(),
                change.getEntityVersion(),
                change.getPayload()
            );

            SyncApplyOutcome outcome = syncApplyService.applyIncomingChange(
                edgeNodeId,
                "EDGE_PULL",
                change.getAggregateType(),
                change.getAggregateId(),
                change.getOperation(),
                change.getEntityVersion() == null ? 1L : change.getEntityVersion(),
                change.getPayload()
            );

            syncInboxJdbcRepository.markResult(
                edgeNodeId,
                change.getChangeId(),
                outcome.getStatus(),
                outcome.getMessage()
            );
            maxChangeId = Math.max(maxChangeId, change.getChangeId());
        }

        syncNodeIdentityService.touchPull(edgeNodeId, Instant.now(), batchId, maxChangeId);
    }

    private SyncPushRequest buildPushRequest(
        UUID edgeNodeId,
        UUID batchId,
        List<SyncChangeItemDto> batch
    ) {
        SyncPushRequest request = new SyncPushRequest();
        request.setEdgeNodeId(edgeNodeId);
        request.setBatchId(batchId);
        request.setChanges(batch);
        return request;
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }

    private record Checkpoint(long lastPulledChangeId, Instant lastSuccessfulPushAt, Instant lastSuccessfulPullAt) {
    }
}
