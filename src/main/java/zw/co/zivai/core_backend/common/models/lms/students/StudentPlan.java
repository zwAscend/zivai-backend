package zw.co.zivai.core_backend.common.models.lms.students;

import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import zw.co.zivai.core_backend.common.models.base.BaseEntity;
import zw.co.zivai.core_backend.common.models.lms.subjects.Subject;
import zw.co.zivai.core_backend.common.models.lms.development.Plan;
import zw.co.zivai.core_backend.common.models.lms.users.User;

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

    @Column(name = "active_step_id")
    private UUID activeStepId;

    @Column(name = "completed_step_ids", nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    private JsonNode completedStepIds;

    @Column(nullable = false)
    private String status = "on_hold";

    @Column(name = "is_current", nullable = false)
    private boolean current = false;

    @Column(name = "completion_date")
    private Instant completionDate;
}
