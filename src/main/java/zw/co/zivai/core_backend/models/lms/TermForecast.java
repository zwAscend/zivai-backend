package zw.co.zivai.core_backend.models.lms;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import zw.co.zivai.core_backend.models.base.BaseEntity;

@Getter
@Setter
@Entity
@Table(name = "term_forecasts", schema = "lms")
public class TermForecast extends BaseEntity {
    @ManyToOne(optional = false)
    @JoinColumn(name = "class_subject_id")
    private ClassSubject classSubject;

    @Column(nullable = false)
    private String term;

    @Column(name = "academic_year", nullable = false)
    private String academicYear;

    @Column(name = "expected_coverage_pct", nullable = false)
    private Double expectedCoveragePct = 0.0;

    @Column(name = "expected_topic_ids")
    @JdbcTypeCode(SqlTypes.JSON)
    private JsonNode expectedTopicIds;

    private String notes;

    @ManyToOne
    @JoinColumn(name = "created_by")
    private User createdBy;
}
