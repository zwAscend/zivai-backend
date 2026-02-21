package zw.co.zivai.core_backend.dtos.teachers;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TeacherStudentSummaryDto {
    String studentId;
    String firstName;
    String lastName;
    String email;
    Double overall;
    String performance;
    String engagement;
    String strength;
    long subjectCount;
    long classCount;
    String planStatus;
    Double planProgress;
    String activePlanName;
}
