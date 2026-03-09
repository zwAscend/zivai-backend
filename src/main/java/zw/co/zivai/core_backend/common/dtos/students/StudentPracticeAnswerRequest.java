package zw.co.zivai.core_backend.common.dtos.students;

import java.util.List;
import java.util.UUID;

import lombok.Data;

@Data
public class StudentPracticeAnswerRequest {
    private UUID assessmentQuestionId;
    private String studentAnswerText;
    private List<String> selectedOptions;
    private boolean skipped;
}
