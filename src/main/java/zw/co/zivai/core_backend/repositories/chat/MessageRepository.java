package zw.co.zivai.core_backend.repositories.chat;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import zw.co.zivai.core_backend.models.lms.Message;

public interface MessageRepository extends JpaRepository<Message, UUID> {
    List<Message> findByChatIdOrderByTsAsc(UUID chatId);
    List<Message> findByChatIdAndReadFalse(UUID chatId);
    java.util.Optional<Message> findFirstByChatIdOrderByTsDesc(UUID chatId);
}
