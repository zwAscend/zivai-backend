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
@Table(name = "student_plans", schema = "lms")
public class StudentPlan extends BaseEntity {
    @ManyToOne(optional = false)
    @JoinColumn(name = "student_id")
    private User student;

    @ManyToOne(optional = false)
    @JoinColumn(name = "plan_id")
    private Plan plan;

    @ManyToOne(optional = false)
    @JoinColumn(name = "subject_id")
    private Subject subject;

    @Column(name = "start_date", nullable = false)
    private Instant startDate = Instant.now();

    @Column(name = "current_progress", nullable = false)
    private Double currentProgress = 0.0;

    @Column(nullable = false)
    private String status = "on_hold";

    @Column(name = "is_current", nullable = false)
    private boolean current = false;

    @Column(name = "completion_date")
    private Instant completionDate;
}
