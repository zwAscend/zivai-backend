package zw.co.zivai.core_backend.common.dtos.teachers;

import java.util.List;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TeacherPerformanceOverviewDto {
    String teacherId;
    String subjectId;
    String subjectName;
    String topicId;
    String topicName;
    String assessmentId;
    String assessmentName;
    String currentView;
    Summary summary;
    Heatmap heatmap;
    List<MisconceptionSignal> misconceptions;

    @Value
    @Builder
    public static class Summary {
        long totalStudents;
        long studentsWithData;
        long supportCount;
        long onTrackCount;
        double averageScore;
        String filterLabel;
        String strongestArea;
        String weakestArea;
    }

    @Value
    @Builder
    public static class Heatmap {
        int columns;
        List<HeatmapCell> cells;
    }

    @Value
    @Builder
    public static class HeatmapCell {
        String studentId;
        String firstName;
        String lastName;
        String email;
        Double score;
        int intensity;
        String status;
        String performance;
        String engagement;
        String strength;
        String className;
        String activePlanName;
        Double planProgress;
        String latestAssessmentName;
        Double latestAssessmentScore;
        String focusArea;
        String note;
    }

    @Value
    @Builder
    public static class MisconceptionSignal {
        String id;
        String title;
        String summary;
        String riskLevel;
        long learnerCount;
        double averageScore;
        List<String> studentIds;
    }
}
