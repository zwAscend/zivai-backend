package zw.co.zivai.core_backend.dtos.assessments;

import java.time.Instant;
import java.util.UUID;

import lombok.Data;

@Data
public class CreateAssessmentResultRequest {
    private UUID student;
    private Double expectedMark;
    private Double actualMark;
    private String grade;
    private String feedback;
    private Instant submittedDate;
    private Object externalAssessmentData;
    private String status;
}
