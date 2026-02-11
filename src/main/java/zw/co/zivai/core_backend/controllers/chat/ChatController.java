package zw.co.zivai.core_backend.controllers.chat;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import zw.co.zivai.core_backend.dtos.chat.CreateChatRequest;
import zw.co.zivai.core_backend.models.lms.Chat;
import zw.co.zivai.core_backend.services.chat.ChatService;

@RestController
@RequestMapping("/api/chats")
@RequiredArgsConstructor
public class ChatController {
    private final ChatService chatService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Chat create(@RequestBody CreateChatRequest request) {
        return chatService.create(request);
    }

    @GetMapping
    public List<Chat> list() {
        return chatService.list();
    }

    @GetMapping("/{id}")
    public Chat get(@PathVariable UUID id) {
        return chatService.get(id);
    }
}
