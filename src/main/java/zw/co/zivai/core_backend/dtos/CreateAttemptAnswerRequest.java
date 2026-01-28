package zw.co.zivai.core_backend.dtos;

import java.util.UUID;

import lombok.Data;

@Data
public class CreateAttemptAnswerRequest {
    private UUID assessmentAttemptId;
    private UUID assessmentQuestionId;
    private String studentAnswerText;
    private String studentAnswerBlob;
    private UUID handwritingResourceId;
    private String ocrText;
    private Double ocrConfidence;
    private String ocrEngine;
    private String ocrLanguage;
    private String ocrMetadata;
    private Double aiScore;
    private Double humanScore;
    private Double maxScore;
    private Double aiConfidence;
    private boolean requiresReview = false;
    private String feedbackText;
    private String answerTraceId;
}
