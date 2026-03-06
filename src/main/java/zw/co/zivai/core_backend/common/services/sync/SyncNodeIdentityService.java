package zw.co.zivai.core_backend.common.services.sync;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import zw.co.zivai.core_backend.common.configs.SyncProperties;
import zw.co.zivai.core_backend.common.repositories.sync.EdgeNodeSyncJdbcRepository;
import zw.co.zivai.core_backend.common.repositories.sync.SyncCheckpointJdbcRepository;

@Service
@RequiredArgsConstructor
public class SyncNodeIdentityService {
    private final SyncProperties syncProperties;
    private final EdgeNodeSyncJdbcRepository edgeNodeSyncJdbcRepository;
    private final SyncCheckpointJdbcRepository syncCheckpointJdbcRepository;

    public Optional<UUID> resolveEdgeNodeId() {
        if (syncProperties.getEdge().getNodeId() != null) {
            return Optional.of(syncProperties.getEdge().getNodeId());
        }

        return edgeNodeSyncJdbcRepository.findFirstSyncEnabledEdgeNodeId();
    }

    public UUID getRequiredEdgeNodeId() {
        return resolveEdgeNodeId()
            .orElseThrow(() -> new IllegalStateException("No sync-enabled edge node is configured for this runtime."));
    }

    public void touchPush(UUID edgeNodeId, Instant when, UUID batchId) {
        syncCheckpointJdbcRepository.touchPush(edgeNodeId, when, batchId);
        edgeNodeSyncJdbcRepository.touchPush(edgeNodeId, when);
    }

    public void touchPull(UUID edgeNodeId, Instant when, UUID batchId, long lastPulledChangeId) {
        syncCheckpointJdbcRepository.touchPull(edgeNodeId, when, batchId, lastPulledChangeId);
        edgeNodeSyncJdbcRepository.touchPull(edgeNodeId, when);
    }

    public EdgeNodeAuthInfo getEdgeNodeAuthInfo(UUID edgeNodeId) {
        EdgeNodeSyncJdbcRepository.EdgeNodeAuthRecord record = edgeNodeSyncJdbcRepository.findEdgeNodeAuthInfo(edgeNodeId);
        return new EdgeNodeAuthInfo(
            record.edgeNodeId(),
            record.schoolId(),
            record.authKeyHash(),
            record.syncEnabled()
        );
    }

    public record EdgeNodeAuthInfo(UUID edgeNodeId, UUID schoolId, String authKeyHash, boolean syncEnabled) {
    }
}
