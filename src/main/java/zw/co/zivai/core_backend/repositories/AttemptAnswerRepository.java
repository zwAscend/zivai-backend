package zw.co.zivai.core_backend.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import zw.co.zivai.core_backend.models.lms.AttemptAnswer;

public interface AttemptAnswerRepository extends JpaRepository<AttemptAnswer, UUID> {
    List<AttemptAnswer> findByAssessmentAttempt_Id(UUID assessmentAttemptId);
    Optional<AttemptAnswer> findFirstByAssessmentAttempt_IdOrderByCreatedAtAsc(UUID assessmentAttemptId);
}
