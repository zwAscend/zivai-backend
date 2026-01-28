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
@Table(name = "plans", schema = "lms")
public class Plan extends BaseEntity {
    @ManyToOne(optional = false)
    @JoinColumn(name = "subject_id")
    private Subject subject;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private Double progress = 0.0;

    @Column(name = "potential_overall", nullable = false)
    private Double potentialOverall;

    @Column(name = "eta_days", nullable = false)
    private Integer etaDays;

    @Column(nullable = false)
    private String performance;
}
