package zw.co.zivai.core_backend.dtos.assessments;

import java.util.UUID;

import lombok.Data;
import java.util.List;

@Data
public class CreateAssessmentRequest {
    private UUID schoolId;
    private UUID subjectId;
    private String name;
    private String description;
    private String assessmentType;
    private String visibility = "private";
    private Integer timeLimitMin;
    private Integer attemptsAllowed;
    private Double maxScore;
    private Double weightPct = 0.0;
    private UUID resourceId;
    private boolean aiEnhanced = false;
    private String status = "draft";
    private UUID createdBy;
    private UUID lastModifiedBy;
    private List<CreateAssessmentQuestionRequest> questions;
}
