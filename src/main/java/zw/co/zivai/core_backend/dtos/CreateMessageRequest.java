package zw.co.zivai.core_backend.dtos;

import java.util.UUID;

import lombok.Data;

@Data
public class CreateMessageRequest {
    private UUID schoolId;
    private UUID chatId;
    private UUID senderId;
    private String content;
}
