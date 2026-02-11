package zw.co.zivai.core_backend.dtos.development;

import java.time.Instant;
import java.util.List;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DevelopmentPlanDto {
    String id;
    String student;
    PlanDto plan;
    Instant startDate;
    Double currentProgress;
    String status;
    Instant completionDate;
    List<StudentSkillProgressDto> skillProgress;
    Instant createdAt;
    Instant updatedAt;

    @Value
    @Builder
    public static class StudentSkillProgressDto {
        String skill;
        Double currentScore;
        Double targetScore;
        Instant lastUpdated;
    }
}
