package zw.co.zivai.core_backend.common.dtos.students;

import java.util.List;

import lombok.Data;

@Data
public class StudentPlanRuntimeProgressRequest {
    private List<String> completedStepIds;
    private String activeStepId;
    private String status;
}
