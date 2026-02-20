package zw.co.zivai.core_backend.dtos.assessments;

import com.fasterxml.jackson.databind.JsonNode;
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
    JsonNode rubricJson;
    Integer sequenceIndex;
    Double points;
}
