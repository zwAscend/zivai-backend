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
@Table(name = "assessment_enrollments", schema = "lms")
public class AssessmentEnrollment extends BaseEntity {
    @ManyToOne(optional = false)
    @JoinColumn(name = "assessment_assignment_id")
    private AssessmentAssignment assessmentAssignment;

    @ManyToOne(optional = false)
    @JoinColumn(name = "student_id")
    private User student;

    @Column(name = "status_code", nullable = false)
    private String statusCode;
}
