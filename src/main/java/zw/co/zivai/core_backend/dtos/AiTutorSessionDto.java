package zw.co.zivai.core_backend.dtos;

import java.time.Instant;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AiTutorSessionDto {
    private String id;
    private String studentId;
    private String studentName;
    private String subjectId;
    private String subjectName;
    private String status;
    private Instant lastMessageAt;
    private Instant createdAt;
}
