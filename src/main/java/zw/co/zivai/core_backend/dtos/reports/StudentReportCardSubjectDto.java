package zw.co.zivai.core_backend.dtos.reports;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class StudentReportCardSubjectDto {
    String subjectId;
    String subjectCode;
    String subjectName;
    double masteryPercent;
    String currentGrade;
    String predictedZimsecGrade;
    int assessmentCount;
}
