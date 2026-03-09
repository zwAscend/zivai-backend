package zw.co.zivai.core_backend.common.dtos.students;

import java.time.Instant;
import java.util.List;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class StudentPracticeSessionDto {
    String sessionId;
    String assessmentId;
    String assignmentId;
    String enrollmentId;
    String subjectId;
    String subjectName;
    String topicId;
    String topicName;
    String mode;
    String title;
    String status;
    Instant startedAt;
    Instant submittedAt;
    Integer questionCount;
    Integer answeredCount;
    Integer correctCount;
    Double score;
    Double maxScore;
    Double percentage;
    Integer durationMinutes;
    List<StudentPracticeQuestionDto> questions;
}
