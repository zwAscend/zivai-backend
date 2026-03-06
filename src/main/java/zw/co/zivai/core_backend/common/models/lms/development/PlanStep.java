package zw.co.zivai.core_backend.common.models.lms.development;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import zw.co.zivai.core_backend.common.models.base.BaseEntity;

@Getter
@Setter
@Entity
@Table(name = "plan_steps", schema = "lms")
public class PlanStep extends BaseEntity {
    @ManyToOne(optional = false)
    @JoinColumn(name = "plan_id")
    private Plan plan;

    @Column(nullable = false)
    private String title;

    @Column(name = "step_type", nullable = false)
    private String stepType;

    @Column(columnDefinition = "text")
    private String content;

    private String link;

    @Column(name = "step_order", nullable = false)
    private Integer stepOrder;
}
