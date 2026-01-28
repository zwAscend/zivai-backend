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
@Table(name = "assessments", schema = "lms")
public class Assessment extends BaseEntity {
    @ManyToOne(optional = false)
    @JoinColumn(name = "school_id")
    private School school;

    @ManyToOne(optional = false)
    @JoinColumn(name = "subject_id")
    private Subject subject;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(name = "assessment_type", nullable = false)
    private String assessmentType;

    @Column(name = "visibility", nullable = false)
    private String visibility = "private";

    @Column(name = "time_limit_min")
    private Integer timeLimitMin;

    @Column(name = "attempts_allowed")
    private Integer attemptsAllowed;

    @Column(name = "max_score", nullable = false)
    private Double maxScore;

    @Column(name = "weight_pct", nullable = false)
    private Double weightPct = 0.0;

    @ManyToOne
    @JoinColumn(name = "resource_id")
    private Resource resource;

    @Column(name = "is_ai_enhanced", nullable = false)
    private boolean aiEnhanced = false;

    @Column(nullable = false)
    private String status = "draft";

    @ManyToOne(optional = false)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @ManyToOne(optional = false)
    @JoinColumn(name = "last_modified_by")
    private User lastModifiedBy;
}
