package zw.co.zivai.core_backend.dtos.assessments;

import java.util.UUID;

import lombok.Data;

@Data
public class CreateAssessmentAttemptRequest {
    private UUID assessmentEnrollmentId;
    private Integer attemptNumber = 1;
    private String gradingStatusCode;
    private Double maxScore;
}
