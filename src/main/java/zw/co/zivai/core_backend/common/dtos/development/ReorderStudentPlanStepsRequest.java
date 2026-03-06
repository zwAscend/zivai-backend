package zw.co.zivai.core_backend.common.dtos.development;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReorderStudentPlanStepsRequest {
    private List<String> stepIds;
}
