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
@Table(name = "answer_attachments", schema = "lms")
public class AnswerAttachment extends BaseEntity {
    @ManyToOne(optional = false)
    @JoinColumn(name = "attempt_answer_id")
    private AttemptAnswer attemptAnswer;

    @ManyToOne
    @JoinColumn(name = "resource_id")
    private Resource resource;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "storage_path")
    private String storagePath;

    @Column(name = "mime_type")
    private String mimeType;

    @Column(name = "size_bytes")
    private Long sizeBytes;
}
