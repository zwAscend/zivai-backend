package zw.co.zivai.core_backend.common.dtos.students;

import java.time.Instant;
import java.util.List;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class StudentPlanRuntimeProgressDto {
    String studentPlanId;
    String studentId;
    String activeStepId;
    List<String> completedStepIds;
    Integer totalSteps;
    Integer completedSteps;
    Double currentProgress;
    String status;
    Instant updatedAt;
}
