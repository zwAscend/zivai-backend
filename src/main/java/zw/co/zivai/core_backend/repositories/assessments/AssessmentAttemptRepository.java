package zw.co.zivai.core_backend.repositories.assessments;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import zw.co.zivai.core_backend.models.lms.AssessmentAttempt;

public interface AssessmentAttemptRepository extends JpaRepository<AssessmentAttempt, UUID> {
    List<AssessmentAttempt> findByAssessmentEnrollment_Student_Id(UUID studentId);
    List<AssessmentAttempt> findByAssessmentEnrollment_AssessmentAssignment_Id(UUID assessmentAssignmentId);
    Optional<AssessmentAttempt> findTopByAssessmentEnrollment_IdOrderByAttemptNumberDesc(UUID assessmentEnrollmentId);

    @Query("""
        select at
        from AssessmentAttempt at
        join fetch at.assessmentEnrollment ae
        join fetch ae.student st
        join fetch ae.assessmentAssignment aa
        join fetch aa.assessment a
        left join fetch aa.classEntity ce
        left join fetch a.subject s
        where at.submittedAt is not null
          and at.deletedAt is null
          and ae.deletedAt is null
          and aa.deletedAt is null
          and a.deletedAt is null
          and (:subjectId is null or s.id = :subjectId)
          and (:classId is null or ce.id = :classId)
    """)
    List<AssessmentAttempt> findSubmittedForReport(@Param("subjectId") UUID subjectId,
                                                   @Param("classId") UUID classId);

    @Query("""
        select at
        from AssessmentAttempt at
        join fetch at.assessmentEnrollment ae
        join fetch ae.student st
        join fetch ae.assessmentAssignment aa
        join fetch aa.assessment a
        left join fetch a.subject s
        where at.submittedAt is not null
          and at.deletedAt is null
          and ae.deletedAt is null
          and aa.deletedAt is null
          and a.deletedAt is null
          and ae.student.id = :studentId
          and (:subjectId is null or s.id = :subjectId)
    """)
    List<AssessmentAttempt> findSubmittedByStudentForReport(@Param("studentId") UUID studentId,
                                                            @Param("subjectId") UUID subjectId);

    @Query("""
        select at
        from AssessmentAttempt at
        join fetch at.assessmentEnrollment ae
        where ae.id in :enrollmentIds
          and at.deletedAt is null
          and ae.deletedAt is null
        order by ae.id asc, at.attemptNumber desc, at.createdAt desc
    """)
    List<AssessmentAttempt> findForStudentHistory(@Param("enrollmentIds") Collection<UUID> enrollmentIds);

    @EntityGraph(attributePaths = {
        "assessmentEnrollment",
        "assessmentEnrollment.student",
        "assessmentEnrollment.assessmentAssignment",
        "assessmentEnrollment.assessmentAssignment.assessment",
        "assessmentEnrollment.assessmentAssignment.assessment.subject",
        "assessmentEnrollment.assessmentAssignment.classEntity"
    })
    @Query("""
        select at
        from AssessmentAttempt at
        join at.assessmentEnrollment ae
        join ae.assessmentAssignment aa
        join aa.assessment a
        left join aa.classEntity ce
        where at.submittedAt is not null
          and at.deletedAt is null
          and ae.deletedAt is null
          and aa.deletedAt is null
          and a.deletedAt is null
          and (:subjectId is null or a.subject.id = :subjectId)
          and (:classId is null or ce.id = :classId)
          and (:assessmentId is null or a.id = :assessmentId)
          and (:studentId is null or ae.student.id = :studentId)
          and (
                :teacherId is null
                or aa.assignedBy.id = :teacherId
                or (:hasClassIds = true and ce.id in :classIds)
              )
          and (
                (:status is null and lower(coalesce(at.gradingStatusCode, '')) <> 'reviewed')
                or (:status is not null and lower(at.gradingStatusCode) = lower(:status))
              )
    """)
    Page<AssessmentAttempt> findTeacherPending(
        @Param("teacherId") UUID teacherId,
        @Param("classIds") List<UUID> classIds,
        @Param("hasClassIds") boolean hasClassIds,
        @Param("subjectId") UUID subjectId,
        @Param("classId") UUID classId,
        @Param("assessmentId") UUID assessmentId,
        @Param("studentId") UUID studentId,
        @Param("status") String status,
        Pageable pageable
    );

    @EntityGraph(attributePaths = {
        "assessmentEnrollment",
        "assessmentEnrollment.student",
        "assessmentEnrollment.assessmentAssignment",
        "assessmentEnrollment.assessmentAssignment.assessment",
        "assessmentEnrollment.assessmentAssignment.assessment.subject",
        "assessmentEnrollment.assessmentAssignment.classEntity"
    })
    @Query("""
        select at
        from AssessmentAttempt at
        join at.assessmentEnrollment ae
        join ae.assessmentAssignment aa
        join aa.assessment a
        left join aa.classEntity ce
        where at.submittedAt is not null
          and at.deletedAt is null
          and ae.deletedAt is null
          and aa.deletedAt is null
          and a.deletedAt is null
          and (
                aa.assignedBy.id = :teacherId
                or (:hasClassIds = true and ce.id in :classIds)
              )
          and (:subjectId is null or a.subject.id = :subjectId)
    """)
    List<AssessmentAttempt> findTeacherSubmittedAttempts(
        @Param("teacherId") UUID teacherId,
        @Param("classIds") List<UUID> classIds,
        @Param("hasClassIds") boolean hasClassIds,
        @Param("subjectId") UUID subjectId
    );
}
