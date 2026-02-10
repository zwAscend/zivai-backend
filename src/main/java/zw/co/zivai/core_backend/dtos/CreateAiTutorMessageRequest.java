package zw.co.zivai.core_backend.dtos;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

@Data
public class CreateAiTutorMessageRequest {
    private String sessionId;
    private String senderId;
    private String senderRole;
    private String contentType;
    private String content;
    private String transcript;
    private String audioUrl;
    private JsonNode contentPayload;
    private Boolean autoReply;
}
