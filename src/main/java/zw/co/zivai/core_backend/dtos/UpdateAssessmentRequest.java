package zw.co.zivai.core_backend.dtos;

import java.util.List;
import java.util.UUID;

import lombok.Data;

@Data
public class UpdateAssessmentRequest {
    private UUID schoolId;
    private UUID subjectId;
    private String name;
    private String description;
    private String assessmentType;
    private String visibility;
    private Integer timeLimitMin;
    private Integer attemptsAllowed;
    private Double maxScore;
    private Double weightPct;
    private UUID resourceId;
    private Boolean aiEnhanced;
    private String status;
    private UUID lastModifiedBy;
    private List<CreateAssessmentQuestionRequest> questions;
}
