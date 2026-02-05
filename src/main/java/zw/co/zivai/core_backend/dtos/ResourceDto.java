package zw.co.zivai.core_backend.dtos;

import java.time.Instant;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ResourceDto {
    String id;
    String name;
    String originalName;
    String mimeType;
    String type;
    long size;
    String url;
    String key;
    String path;
    int downloads;
    String subject;
    Instant createdAt;
    Instant updatedAt;
    UploadedBy uploadedBy;

    @Value
    @Builder
    public static class UploadedBy {
        String id;
        String firstName;
        String lastName;
    }
}
