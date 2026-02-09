package zw.co.zivai.core_backend.dtos;

import java.time.Instant;
import java.util.List;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AssessmentWithQuestionsDto {
    String id;
    String schoolId;
    String subjectId;
    String name;
    String description;
    String assessmentType;
    String visibility;
    Integer timeLimitMin;
    Integer attemptsAllowed;
    Double maxScore;
    Double weightPct;
    String resourceId;
    boolean aiEnhanced;
    String status;
    String createdBy;
    String lastModifiedBy;
    Instant createdAt;
    Instant updatedAt;
    List<AssessmentQuestionDto> questions;
}
