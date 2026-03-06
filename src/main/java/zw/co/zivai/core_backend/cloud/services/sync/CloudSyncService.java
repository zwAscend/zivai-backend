package zw.co.zivai.core_backend.cloud.services.sync;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import zw.co.zivai.core_backend.common.configs.SyncProperties;
import zw.co.zivai.core_backend.common.dtos.sync.SyncPullChangeDto;
import zw.co.zivai.core_backend.common.dtos.sync.SyncPullResponse;
import zw.co.zivai.core_backend.common.dtos.sync.SyncPushItemResultDto;
import zw.co.zivai.core_backend.common.dtos.sync.SyncPushRequest;
import zw.co.zivai.core_backend.common.dtos.sync.SyncPushResponse;
import zw.co.zivai.core_backend.common.repositories.sync.SyncChangeLogJdbcRepository;
import zw.co.zivai.core_backend.common.services.sync.SyncApplyOutcome;
import zw.co.zivai.core_backend.common.services.sync.SyncApplyService;
import zw.co.zivai.core_backend.common.services.sync.SyncNodeIdentityService;

@Service
@Profile("cloud")
@RequiredArgsConstructor
public class CloudSyncService {
    private final SyncProperties syncProperties;
    private final SyncNodeIdentityService syncNodeIdentityService;
    private final SyncApplyService syncApplyService;
    private final SyncChangeLogJdbcRepository syncChangeLogJdbcRepository;
    private final PasswordEncoder passwordEncoder;

    public SyncPushResponse acceptPush(SyncPushRequest request, String authKey) {
        validateEdgeNode(request.getEdgeNodeId(), authKey);
        UUID batchId = request.getBatchId() == null ? UUID.randomUUID() : request.getBatchId();
        SyncNodeIdentityService.EdgeNodeAuthInfo nodeInfo = syncNodeIdentityService.getEdgeNodeAuthInfo(request.getEdgeNodeId());

        List<SyncPushItemResultDto> results = new ArrayList<>();
        if (request.getChanges() != null) {
            for (var change : request.getChanges()) {
                SyncApplyOutcome outcome = syncApplyService.applyIncomingChange(
                    request.getEdgeNodeId(),
                    "CLOUD_PUSH",
                    change.getAggregateType(),
                    change.getAggregateId(),
                    change.getOperation(),
                    change.getEntityVersion() == null ? 1L : change.getEntityVersion(),
                    change.getPayload()
                );
                Long changeId = null;
                if ("APPLIED".equals(outcome.getStatus())) {
                    changeId = syncChangeLogJdbcRepository.appendChange(
                        request.getEdgeNodeId(),
                        change.getAggregateType(),
                        change.getAggregateId(),
                        change.getOperation(),
                        change.getEntityVersion(),
                        change.getPayload(),
                        nodeInfo.schoolId()
                    );
                }
                results.add(
                    SyncPushItemResultDto.builder()
                        .eventId(change.getEventId())
                        .status("FAILED".equals(outcome.getStatus()) ? "FAILED" : ("CONFLICT".equals(outcome.getStatus()) ? "CONFLICT" : "SYNCED"))
                        .message(outcome.getMessage())
                        .changeId(changeId)
                        .appliedVersion(outcome.getAppliedVersion())
                        .build()
                );
            }
        }

        return SyncPushResponse.builder()
            .edgeNodeId(request.getEdgeNodeId())
            .batchId(batchId)
            .results(results)
            .build();
    }

    public SyncPullResponse pullChanges(UUID edgeNodeId, long afterChangeId, String authKey) {
        validateEdgeNode(edgeNodeId, authKey);
        SyncNodeIdentityService.EdgeNodeAuthInfo nodeInfo = syncNodeIdentityService.getEdgeNodeAuthInfo(edgeNodeId);
        List<SyncPullChangeDto> changes = syncChangeLogJdbcRepository.findAfter(
            afterChangeId,
            nodeInfo.schoolId(),
            edgeNodeId,
            syncProperties.getCloud().getPullBatchSize()
        );

        long toChangeId = changes.stream()
            .map(SyncPullChangeDto::getChangeId)
            .max(Long::compareTo)
            .orElse(afterChangeId);

        return SyncPullResponse.builder()
            .edgeNodeId(edgeNodeId)
            .fromChangeId(afterChangeId)
            .toChangeId(toChangeId)
            .changes(changes)
            .build();
    }

    private void validateEdgeNode(UUID edgeNodeId, String authKey) {
        SyncNodeIdentityService.EdgeNodeAuthInfo nodeInfo = syncNodeIdentityService.getEdgeNodeAuthInfo(edgeNodeId);
        if (!nodeInfo.syncEnabled()) {
            throw new IllegalStateException("Sync is disabled for edge node " + edgeNodeId);
        }
        if (nodeInfo.authKeyHash() != null && !nodeInfo.authKeyHash().isBlank()) {
            if (authKey == null || !passwordEncoder.matches(authKey, nodeInfo.authKeyHash())) {
                throw new IllegalStateException("Invalid sync credentials for edge node " + edgeNodeId);
            }
            return;
        }
        if (syncProperties.getCloud().isRequireAuth()) {
            throw new IllegalStateException("Sync credentials are required for edge node " + edgeNodeId);
        }
    }

}
