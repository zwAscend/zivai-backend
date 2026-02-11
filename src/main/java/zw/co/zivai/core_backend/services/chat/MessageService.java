package zw.co.zivai.core_backend.services.chat;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import zw.co.zivai.core_backend.dtos.chat.CreateMessageRequest;
import zw.co.zivai.core_backend.exceptions.NotFoundException;
import zw.co.zivai.core_backend.models.lms.Chat;
import zw.co.zivai.core_backend.models.lms.Message;
import zw.co.zivai.core_backend.models.lms.School;
import zw.co.zivai.core_backend.models.lms.User;
import zw.co.zivai.core_backend.repositories.chat.ChatRepository;
import zw.co.zivai.core_backend.repositories.chat.MessageRepository;
import zw.co.zivai.core_backend.repositories.school.SchoolRepository;
import zw.co.zivai.core_backend.repositories.user.UserRepository;

@Service
@RequiredArgsConstructor
public class MessageService {
    private final MessageRepository messageRepository;
    private final SchoolRepository schoolRepository;
    private final ChatRepository chatRepository;
    private final UserRepository userRepository;

    public Message create(CreateMessageRequest request) {
        School school = schoolRepository.findById(request.getSchoolId())
            .orElseThrow(() -> new NotFoundException("School not found: " + request.getSchoolId()));
        Chat chat = chatRepository.findById(request.getChatId())
            .orElseThrow(() -> new NotFoundException("Chat not found: " + request.getChatId()));
        User sender = userRepository.findById(request.getSenderId())
            .orElseThrow(() -> new NotFoundException("Sender not found: " + request.getSenderId()));

        Message message = new Message();
        message.setSchool(school);
        message.setChat(chat);
        message.setSender(sender);
        message.setContent(request.getContent());

        return messageRepository.save(message);
    }

    public List<Message> listByChat(UUID chatId) {
        return messageRepository.findByChatIdOrderByTsAsc(chatId);
    }

    public Message get(UUID id) {
        return messageRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Message not found: " + id));
    }
}
