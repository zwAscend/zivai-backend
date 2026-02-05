package zw.co.zivai.core_backend.dtos;

import java.time.Instant;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ChatMessageDto {
    String id;
    SenderDto sender;
    String content;
    Instant timestamp;
    boolean read;
    String chatId;
    Boolean isTeacher;
    Instant createdAt;
    Instant updatedAt;

    @Value
    @Builder
    public static class SenderDto {
        String id;
        String firstName;
        String lastName;
        String avatar;
    }
}
