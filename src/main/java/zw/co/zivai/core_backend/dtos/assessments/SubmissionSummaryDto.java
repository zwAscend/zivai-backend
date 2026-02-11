package zw.co.zivai.core_backend.dtos.assessments;

import java.time.Instant;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SubmissionSummaryDto {
    private String id;
    private String student;
    private String assessment;
    private String submissionType;
    private Instant submittedAt;
    private String status;
    private String originalFilename;
}
