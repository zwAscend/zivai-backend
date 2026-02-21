package zw.co.zivai.core_backend.repositories.assessments;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import zw.co.zivai.core_backend.models.lms.AssessmentEnrollment;

public interface AssessmentEnrollmentRepository extends JpaRepository<AssessmentEnrollment, UUID> {
    Optional<AssessmentEnrollment> findByAssessmentAssignment_IdAndStudent_Id(UUID assessmentAssignmentId, UUID studentId);
    List<AssessmentEnrollment> findByStudent_Id(UUID studentId);
    List<AssessmentEnrollment> findByAssessmentAssignment_Id(UUID assessmentAssignmentId);
    List<AssessmentEnrollment> findByAssessmentAssignment_IdInAndStudent_IdAndDeletedAtIsNull(Collection<UUID> assessmentAssignmentIds,
                                                                                               UUID studentId);
    List<AssessmentEnrollment> findByAssessmentAssignment_ClassEntity_Id(UUID classId);

    @Query("""
        select distinct ae
        from AssessmentEnrollment ae
        join fetch ae.assessmentAssignment aa
        join fetch aa.assessment a
        join fetch a.subject s
        left join fetch aa.classEntity ce
        where ae.student.id = :studentId
          and ae.deletedAt is null
          and aa.deletedAt is null
          and a.deletedAt is null
          and s.deletedAt is null
          and (:subjectId is null or s.id = :subjectId)
          and (:fromDate is null or aa.dueTime >= :fromDate)
          and (:toDate is null or aa.dueTime <= :toDate)
    """)
    List<AssessmentEnrollment> findStudentHistory(@Param("studentId") UUID studentId,
                                                  @Param("subjectId") UUID subjectId,
                                                  @Param("fromDate") Instant fromDate,
                                                  @Param("toDate") Instant toDate);

    @Query("""
        select distinct ae
        from AssessmentEnrollment ae
        join fetch ae.assessmentAssignment aa
        join fetch aa.assessment a
        join fetch a.subject s
        left join fetch aa.classEntity ce
        where ae.student.id = :studentId
          and a.id = :assessmentId
          and ae.deletedAt is null
          and aa.deletedAt is null
          and a.deletedAt is null
          and s.deletedAt is null
    """)
    List<AssessmentEnrollment> findStudentHistoryByAssessment(@Param("studentId") UUID studentId,
                                                              @Param("assessmentId") UUID assessmentId);
}
