package zw.co.zivai.core_backend.common.services.chat;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import zw.co.zivai.core_backend.common.dtos.chat.CreateChatRequest;
import zw.co.zivai.core_backend.common.exceptions.NotFoundException;
import zw.co.zivai.core_backend.common.models.lms.chat.Chat;
import zw.co.zivai.core_backend.common.models.lms.school.School;
import zw.co.zivai.core_backend.common.repositories.chat.ChatRepository;
import zw.co.zivai.core_backend.common.repositories.school.SchoolRepository;

@Service
@RequiredArgsConstructor
public class ChatService {
    private final ChatRepository chatRepository;
    private final SchoolRepository schoolRepository;

    public Chat create(CreateChatRequest request) {
        School school = schoolRepository.findByIdAndDeletedAtIsNull(request.getSchoolId())
            .orElseThrow(() -> new NotFoundException("School not found: " + request.getSchoolId()));

        Chat chat = new Chat();
        chat.setSchool(school);
        chat.setChatType(request.getChatType());
        chat.setTitle(request.getTitle());
        return chatRepository.save(chat);
    }

    public List<Chat> list() {
        return chatRepository.findAllByDeletedAtIsNull();
    }

    public Chat get(UUID id) {
        return chatRepository.findByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> new NotFoundException("Chat not found: " + id));
    }
}
