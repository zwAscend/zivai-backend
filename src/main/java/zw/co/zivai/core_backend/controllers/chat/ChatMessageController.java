package zw.co.zivai.core_backend.controllers.chat;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import zw.co.zivai.core_backend.dtos.chat.ChatMessageDto;
import zw.co.zivai.core_backend.dtos.chat.UnreadChatCountDto;
import zw.co.zivai.core_backend.services.chat.ChatMessageService;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatMessageController {
    private final ChatMessageService chatMessageService;

    @GetMapping("/messages/{studentId}")
    public List<ChatMessageDto> listMessages(@PathVariable UUID studentId) {
        return chatMessageService.getMessagesForStudent(studentId);
    }

    @PostMapping("/messages/{studentId}")
    @ResponseStatus(HttpStatus.CREATED)
    public ChatMessageDto sendMessage(@PathVariable UUID studentId, @RequestBody Map<String, String> payload) {
        String content = payload != null ? payload.getOrDefault("content", "") : "";
        String senderIdRaw = payload != null ? payload.get("senderId") : null;
        UUID senderId = senderIdRaw != null && !senderIdRaw.isBlank() ? UUID.fromString(senderIdRaw) : null;
        return chatMessageService.sendMessage(studentId, senderId, content);
    }

    @PutMapping("/read/{studentId}")
    public Map<String, String> markRead(@PathVariable UUID studentId) {
        chatMessageService.markChatRead(studentId);
        return Map.of("message", "Marked as read");
    }

    @GetMapping("/unread")
    public List<UnreadChatCountDto> unreadCounts() {
        return chatMessageService.getUnreadCounts();
    }
}
