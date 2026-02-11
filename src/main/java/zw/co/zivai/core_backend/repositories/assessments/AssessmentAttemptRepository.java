package zw.co.zivai.core_backend.repositories.assessments;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
}
