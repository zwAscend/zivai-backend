package zw.co.zivai.core_backend.repositories.assessments;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import zw.co.zivai.core_backend.models.lms.AssessmentQuestion;

public interface AssessmentQuestionRepository extends JpaRepository<AssessmentQuestion, UUID> {
    Optional<AssessmentQuestion> findFirstByAssessment_IdOrderBySequenceIndexAsc(UUID assessmentId);
    List<AssessmentQuestion> findByAssessment_IdOrderBySequenceIndexAsc(UUID assessmentId);
    void deleteByAssessment_Id(UUID assessmentId);
}
