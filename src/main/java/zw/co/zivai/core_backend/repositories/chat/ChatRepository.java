package zw.co.zivai.core_backend.repositories.chat;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import zw.co.zivai.core_backend.models.lms.chat.Chat;

public interface ChatRepository extends JpaRepository<Chat, UUID> {
    List<Chat> findAllByDeletedAtIsNull();
    Optional<Chat> findByIdAndDeletedAtIsNull(UUID id);
}
