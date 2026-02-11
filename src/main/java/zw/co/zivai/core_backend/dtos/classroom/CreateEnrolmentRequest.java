package zw.co.zivai.core_backend.dtos.classroom;

import java.util.UUID;

import lombok.Data;

@Data
public class CreateEnrolmentRequest {
    private UUID classId;
    private UUID studentId;
    private String enrolmentStatusCode;
}
