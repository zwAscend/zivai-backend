package zw.co.zivai.core_backend.dtos.ai;

import java.time.Instant;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AiTutorMessageDto {
    private String id;
    private String sessionId;
    private String senderId;
    private String senderRole;
    private String contentType;
    private String content;
    private String transcript;
    private String audioUrl;
    private JsonNode contentPayload;
    private Instant ts;
}
