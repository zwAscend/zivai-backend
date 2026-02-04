package zw.co.zivai.core_backend.models.lms;

import java.time.Instant;

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
@Table(name = "assessment_attempts", schema = "lms")
public class AssessmentAttempt extends BaseEntity {
    @ManyToOne(optional = false)
    @JoinColumn(name = "assessment_enrollment_id")
    private AssessmentEnrollment assessmentEnrollment;

    @Column(name = "attempt_number", nullable = false)
    private Integer attemptNumber = 1;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt = Instant.now();

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "total_score")
    private Double totalScore;

    @Column(name = "max_score")
    private Double maxScore;

    @Column(name = "final_score")
    private Double finalScore;

    @Column(name = "final_grade")
    private String finalGrade;

    @Column(name = "submission_type")
    private String submissionType;

    @Column(name = "original_filename")
    private String originalFilename;

    @Column(name = "file_type")
    private String fileType;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Column(name = "storage_path")
    private String storagePath;

    @Column(name = "grading_status_code", nullable = false)
    private String gradingStatusCode;

    @Column(name = "ai_confidence")
    private Double aiConfidence;

    @Column(name = "attempt_trace_id")
    private String attemptTraceId;
}
