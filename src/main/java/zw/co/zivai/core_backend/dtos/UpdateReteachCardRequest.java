package zw.co.zivai.core_backend.dtos;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class UpdateReteachCardRequest {
    private String subjectId;
    private String topicId;
    private String title;
    private String issueSummary;
    private String recommendedActions;
    private String priority;
    private String status;
    private List<String> affectedStudentIds;
    private String createdBy;
}
