package zw.co.zivai.core_backend.dtos.assessments;

import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;

import lombok.Data;

@Data
public class SubmitAssessmentAnswersRequest {
    private UUID assessmentId;
    private UUID assessmentAssignmentId;
    private UUID studentId;
    private String submissionType = "manual";
    private List<AnswerItem> answers;

    @Data
    public static class AnswerItem {
        private UUID assessmentQuestionId;
        private String studentAnswerText;
        private JsonNode studentAnswerBlob;
    }
}
