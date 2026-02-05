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
@Table(name = "student_subject_enrolments", schema = "lms")
public class StudentSubjectEnrolment extends BaseEntity {
    @ManyToOne(optional = false)
    @JoinColumn(name = "class_subject_id")
    private ClassSubject classSubject;

    @ManyToOne(optional = false)
    @JoinColumn(name = "student_id")
    private User student;

    @Column(name = "status_code", nullable = false)
    private String statusCode;

    @Column(name = "enrolled_at", nullable = false)
    private Instant enrolledAt = Instant.now();
}
