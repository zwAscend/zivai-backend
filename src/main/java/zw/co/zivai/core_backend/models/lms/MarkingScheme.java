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
@Table(name = "marking_schemes", schema = "lms")
public class MarkingScheme extends BaseEntity {
    @ManyToOne(optional = false)
    @JoinColumn(name = "question_id")
    private Question question;

    @Column(nullable = false)
    private Integer version = 1;

    @Column(name = "total_mark", nullable = false)
    private Double totalMark;

    @Column(name = "scheme_source")
    private String schemeSource;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;
}
