package zw.co.zivai.core_backend.dtos.admin;

import java.time.OffsetDateTime;
import java.util.UUID;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminEdgeNodeDto {
    private UUID id;
    private String deviceId;
    private String status;
    private OffsetDateTime lastSeenAt;
    private String softwareVersion;
    private long activeDeployments;
    private long pendingOutboxEvents;
    private long failedInboxEvents;
    private String location;
    private String ipAddress;
    private String hardwareModel;
    private String serialNumber;
    private String comments;
}
