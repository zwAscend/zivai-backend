package zw.co.zivai.core_backend.dtos.reports;

import java.util.List;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class StudentReportDto {
    String studentId;
    String studentName;
    String subjectId;
    String subjectName;
    double averagePercent;
    String predictedGrade;
    int assessmentCount;
    List<StudentReportAssessmentDto> assessments;
    List<StudentTopicMasteryDto> masteryGaps;
}
