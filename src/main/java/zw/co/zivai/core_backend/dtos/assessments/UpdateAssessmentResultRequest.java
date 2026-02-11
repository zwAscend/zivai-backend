package zw.co.zivai.core_backend.dtos.assessments;

import java.time.Instant;

import lombok.Data;

@Data
public class UpdateAssessmentResultRequest {
    private Double expectedMark;
    private Double actualMark;
    private String grade;
    private String feedback;
    private Instant submittedDate;
    private Object externalAssessmentData;
    private String status;
}
