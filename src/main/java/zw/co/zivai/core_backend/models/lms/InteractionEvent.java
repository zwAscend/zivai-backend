package zw.co.zivai.core_backend.models.lms;

import java.time.Instant;
import java.util.UUID;

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
@Table(name = "interaction_events", schema = "lms")
public class InteractionEvent extends BaseEntity {
    @ManyToOne(optional = false)
    @JoinColumn(name = "school_id")
    private School school;

    @Column(name = "edge_node_id")
    private UUID edgeNodeId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "student_id")
    private User student;

    @ManyToOne
    @JoinColumn(name = "subject_id")
    private Subject subject;

    @ManyToOne(optional = false)
    @JoinColumn(name = "skill_id")
    private Skill skill;

    @ManyToOne
    @JoinColumn(name = "assessment_attempt_id")
    private AssessmentAttempt assessmentAttempt;

    @ManyToOne
    @JoinColumn(name = "attempt_answer_id")
    private AttemptAnswer attemptAnswer;

    @Column(name = "is_correct", nullable = false)
    private Short isCorrect;

    private Double score;

    @Column(name = "max_score")
    private Double maxScore;

    @Column(name = "event_time", nullable = false)
    private Instant eventTime = Instant.now();

    @Column(name = "trace_id")
    private String traceId;
}
