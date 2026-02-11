package zw.co.zivai.core_backend.dtos.resources;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import lombok.Data;

@Data
public class UpdateResourceRequest {
    private UUID subjectId;
    private String name;
    private String originalName;
    private String mimeType;
    private String resType;
    private Long sizeBytes;
    private String url;
    private String storageKey;
    private String storagePath;
    private List<String> tags;
    private String contentType;
    private String contentBody;
    private Instant publishAt;
    private Integer displayOrder;
    private String status;
}
