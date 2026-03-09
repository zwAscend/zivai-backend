package zw.co.zivai.core_backend.common.dtos.students;

import java.util.List;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class StudentPracticeQuestionDto {
    String assessmentQuestionId;
    String questionId;
    String topicId;
    String topicName;
    String prompt;
    String questionType;
    Double maxScore;
    List<String> options;
    boolean multipleSelection;
}
