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
@Table(name = "student_attributes", schema = "lms")
public class StudentAttribute extends BaseEntity {
    @ManyToOne(optional = false)
    @JoinColumn(name = "student_id")
    private User student;

    @ManyToOne(optional = false)
    @JoinColumn(name = "skill_id")
    private Skill skill;

    @Column(name = "current_score", nullable = false)
    private Double currentScore;

    @Column(name = "potential_score", nullable = false)
    private Double potentialScore;

    @Column(name = "last_assessed", nullable = false)
    private Instant lastAssessed = Instant.now();
}
