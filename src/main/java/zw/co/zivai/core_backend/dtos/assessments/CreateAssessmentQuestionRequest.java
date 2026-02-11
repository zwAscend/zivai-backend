package zw.co.zivai.core_backend.dtos.assessments;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

@Data
public class CreateAssessmentQuestionRequest {
    private String stem;
    private String questionTypeCode;
    private Double maxMark;
    private Integer difficulty;
    private JsonNode rubricJson;
    private Integer sequenceIndex;
    private Double points;
}
