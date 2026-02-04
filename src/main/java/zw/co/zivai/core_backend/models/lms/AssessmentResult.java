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
@Table(name = "assessment_results", schema = "lms")
public class AssessmentResult extends BaseEntity {
    @ManyToOne(optional = false)
    @JoinColumn(name = "assessment_assignment_id")
    private AssessmentAssignment assessmentAssignment;

    @ManyToOne(optional = false)
    @JoinColumn(name = "student_id")
    private User student;

    @ManyToOne
    @JoinColumn(name = "finalized_attempt_id")
    private AssessmentAttempt finalizedAttempt;

    @Column(name = "expected_mark")
    private Double expectedMark;

    @Column(name = "actual_mark")
    private Double actualMark;

    private String grade;

    private String feedback;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "graded_at")
    private Instant gradedAt;

    @Column(nullable = false)
    private String status = "draft";
}
