package zw.co.zivai.core_backend.dtos.teachers;

import java.time.Instant;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TeacherAssessmentOverviewDto {
    String assignmentId;
    String assessmentId;
    String assessmentName;
    String assessmentType;
    String assessmentStatus;
    String subjectId;
    String subjectName;
    String classId;
    String className;
    Instant dueTime;
    boolean published;
    boolean aiEnhanced;
    long attempted;
    long submitted;
    long passed;
    long failed;
    double averageScore;
    double passRate;
    Double studentActualMark;
    Double studentExpectedMark;
    String studentGrade;
    String studentStatus;
}
