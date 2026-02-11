package zw.co.zivai.core_backend.dtos.development;

import java.time.Instant;
import java.util.List;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PlanDto {
    String id;
    String name;
    String description;
    double progress;
    double potentialOverall;
    int eta;
    String performance;
    List<PlanSkillDto> skills;
    List<PlanStepDto> steps;
    String subjectId;
    Instant createdAt;
    Instant updatedAt;

    @Value
    @Builder
    public static class PlanSkillDto {
        String name;
        Double score;
        List<PlanSubskillDto> subskills;
    }

    @Value
    @Builder
    public static class PlanSubskillDto {
        String name;
        Double score;
        String color;
    }

    @Value
    @Builder
    public static class PlanStepDto {
        String title;
        String type;
        String link;
        Integer order;
        List<String> additionalResources;
    }
}
