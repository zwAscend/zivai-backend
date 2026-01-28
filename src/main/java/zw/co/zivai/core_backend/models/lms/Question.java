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
@Table(name = "questions", schema = "lms")
public class Question extends BaseEntity {
    @ManyToOne(optional = false)
    @JoinColumn(name = "subject_id")
    private Subject subject;

    @ManyToOne
    @JoinColumn(name = "topic_id")
    private Topic topic;

    @ManyToOne
    @JoinColumn(name = "author_id")
    private User author;

    private String code;

    @Column(nullable = false)
    private String stem;

    @Column(name = "question_type_code", nullable = false)
    private String questionTypeCode;

    @Column(name = "max_mark", nullable = false)
    private Double maxMark;

    private Short difficulty;

    @Column(name = "exam_style_code")
    private String examStyleCode;

    @Column(name = "source_year")
    private Short sourceYear;

    @Column(name = "rubric_json")
    private String rubricJson;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;
}
