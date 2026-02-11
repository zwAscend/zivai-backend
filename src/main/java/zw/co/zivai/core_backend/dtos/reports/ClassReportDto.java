package zw.co.zivai.core_backend.dtos.reports;

import java.util.List;
import java.util.Map;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ClassReportDto {
    String subjectId;
    String subjectName;
    String classId;
    String className;
    double classAveragePercent;
    String predictedGrade;
    int studentCount;
    int assessmentCount;
    Map<String, Long> gradeDistribution;
    List<CurriculumTopicForecastDto> masteryGaps;
}
