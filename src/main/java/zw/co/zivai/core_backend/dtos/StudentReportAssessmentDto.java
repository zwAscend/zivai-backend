package zw.co.zivai.core_backend.dtos;

import java.time.Instant;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class StudentReportAssessmentDto {
    String assessmentId;
    String assessmentName;
    String assessmentType;
    Double score;
    Double maxScore;
    Double percent;
    Instant submittedAt;
}
