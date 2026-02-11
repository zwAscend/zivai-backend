package zw.co.zivai.core_backend.dtos.resources;

import java.time.Instant;
import java.util.List;

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
    String status;
    List<String> tags;
    String contentType;
    String contentBody;
    Instant publishAt;
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
