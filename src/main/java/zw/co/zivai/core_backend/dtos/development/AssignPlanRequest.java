package zw.co.zivai.core_backend.dtos.development;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AssignPlanRequest {
    private String planId;
    private String subjectId;
}
