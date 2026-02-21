package zw.co.zivai.core_backend.dtos.teachers;

import java.time.Instant;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TeacherStudentProfileSummaryDto {
    String teacherId;
    String studentId;
    StudentProfile student;
    PlanSummary planSummary;
    AssessmentSummary assessmentSummary;

    @Value
    @Builder
    public static class StudentProfile {
        String id;
        String firstName;
        String lastName;
        String email;
        Double overall;
        String performance;
        String engagement;
        String strength;
        String gradeLevel;
    }

    @Value
    @Builder
    public static class PlanSummary {
        long totalPlans;
        long activePlans;
        long completedPlans;
        Double averageProgress;
        String latestStatus;
        String latestPlanName;
    }

    @Value
    @Builder
    public static class AssessmentSummary {
        long totalAssigned;
        long attempted;
        long reviewed;
        Double averageScore;
        String latestAssessmentName;
        Instant latestDueTime;
        Double latestScore;
        String latestGrade;
    }
}
