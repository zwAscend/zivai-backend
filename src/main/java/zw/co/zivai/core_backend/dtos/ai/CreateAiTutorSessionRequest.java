package zw.co.zivai.core_backend.dtos.ai;

import lombok.Data;

@Data
public class CreateAiTutorSessionRequest {
    private String studentId;
    private String subjectId;
    private String createdBy;
}
