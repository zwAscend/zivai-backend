package zw.co.zivai.core_backend.common.dtos.sync;

import java.util.List;
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
public class SyncPushResponse {
    private UUID edgeNodeId;
    private UUID batchId;
    private List<SyncPushItemResultDto> results;
}
