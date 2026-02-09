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
@Table(name = "reteach_cards", schema = "lms")
public class ReteachCard extends BaseEntity {
    @ManyToOne(optional = false)
    @JoinColumn(name = "subject_id")
    private Subject subject;

    @ManyToOne
    @JoinColumn(name = "topic_id")
    private Topic topic;

    @Column(nullable = false)
    private String title;

    @Column(name = "issue_summary")
    private String issueSummary;

    @Column(name = "recommended_actions")
    private String recommendedActions;

    @Column(name = "priority_code", nullable = false)
    private String priorityCode = "medium";

    @Column(name = "status_code", nullable = false)
    private String statusCode = "draft";

    @Column(name = "affected_student_ids")
    @JdbcTypeCode(SqlTypes.JSON)
    private JsonNode affectedStudentIds;

    @ManyToOne
    @JoinColumn(name = "created_by")
    private User createdBy;
}
