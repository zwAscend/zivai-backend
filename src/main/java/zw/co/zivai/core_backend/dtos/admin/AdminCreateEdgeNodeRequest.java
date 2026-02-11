package zw.co.zivai.core_backend.dtos.admin;

import java.time.OffsetDateTime;
import java.util.Map;

import lombok.Data;

@Data
public class AdminCreateEdgeNodeRequest {
    private String schoolId;
    private String deviceId;
    private String status;
    private OffsetDateTime lastSeenAt;
    private String softwareVersion;
    private Map<String, Object> metadata;
}
