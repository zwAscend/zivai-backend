package zw.co.zivai.core_backend.common.dtos.reports;

import java.util.Map;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ClassGradeDistributionDto {
    String subjectId;
    String subjectName;
    String classId;
    String className;
    int studentCount;
    int assessmentCount;
    Map<String, Long> gradeDistribution;
    boolean hasGradedSubmissions;
}
