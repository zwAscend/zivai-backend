package zw.co.zivai.core_backend.dtos.assessments;

import java.util.List;
import java.util.UUID;

import lombok.Data;

@Data
public class BulkAssessmentEnrollmentRequest {
    private List<UUID> studentIds;
    private String statusCode = "assigned";
}
