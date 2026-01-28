package zw.co.zivai.core_backend.services;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import zw.co.zivai.core_backend.dtos.CreateChatRequest;
import zw.co.zivai.core_backend.exceptions.NotFoundException;
import zw.co.zivai.core_backend.models.lms.Chat;
import zw.co.zivai.core_backend.models.lms.School;
import zw.co.zivai.core_backend.repositories.ChatRepository;
import zw.co.zivai.core_backend.repositories.SchoolRepository;

@Service
@RequiredArgsConstructor
public class ChatService {
    private final ChatRepository chatRepository;
    private final SchoolRepository schoolRepository;

    public Chat create(CreateChatRequest request) {
        School school = schoolRepository.findById(request.getSchoolId())
            .orElseThrow(() -> new NotFoundException("School not found: " + request.getSchoolId()));

        Chat chat = new Chat();
        chat.setSchool(school);
        chat.setChatType(request.getChatType());
        chat.setTitle(request.getTitle());
        return chatRepository.save(chat);
    }

    public List<Chat> list() {
        return chatRepository.findAll();
    }

    public Chat get(UUID id) {
        return chatRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Chat not found: " + id));
    }
}
