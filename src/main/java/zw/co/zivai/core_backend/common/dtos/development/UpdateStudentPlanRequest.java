package zw.co.zivai.core_backend.common.dtos.development;

import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class UpdateStudentPlanRequest {
    private String planId;
    private String subjectId;
    private Double currentProgress;
    private String status;
    private Boolean current;
    private Instant startDate;
    private Instant completionDate;

    private String name;
    private String description;
    private Double progress;
    private Double potentialOverall;
    private Integer eta;
    private String performance;
    private List<PlanSkillRequest> skills;
    private List<PlanStepRequest> steps;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PlanSkillRequest {
        private String name;
        private Double score;
        private List<PlanSubskillRequest> subskills;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PlanSubskillRequest {
        private String name;
        private Double score;
        private String color;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PlanStepRequest {
        private String title;
        private String type;
        private String content;
        private String link;
        private Integer order;
        private List<String> additionalResources;
    }
}
