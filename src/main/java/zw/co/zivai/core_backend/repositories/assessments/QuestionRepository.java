package zw.co.zivai.core_backend.repositories.assessments;

import java.util.UUID;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import zw.co.zivai.core_backend.models.lms.Question;

public interface QuestionRepository extends JpaRepository<Question, UUID> {
    List<Question> findBySubject_IdAndDeletedAtIsNull(UUID subjectId);

    @Query("""
        select q.topic.id, count(q.id)
        from Question q
        where q.subject.id = :subjectId
          and q.deletedAt is null
          and q.topic is not null
        group by q.topic.id
    """)
    List<Object[]> countQuestionsByTopic(@Param("subjectId") UUID subjectId);
}
