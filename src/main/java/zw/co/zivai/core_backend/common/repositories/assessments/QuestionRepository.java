package zw.co.zivai.core_backend.common.repositories.assessments;

import java.util.UUID;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import zw.co.zivai.core_backend.common.models.lms.resources.Question;

public interface QuestionRepository extends JpaRepository<Question, UUID> {
    List<Question> findBySubject_IdAndDeletedAtIsNull(UUID subjectId);
    List<Question> findBySubject_IdAndTopic_IdAndDeletedAtIsNull(UUID subjectId, UUID topicId);
    List<Question> findBySubject_IdAndActiveTrueAndDeletedAtIsNull(UUID subjectId);
    List<Question> findBySubject_IdAndTopic_IdAndActiveTrueAndDeletedAtIsNull(UUID subjectId, UUID topicId);

    @Query("""
        select q.topic.id, count(q.id)
        from Question q
        where q.subject.id = :subjectId
          and q.deletedAt is null
          and q.active = true
          and q.topic is not null
        group by q.topic.id
    """)
    List<Object[]> countQuestionsByTopic(@Param("subjectId") UUID subjectId);
}
