package zw.co.zivai.core_backend.models.lms.students;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import zw.co.zivai.core_backend.models.base.BaseEntity;
import zw.co.zivai.core_backend.models.lms.classroom.ClassEntity;
import zw.co.zivai.core_backend.models.lms.users.User;

@Getter
@Setter
@Entity
@Table(name = "enrolments", schema = "lms")
public class Enrolment extends BaseEntity {
    @ManyToOne(optional = false)
    @JoinColumn(name = "class_id")
    private ClassEntity classEntity;

    @ManyToOne(optional = false)
    @JoinColumn(name = "student_id")
    private User student;

    @Column(name = "enrolment_status_code", nullable = false)
    private String enrolmentStatusCode;
}
