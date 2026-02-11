package zw.co.zivai.core_backend.dtos.assessments;

import java.time.Instant;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AssessmentEnrollmentSummaryDto {
    private String id;
    private String statusCode;

    private String studentId;
    private String studentFirstName;
    private String studentLastName;
    private String studentEmail;

    private String assignmentId;
    private String assessmentId;
    private String assessmentName;
    private String classId;
    private String className;
    private Instant dueTime;
    private boolean published;

    private Instant createdAt;
    private Instant updatedAt;
}
