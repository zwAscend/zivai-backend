package zw.co.zivai.core_backend.dtos.students;

import java.util.List;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class StudentSubjectOverviewDto {
    String studentId;
    String subjectId;
    String subjectName;
    long topicCount;
    long unitCount;
    long totalQuestionCount;
    StudentChallengeEligibilityDto challengeEligibility;
    List<StudentSubjectOverviewUnitDto> units;
}
