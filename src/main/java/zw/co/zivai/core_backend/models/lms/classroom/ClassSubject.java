package zw.co.zivai.core_backend.models.lms.classroom;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import zw.co.zivai.core_backend.models.base.BaseEntity;
import zw.co.zivai.core_backend.models.lms.school.School;
import zw.co.zivai.core_backend.models.lms.subjects.Subject;
import zw.co.zivai.core_backend.models.lms.users.User;

@Getter
@Setter
@Entity
@Table(name = "class_subjects", schema = "lms")
public class ClassSubject extends BaseEntity {
    @ManyToOne(optional = false)
    @JoinColumn(name = "school_id")
    private School school;

    @ManyToOne(optional = false)
    @JoinColumn(name = "class_id")
    private ClassEntity classEntity;

    @ManyToOne(optional = false)
    @JoinColumn(name = "subject_id")
    private Subject subject;

    @ManyToOne
    @JoinColumn(name = "teacher_id")
    private User teacher;

    @Column(name = "academic_year", nullable = false)
    private String academicYear;

    private String term;
    private String name;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;
}
