package zw.co.zivai.core_backend.common.dtos.sync;

import java.time.Instant;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EdgeSyncStatusDto {
    private String role;
    private UUID edgeNodeId;
    private Long pendingOutbox;
    private Long failedOutbox;
    private Long lastPulledChangeId;
    private Instant lastSuccessfulPushAt;
    private Instant lastSuccessfulPullAt;
    private String cloudBaseUrl;
}
