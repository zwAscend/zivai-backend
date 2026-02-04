package zw.co.zivai.core_backend.dtos;

import lombok.Data;

@Data
public class ReviewSubmissionRequest {
    private Double scoreAdjustment;
    private String feedbackAdjustment;
    private Double finalScore;
    private String finalGrade;
}
