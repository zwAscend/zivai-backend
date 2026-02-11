package zw.co.zivai.core_backend.repositories.assessments;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import zw.co.zivai.core_backend.models.lms.AttemptAnswer;
import zw.co.zivai.core_backend.dtos.assessments.TopicAnswerStat;

public interface AttemptAnswerRepository extends JpaRepository<AttemptAnswer, UUID> {
    List<AttemptAnswer> findByAssessmentAttempt_Id(UUID assessmentAttemptId);
    Optional<AttemptAnswer> findFirstByAssessmentAttempt_IdOrderByCreatedAtAsc(UUID assessmentAttemptId);
    long countByAssessmentAttempt_Id(UUID assessmentAttemptId);

    @Query("""
        select new zw.co.zivai.core_backend.dtos.assessments.TopicAnswerStat(
            q.topic.id,
            ae.student.id,
            aq.id,
            coalesce(aa.humanScore, aa.aiScore, 0),
            aa.maxScore
        )
        from AttemptAnswer aa
        join aa.assessmentQuestion aq
        join aq.question q
        join aa.assessmentAttempt at
        join at.assessmentEnrollment ae
        where q.subject.id = :subjectId
          and aa.deletedAt is null
          and aq.deletedAt is null
          and q.deletedAt is null
          and at.deletedAt is null
          and ae.deletedAt is null
    """)
    List<TopicAnswerStat> findTopicStatsBySubject(@Param("subjectId") UUID subjectId);
}
