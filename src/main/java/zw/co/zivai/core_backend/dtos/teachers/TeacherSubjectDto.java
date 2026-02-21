package zw.co.zivai.core_backend.dtos.teachers;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TeacherSubjectDto {
    String subjectId;
    String subjectCode;
    String subjectName;
    long classCount;
    long studentCount;
}
