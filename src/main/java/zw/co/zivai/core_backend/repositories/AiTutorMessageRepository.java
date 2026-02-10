package zw.co.zivai.core_backend.repositories;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import zw.co.zivai.core_backend.models.lms.AiTutorMessage;

public interface AiTutorMessageRepository extends JpaRepository<AiTutorMessage, UUID> {
    List<AiTutorMessage> findBySession_IdAndDeletedAtIsNullOrderByTsAsc(UUID sessionId);
}
