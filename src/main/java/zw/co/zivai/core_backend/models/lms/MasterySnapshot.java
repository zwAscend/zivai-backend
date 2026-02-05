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
@Table(name = "mastery_snapshots", schema = "lms")
public class MasterySnapshot extends BaseEntity {
    @ManyToOne(optional = false)
    @JoinColumn(name = "student_id")
    private User student;

    @ManyToOne(optional = false)
    @JoinColumn(name = "subject_id")
    private Subject subject;

    @Column(name = "snapshot_time", nullable = false)
    private Instant snapshotTime = Instant.now();

    @Column(nullable = false)
    private String source;

    @Column(name = "average_mastery")
    private Double averageMastery;

    @Column(name = "risk_level_code")
    private String riskLevelCode;
}
