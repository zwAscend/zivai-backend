package zw.co.zivai.core_backend.common.dtos.notification;

import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NotificationResponse {
    private UUID id;
    private UUID schoolId;
    private UUID recipientId;
    private String notifType;
    private String title;
    private String message;
    private JsonNode data;
    private boolean read;
    private Instant readAt;
    private String priority;
    private Instant expiresAt;
    private Instant createdAt;
    private Instant updatedAt;
}
