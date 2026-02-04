package zw.co.zivai.core_backend.dtos;

import java.time.Instant;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AssessmentResultDto {
    private String id;
    private String student;
    private String assessment;
    private Double expectedMark;
    private Double actualMark;
    private String grade;
    private String feedback;
    private Instant submittedDate;
    private Instant createdAt;
    private Instant updatedAt;
    private Object externalAssessmentData;
}
