package zw.co.zivai.core_backend.dtos;

import java.time.Instant;
import java.util.UUID;

import lombok.Data;

@Data
public class CreateNotificationRequest {
    private UUID schoolId;
    private UUID recipientId;
    private String notifType;
    private String title;
    private String message;
    private Object data;
    private boolean read = false;
    private Instant readAt;
    private String priority = "medium";
    private Instant expiresAt;
}
