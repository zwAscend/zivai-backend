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
@Table(name = "assessment_assignments", schema = "lms")
public class AssessmentAssignment extends BaseEntity {
    @ManyToOne(optional = false)
    @JoinColumn(name = "assessment_id")
    private Assessment assessment;

    @ManyToOne
    @JoinColumn(name = "class_id")
    private ClassEntity classEntity;

    @ManyToOne(optional = false)
    @JoinColumn(name = "assigned_by")
    private User assignedBy;

    private String title;

    private String instructions;

    @Column(name = "start_time")
    private Instant startTime;

    @Column(name = "due_time")
    private Instant dueTime;

    @Column(name = "is_published", nullable = false)
    private boolean published = false;
}
