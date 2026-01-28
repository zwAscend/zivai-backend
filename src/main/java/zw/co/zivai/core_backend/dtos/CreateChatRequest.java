package zw.co.zivai.core_backend.dtos;

import java.util.UUID;

import lombok.Data;

@Data
public class CreateChatRequest {
    private UUID schoolId;
    private String chatType = "direct";
    private String title;
}
