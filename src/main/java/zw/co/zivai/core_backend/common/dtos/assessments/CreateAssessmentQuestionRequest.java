package zw.co.zivai.core_backend.common.dtos.assessments;

import lombok.Data;

@Data
public class CreateAssessmentQuestionRequest {
    private String stem;
    private String questionTypeCode;
    private Double maxMark;
    private Integer difficulty;
    private Object rubricJson;
    private Integer sequenceIndex;
    private Double points;
}
