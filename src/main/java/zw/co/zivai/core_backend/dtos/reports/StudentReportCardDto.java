package zw.co.zivai.core_backend.dtos.reports;

import java.util.List;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class StudentReportCardDto {
    String studentId;
    String studentName;
    List<StudentReportCardSubjectDto> subjects;
}
