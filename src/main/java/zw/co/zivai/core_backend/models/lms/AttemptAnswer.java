package zw.co.zivai.core_backend.models.lms;

import java.time.Instant;

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
@Table(name = "attempt_answers", schema = "lms")
public class AttemptAnswer extends BaseEntity {
    @ManyToOne(optional = false)
    @JoinColumn(name = "assessment_attempt_id")
    private AssessmentAttempt assessmentAttempt;

    @ManyToOne(optional = false)
    @JoinColumn(name = "assessment_question_id")
    private AssessmentQuestion assessmentQuestion;

    @Column(name = "student_answer_text")
    private String studentAnswerText;

    @Column(name = "student_answer_blob")
    private String studentAnswerBlob;

    @Column(name = "submission_type")
    private String submissionType;

    @Column(name = "text_content")
    private String textContent;

    @Column(name = "external_assessment_data")
    private String externalAssessmentData;

    @ManyToOne
    @JoinColumn(name = "handwriting_resource_id")
    private Resource handwritingResource;

    @Column(name = "ocr_text")
    private String ocrText;

    @Column(name = "ocr_confidence")
    private Double ocrConfidence;

    @Column(name = "ocr_engine")
    private String ocrEngine;

    @Column(name = "ocr_language")
    private String ocrLanguage;

    @Column(name = "ocr_metadata")
    private String ocrMetadata;

    @Column(name = "ai_score")
    private Double aiScore;

    @Column(name = "human_score")
    private Double humanScore;

    @Column(name = "max_score", nullable = false)
    private Double maxScore;

    @Column(name = "ai_confidence")
    private Double aiConfidence;

    @Column(name = "requires_review", nullable = false)
    private boolean requiresReview = false;

    @Column(name = "feedback_text")
    private String feedbackText;

    @Column(name = "graded_at")
    private Instant gradedAt;

    @Column(name = "answer_trace_id")
    private String answerTraceId;
}
