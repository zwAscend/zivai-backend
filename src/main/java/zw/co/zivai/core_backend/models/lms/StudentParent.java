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
@Table(name = "student_parents", schema = "lms")
public class StudentParent extends BaseEntity {
    @ManyToOne(optional = false)
    @JoinColumn(name = "student_id")
    private User student;

    @ManyToOne(optional = false)
    @JoinColumn(name = "parent_id")
    private Parent parent;

    @Column(name = "relationship_code", nullable = false)
    private String relationshipCode;

    @Column(name = "is_primary_guardian", nullable = false)
    private boolean primaryGuardian = false;

    private String notes;
}
