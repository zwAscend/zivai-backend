package zw.co.zivai.core_backend.dtos;

import java.time.OffsetDateTime;
import java.util.Map;

import lombok.Data;

@Data
public class AdminUpdateEdgeNodeRequest {
    private String deviceId;
    private String status;
    private OffsetDateTime lastSeenAt;
    private String softwareVersion;
    private Map<String, Object> metadata;
}
