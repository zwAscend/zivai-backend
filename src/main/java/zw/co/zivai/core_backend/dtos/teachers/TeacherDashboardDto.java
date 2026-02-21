package zw.co.zivai.core_backend.dtos.teachers;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TeacherDashboardDto {
    String teacherId;
    String subjectId;
    long totalStudents;
    long pendingSubmissions;
    long reviewedSubmissions;
    long autoGradedSubmissions;
    double averageScore;
    double averageAiConfidence;
    long unreadNotifications;
    long criticalAlerts;
    long masteryRiskCount;
    long activePlans;
}
