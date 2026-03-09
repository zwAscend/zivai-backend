package zw.co.zivai.core_backend.common.dtos.students;

import java.util.UUID;

import lombok.Data;

@Data
public class StartStudentPracticeSessionRequest {
    private UUID topicId;
    private Integer questionCount;
    private String mode;
    private String title;
}
