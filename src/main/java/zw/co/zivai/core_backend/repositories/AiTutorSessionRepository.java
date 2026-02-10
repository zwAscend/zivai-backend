package zw.co.zivai.core_backend.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import zw.co.zivai.core_backend.models.lms.AiTutorSession;

public interface AiTutorSessionRepository extends JpaRepository<AiTutorSession, UUID> {
    Optional<AiTutorSession> findByIdAndDeletedAtIsNull(UUID id);
    Optional<AiTutorSession> findByStudent_IdAndSubject_IdAndDeletedAtIsNull(UUID studentId, UUID subjectId);
    List<AiTutorSession> findByStudent_IdAndDeletedAtIsNull(UUID studentId);
}
