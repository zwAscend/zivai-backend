package zw.co.zivai.core_backend.dtos.resources;

import java.time.Instant;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ResourceRecentDto {
    String id;
    String name;
    String type;
    Instant createdAt;
    SubjectRef subject;
    UserRef uploadedBy;

    @Value
    @Builder
    public static class SubjectRef {
        String id;
        String name;
        String code;
    }

    @Value
    @Builder
    public static class UserRef {
        String id;
        String firstName;
        String lastName;
    }
}
