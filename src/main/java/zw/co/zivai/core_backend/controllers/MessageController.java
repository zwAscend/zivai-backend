package zw.co.zivai.core_backend.controllers;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import zw.co.zivai.core_backend.dtos.CreateMessageRequest;
import zw.co.zivai.core_backend.models.lms.Message;
import zw.co.zivai.core_backend.services.MessageService;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageController {
    private final MessageService messageService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Message create(@RequestBody CreateMessageRequest request) {
        return messageService.create(request);
    }

    @GetMapping
    public List<Message> listByChat(@RequestParam UUID chatId) {
        return messageService.listByChat(chatId);
    }

    @GetMapping("/{id}")
    public Message get(@PathVariable UUID id) {
        return messageService.get(id);
    }
}
