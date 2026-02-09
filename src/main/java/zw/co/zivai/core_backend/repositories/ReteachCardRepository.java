package zw.co.zivai.core_backend.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import zw.co.zivai.core_backend.models.lms.ReteachCard;

public interface ReteachCardRepository extends JpaRepository<ReteachCard, UUID> {
    List<ReteachCard> findByDeletedAtIsNull();
    Optional<ReteachCard> findByIdAndDeletedAtIsNull(UUID id);
    List<ReteachCard> findBySubject_IdAndDeletedAtIsNull(UUID subjectId);
    List<ReteachCard> findByTopic_IdAndDeletedAtIsNull(UUID topicId);
}
