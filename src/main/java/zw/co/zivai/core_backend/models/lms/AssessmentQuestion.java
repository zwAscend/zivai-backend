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
@Table(name = "assessment_questions", schema = "lms")
public class AssessmentQuestion extends BaseEntity {
    @ManyToOne(optional = false)
    @JoinColumn(name = "assessment_id")
    private Assessment assessment;

    @ManyToOne(optional = false)
    @JoinColumn(name = "question_id")
    private Question question;

    @Column(name = "sequence_index", nullable = false)
    private Integer sequenceIndex;

    @Column(nullable = false)
    private Double points;

    @ManyToOne
    @JoinColumn(name = "rubric_scheme_id")
    private MarkingScheme rubricScheme;

    @Column(name = "rubric_scheme_version")
    private Integer rubricSchemeVersion;
}
