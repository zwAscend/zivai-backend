package zw.co.zivai.core_backend.dtos;

import java.time.Instant;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ResourceCountsDto {
    long count;
    Instant lastUpdated;
    long documents;
    long images;
    long videos;
    long others;
}
