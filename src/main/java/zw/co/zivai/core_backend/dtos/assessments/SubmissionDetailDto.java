package zw.co.zivai.core_backend.dtos.assessments;

import java.time.Instant;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SubmissionDetailDto {
    private String id;
    private StudentSummary student;
    private AssessmentSummary assessment;
    private String submissionType;
    private Instant submittedAt;
    private String status;
    private AutoGrading autoGrading;
    private TeacherReview teacherReview;
    private String submissionContent;
    private Object externalAssessmentData;

    @Data
    @Builder
    public static class StudentSummary {
        private String id;
        private String firstName;
        private String lastName;
        private String email;
    }

    @Data
    @Builder
    public static class AssessmentSummary {
        private String id;
        private String name;
        private String description;
        private String type;
        private Double maxScore;
        private Double weight;
        private Instant dueDate;
    }

    @Data
    @Builder
    public static class AutoGrading {
        private AutoGradingResult result;
    }

    @Data
    @Builder
    public static class AutoGradingResult {
        private Double totalScore;
        private Double percentage;
        private String grade;
        private String feedback;
        private Object breakdown;
        private Double confidence;
        private Instant gradedAt;
    }

    @Data
    @Builder
    public static class TeacherReview {
        private boolean reviewed;
        private Instant reviewedAt;
        private ReviewAdjustments adjustments;
    }

    @Data
    @Builder
    public static class ReviewAdjustments {
        private Double scoreAdjustment;
        private String feedbackAdjustment;
        private Double finalScore;
        private String finalGrade;
    }
}
