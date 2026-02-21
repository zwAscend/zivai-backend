package zw.co.zivai.core_backend.dtos.students;

import java.time.Instant;
import java.util.List;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class StudentAssessmentDetailDto {
    String studentId;
    String assessmentId;
    String assessmentName;
    String assessmentType;
    String subjectId;
    String subjectName;
    String description;
    Double maxScore;
    String latestStatus;
    Instant latestDueTime;
    Double latestScore;
    String latestGrade;
    String latestFeedback;
    List<StudentAssessmentHistoryItemDto> history;
}
