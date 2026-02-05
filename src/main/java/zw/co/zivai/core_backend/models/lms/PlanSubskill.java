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
@Table(name = "plan_subskills", schema = "lms")
public class PlanSubskill extends BaseEntity {
    @ManyToOne(optional = false)
    @JoinColumn(name = "plan_skill_id")
    private PlanSkill planSkill;

    @Column(nullable = false)
    private String name;

    private Double score;

    @Column(nullable = false)
    private String color = "yellow";
}
