package zw.co.zivai.core_backend.models.lms;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import zw.co.zivai.core_backend.models.base.BaseEntity;

@Getter
@Setter
@Entity
@Table(name = "resources", schema = "lms")
public class Resource extends BaseEntity {
    @ManyToOne(optional = false)
    @JoinColumn(name = "school_id")
    private School school;

    @ManyToOne
    @JoinColumn(name = "subject_id")
    private Subject subject;

    @ManyToOne(optional = false)
    @JoinColumn(name = "uploaded_by")
    private User uploadedBy;

    @Column(nullable = false)
    private String name;

    @Column(name = "original_name", nullable = false)
    private String originalName;

    @Column(name = "mime_type", nullable = false)
    private String mimeType;

    @Column(name = "res_type", nullable = false)
    private String resType;

    @Column(name = "size_bytes", nullable = false)
    private Long sizeBytes;

    @Column(nullable = false)
    private String url;

    @Column(name = "storage_key")
    private String storageKey;

    @Column(name = "storage_path")
    private String storagePath;

    @Column(name = "downloads", nullable = false)
    private Integer downloads = 0;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    @Column(name = "status", nullable = false)
    private String status = "active";
}
