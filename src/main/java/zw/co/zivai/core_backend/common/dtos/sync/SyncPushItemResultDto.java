package zw.co.zivai.core_backend.common.dtos.sync;

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
public class SyncPushItemResultDto {
    private UUID eventId;
    private String status;
    private String message;
    private Long changeId;
    private Long appliedVersion;
}
