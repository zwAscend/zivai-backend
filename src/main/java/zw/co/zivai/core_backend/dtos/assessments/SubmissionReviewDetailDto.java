package zw.co.zivai.core_backend.dtos.assessments;

import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SubmissionReviewDetailDto {
    private String submissionId;
    private String assessmentId;
    private String studentId;
    private List<QuestionReviewDetail> questions;

    @Data
    @Builder
    public static class QuestionReviewDetail {
        private String assessmentQuestionId;
        private Integer order;
        private String prompt;
        private String studentAnswer;
        private List<String> expectedMarkingPoints;
        private Double awardedMarks;
        private Double maxMarks;
        private String feedback;
    }
}
