package zw.co.zivai.core_backend.dtos.assessments;

import java.util.UUID;

import lombok.Data;

@Data
public class CreateAssessmentEnrollmentRequest {
    private UUID assessmentAssignmentId;
    private UUID studentId;
    private String statusCode;
}
