package zw.co.zivai.core_backend.dtos.development;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class StudentPlanStepRequest {
    private String title;
    private String type;
    private String content;
    private String link;
    private Integer order;
    private List<String> additionalResources;
}
