package zw.co.zivai.core_backend.dtos.students;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class StudentChallengeEligibilityDto {
    boolean eligible;
    String reason;
    int minQuestionsRequired;
    int availableQuestions;
}
