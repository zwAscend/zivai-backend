package zw.co.zivai.core_backend.dtos;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class UpdatePlanProgressRequest {
    private Double currentProgress;
    private String status;
    private List<SkillProgressRequest> skillProgress;

    @Data
    public static class SkillProgressRequest {
        private String skill;
        private Double currentScore;
        private Double targetScore;
    }
}
