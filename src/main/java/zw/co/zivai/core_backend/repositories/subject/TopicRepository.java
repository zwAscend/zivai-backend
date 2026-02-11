package zw.co.zivai.core_backend.repositories.subject;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import zw.co.zivai.core_backend.models.lms.Topic;

public interface TopicRepository extends JpaRepository<Topic, UUID> {
    List<Topic> findBySubject_IdAndDeletedAtIsNullOrderBySequenceIndexAsc(UUID subjectId);
}
