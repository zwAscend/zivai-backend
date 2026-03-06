package zw.co.zivai.core_backend.common.dtos.sync;

import java.util.List;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SyncPushRequest {
    private UUID edgeNodeId;
    private UUID batchId;
    private List<SyncChangeItemDto> changes;
}
