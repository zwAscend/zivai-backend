package zw.co.zivai.core_backend.common.repositories.assessments;

import java.util.List;
import java.util.Optional;
import java.util.Collection;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import zw.co.zivai.core_backend.common.models.lms.assessments.AttemptAnswer;
import zw.co.zivai.core_backend.common.dtos.assessments.TopicAnswerStat;
import zw.co.zivai.core_backend.common.dtos.assessments.SubjectTopicAnswerStat;

public interface AttemptAnswerRepository extends JpaRepository<AttemptAnswer, UUID> {
    List<AttemptAnswer> findByAssessmentAttempt_Id(UUID assessmentAttemptId);
    List<AttemptAnswer> findByAssessmentAttempt_IdAndDeletedAtIsNull(UUID assessmentAttemptId);
    Optional<AttemptAnswer> findByAssessmentAttempt_IdAndAssessmentQuestion_IdAndDeletedAtIsNull(UUID assessmentAttemptId, UUID assessmentQuestionId);
    Optional<AttemptAnswer> findFirstByAssessmentAttempt_IdOrderByCreatedAtAsc(UUID assessmentAttemptId);
    long countByAssessmentAttempt_Id(UUID assessmentAttemptId);
    @Query("""
        select aa
        from AttemptAnswer aa
        join fetch aa.assessmentQuestion aq
        join fetch aq.question q
        left join fetch aq.rubricScheme rs
        where aa.assessmentAttempt.id = :assessmentAttemptId
          and aa.deletedAt is null
          and aq.deletedAt is null
          and q.deletedAt is null
        order by aq.sequenceIndex asc, aa.createdAt asc
    """)
    List<AttemptAnswer> findReviewDetailsByAssessmentAttemptId(@Param("assessmentAttemptId") UUID assessmentAttemptId);

    @Query("""
        select new zw.co.zivai.core_backend.common.dtos.assessments.TopicAnswerStat(
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

    @Query("""
        select new zw.co.zivai.core_backend.common.dtos.assessments.TopicAnswerStat(
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
          and ae.student.id = :studentId
          and aa.deletedAt is null
          and aq.deletedAt is null
          and q.deletedAt is null
          and at.deletedAt is null
          and ae.deletedAt is null
    """)
    List<TopicAnswerStat> findTopicStatsBySubjectAndStudent(@Param("subjectId") UUID subjectId,
                                                             @Param("studentId") UUID studentId);

    @Query("""
        select new zw.co.zivai.core_backend.common.dtos.assessments.TopicAnswerStat(
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
        where ae.assessmentAssignment.id = :assessmentAssignmentId
          and aa.deletedAt is null
          and aq.deletedAt is null
          and q.deletedAt is null
          and at.deletedAt is null
          and ae.deletedAt is null
    """)
    List<TopicAnswerStat> findTopicStatsByAssessmentAssignment(@Param("assessmentAssignmentId") UUID assessmentAssignmentId);

    @Query("""
        select new zw.co.zivai.core_backend.common.dtos.assessments.SubjectTopicAnswerStat(
            q.subject.id,
            q.topic.id,
            coalesce(aa.humanScore, aa.aiScore, 0),
            aa.maxScore
        )
        from AttemptAnswer aa
        join aa.assessmentQuestion aq
        join aq.question q
        join aa.assessmentAttempt at
        join at.assessmentEnrollment ae
        where ae.student.id = :studentId
          and aa.deletedAt is null
          and aq.deletedAt is null
          and q.deletedAt is null
          and at.deletedAt is null
          and ae.deletedAt is null
    """)
    List<SubjectTopicAnswerStat> findTopicStatsByStudent(@Param("studentId") UUID studentId);

    @Query("""
        select
            aa.assessmentAttempt.id,
            count(aa.id),
            sum(case when coalesce(aa.humanScore, aa.aiScore, 0) >= aa.maxScore and aa.maxScore > 0 then 1 else 0 end),
            sum(coalesce(aa.humanScore, aa.aiScore, 0)),
            sum(coalesce(aa.maxScore, 0))
        from AttemptAnswer aa
        where aa.assessmentAttempt.id in :attemptIds
          and aa.deletedAt is null
        group by aa.assessmentAttempt.id
    """)
    List<Object[]> summarizeByAssessmentAttemptIds(@Param("attemptIds") Collection<UUID> attemptIds);
}
