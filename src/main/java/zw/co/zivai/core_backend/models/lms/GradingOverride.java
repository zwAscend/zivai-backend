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
@Table(name = "grading_overrides", schema = "lms")
public class GradingOverride extends BaseEntity {
    @ManyToOne(optional = false)
    @JoinColumn(name = "attempt_answer_id")
    private AttemptAnswer attemptAnswer;

    @ManyToOne(optional = false)
    @JoinColumn(name = "teacher_id")
    private User teacher;

    @Column(name = "old_score")
    private Double oldScore;

    @Column(name = "new_score", nullable = false)
    private Double newScore;

    private String reason;

    @Column(name = "overridden_at", nullable = false)
    private Instant overriddenAt = Instant.now();

    @Column(name = "linked_trace_id")
    private String linkedTraceId;
}
