package zw.co.zivai.core_backend.dtos.assessments;

import java.time.Instant;
import java.util.UUID;

import lombok.Data;

@Data
public class CreateAssessmentAssignmentRequest {
    private UUID assessmentId;
    private UUID classId;
    private UUID assignedBy;
    private String title;
    private String instructions;
    private Instant startTime;
    private Instant dueTime;
    private boolean published = false;
}
