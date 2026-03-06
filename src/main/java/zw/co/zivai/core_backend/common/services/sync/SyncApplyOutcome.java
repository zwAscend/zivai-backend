package zw.co.zivai.core_backend.common.services.sync;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SyncApplyOutcome {
    private final String status;
    private final String message;
    private final Long appliedVersion;
}
