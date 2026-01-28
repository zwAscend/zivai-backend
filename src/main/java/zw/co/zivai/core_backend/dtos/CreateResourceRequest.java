package zw.co.zivai.core_backend.dtos;

import java.util.UUID;

import lombok.Data;

@Data
public class CreateResourceRequest {
    private UUID schoolId;
    private UUID subjectId;
    private UUID uploadedBy;
    private String name;
    private String originalName;
    private String mimeType;
    private String resType;
    private Long sizeBytes;
    private String url;
    private String storageKey;
    private String storagePath;
    private Integer displayOrder = 0;
    private String status = "active";
}
