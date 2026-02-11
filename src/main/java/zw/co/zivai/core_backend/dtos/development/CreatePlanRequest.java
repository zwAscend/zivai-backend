package zw.co.zivai.core_backend.dtos.development;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CreatePlanRequest {
    private String name;
    private String description;
    private Double progress;
    private Double potentialOverall;
    private Integer eta;
    private String performance;
    private String subjectId;
    private List<PlanSkillRequest> skills;
    private List<PlanStepRequest> steps;

    @Data
    public static class PlanSkillRequest {
        private String name;
        private Double score;
        private List<PlanSubskillRequest> subskills;
    }

    @Data
    public static class PlanSubskillRequest {
        private String name;
        private Double score;
        private String color;
    }

    @Data
    public static class PlanStepRequest {
        private String title;
        private String type;
        private String link;
        private Integer order;
        private List<String> additionalResources;
    }
}
