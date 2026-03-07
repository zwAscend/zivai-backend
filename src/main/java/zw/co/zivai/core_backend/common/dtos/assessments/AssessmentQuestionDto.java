package zw.co.zivai.core_backend.common.dtos.assessments;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AssessmentQuestionDto {
    String assessmentQuestionId;
    String questionId;
    String id;
    String stem;
    String questionTypeCode;
    Double maxMark;
    Integer difficulty;
    Object rubricJson;
    Integer sequenceIndex;
    Double points;
}
