package zw.co.zivai.core_backend.dtos.teachers;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TeacherClassDto {
    String classId;
    String code;
    String name;
    String gradeLevel;
    String academicYear;
    long subjectCount;
    long studentCount;
}
