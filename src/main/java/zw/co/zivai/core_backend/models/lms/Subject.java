package zw.co.zivai.core_backend.models.lms;

import com.fasterxml.jackson.databind.JsonNode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.Getter;
import lombok.Setter;
import zw.co.zivai.core_backend.models.base.BaseEntity;

@Getter
@Setter
@Entity
@Table(name = "subjects", schema = "lms")
public class Subject extends BaseEntity {
    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(name = "exam_board_code")
    private String examBoardCode;

    private String description;

    @Column(name = "subject_attributes")
    @JdbcTypeCode(SqlTypes.JSON)
    private JsonNode subjectAttributes;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;
}
