package zw.co.zivai.core_backend.common.dtos.sync;

import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;

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
public class SyncPullChangeDto {
    private Long changeId;
    private UUID sourceEdgeNodeId;
    private String aggregateType;
    private UUID aggregateId;
    private String operation;
    private Long entityVersion;
    private JsonNode payload;
    private Instant createdAt;
}
