package zw.co.zivai.core_backend.common.models.lms.classroom;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import zw.co.zivai.core_backend.common.models.base.BaseEntity;
import zw.co.zivai.core_backend.common.models.lms.school.School;
import zw.co.zivai.core_backend.common.models.lms.users.User;

@Getter
@Setter
@Entity
@Table(name = "classes", schema = "lms")
public class ClassEntity extends BaseEntity {
    @ManyToOne(optional = false)
    @JoinColumn(name = "school_id")
    private School school;

    @Column(nullable = false)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(name = "grade_level")
    private String gradeLevel;

    @Column(name = "academic_year")
    private String academicYear;

    @ManyToOne
    @JoinColumn(name = "homeroom_teacher_id")
    private User homeroomTeacher;
}
