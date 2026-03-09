package zw.co.zivai.core_backend.common.dtos.students;

import java.time.Instant;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class StudentPracticeAnswerResultDto {
    String sessionId;
    String answerId;
    String assessmentQuestionId;
    boolean correct;
    boolean skipped;
    Double score;
    Double maxScore;
    String feedback;
    Instant gradedAt;
    Integer answeredCount;
    Integer totalQuestions;
    Integer correctCount;
    Double sessionScore;
    Double sessionMaxScore;
    Double sessionPercentage;
    boolean completed;
}
