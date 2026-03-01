package zw.co.zivai.core_backend.models.lms.students;

import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import zw.co.zivai.core_backend.models.base.BaseEntity;
import zw.co.zivai.core_backend.models.lms.subjects.Subject;
import zw.co.zivai.core_backend.models.lms.users.User;

@Getter
@Setter
@Entity
@Table(name = "student_documents", schema = "lms")
public class StudentDocument {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "student_id")
    private User student;

    @Column(name = "document_type_code", nullable = false)
    private String documentTypeCode;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "storage_path", nullable = false)
    private String storagePath;

    @Column(name = "mime_type")
    private String mimeType;

    @Column(name = "uploaded_at", nullable = false)
    private Instant uploadedAt;

    @ManyToOne
    @JoinColumn(name = "uploaded_by")
    private User uploadedBy;

    @Column(name = "metadata")
    @JdbcTypeCode(SqlTypes.JSON)
    private JsonNode metadata;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "origin_node_id")
    private UUID originNodeId;

    @Column(name = "sync_version", nullable = false)
    private Long syncVersion = 0L;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @PrePersist
    void onCreate() {
        if (uploadedAt == null) {
            uploadedAt = Instant.now();
        }
        if (syncVersion == null) {
            syncVersion = 0L;
        }
    }

    @PreUpdate
    void onUpdate() {
        if (syncVersion == null) {
            syncVersion = 0L;
        }
        syncVersion += 1;
    }

    @Getter
    @Setter
    @Entity
    @Table(name = "mastery_snapshots", schema = "lms")
    public static class MasterySnapshot extends BaseEntity {
        @ManyToOne(optional = false)
        @JoinColumn(name = "student_id")
        private User student;

        @ManyToOne(optional = false)
        @JoinColumn(name = "subject_id")
        private Subject subject;

        @Column(name = "snapshot_time", nullable = false)
        private Instant snapshotTime = Instant.now();

        @Column(nullable = false)
        private String source;

        @Column(name = "average_mastery")
        private Double averageMastery;

        @Column(name = "risk_level_code")
        private String riskLevelCode;
    }
}
