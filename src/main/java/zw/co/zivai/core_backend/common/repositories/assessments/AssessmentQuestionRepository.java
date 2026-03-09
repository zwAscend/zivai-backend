package zw.co.zivai.core_backend.common.repositories.assessments;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.Collection;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import zw.co.zivai.core_backend.common.models.lms.assessments.AssessmentQuestion;

public interface AssessmentQuestionRepository extends JpaRepository<AssessmentQuestion, UUID> {
    Optional<AssessmentQuestion> findFirstByAssessment_IdOrderBySequenceIndexAsc(UUID assessmentId);
    @EntityGraph(attributePaths = {"question"})
    List<AssessmentQuestion> findByAssessment_IdOrderBySequenceIndexAsc(UUID assessmentId);
    @EntityGraph(attributePaths = {"question", "question.topic"})
    List<AssessmentQuestion> findByAssessment_IdAndDeletedAtIsNullOrderBySequenceIndexAsc(UUID assessmentId);
    Optional<AssessmentQuestion> findByIdAndAssessment_IdAndDeletedAtIsNull(UUID id, UUID assessmentId);
    void deleteByAssessment_Id(UUID assessmentId);

    @Query("""
        select aq.assessment.id, count(aq.id)
        from AssessmentQuestion aq
        where aq.assessment.id in :assessmentIds
          and aq.deletedAt is null
        group by aq.assessment.id
    """)
    List<Object[]> countByAssessmentIds(@Param("assessmentIds") Collection<UUID> assessmentIds);
}
