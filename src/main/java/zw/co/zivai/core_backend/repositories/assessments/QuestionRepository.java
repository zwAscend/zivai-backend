package zw.co.zivai.core_backend.repositories.assessments;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import zw.co.zivai.core_backend.models.lms.Question;

public interface QuestionRepository extends JpaRepository<Question, UUID> {
    java.util.List<Question> findBySubject_IdAndDeletedAtIsNull(UUID subjectId);
}
