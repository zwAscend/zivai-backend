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
@Table(name = "marking_scheme_items", schema = "lms")
public class MarkingSchemeItem extends BaseEntity {
    @ManyToOne(optional = false)
    @JoinColumn(name = "marking_scheme_id")
    private MarkingScheme markingScheme;

    @Column(name = "step_index", nullable = false)
    private Integer stepIndex;

    @Column(nullable = false)
    private String description;

    @Column(name = "mark_value", nullable = false)
    private Double markValue;

    @Column(name = "rubric_code")
    private String rubricCode;
}
