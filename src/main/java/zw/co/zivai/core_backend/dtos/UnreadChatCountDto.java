package zw.co.zivai.core_backend.dtos;

import java.time.Instant;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class UnreadChatCountDto {
    String studentId;
    long unreadCount;
    String studentName;
    String lastMessage;
    Instant lastMessageTime;
}
