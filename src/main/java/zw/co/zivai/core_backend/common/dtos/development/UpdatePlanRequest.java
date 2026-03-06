package zw.co.zivai.core_backend.common.dtos.development;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class UpdatePlanRequest {
    private String name;
    private String description;
    private Double progress;
    private Double potentialOverall;
    private Integer eta;
    private String performance;
    private String subjectId;
    private List<CreatePlanRequest.PlanSkillRequest> skills;
    private List<CreatePlanRequest.PlanStepRequest> steps;
}
