package zw.co.zivai.core_backend.common.repositories.assessments;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import zw.co.zivai.core_backend.common.models.lms.assessments.AssessmentEnrollment;

public interface AssessmentEnrollmentRepository extends JpaRepository<AssessmentEnrollment, UUID> {
    @EntityGraph(attributePaths = {"assessmentAssignment", "assessmentAssignment.assessment", "assessmentAssignment.classEntity", "student"})
    Optional<AssessmentEnrollment> findByAssessmentAssignment_IdAndStudent_Id(UUID assessmentAssignmentId, UUID studentId);
    @EntityGraph(attributePaths = {"assessmentAssignment", "assessmentAssignment.assessment", "assessmentAssignment.classEntity", "student"})
    List<AssessmentEnrollment> findByDeletedAtIsNull();
    @EntityGraph(attributePaths = {"assessmentAssignment", "assessmentAssignment.assessment", "assessmentAssignment.classEntity", "student"})
    List<AssessmentEnrollment> findByStudent_IdAndDeletedAtIsNull(UUID studentId);
    default List<AssessmentEnrollment> findByStudent_Id(UUID studentId) {
        return findByStudent_IdAndDeletedAtIsNull(studentId);
    }
    @EntityGraph(attributePaths = {"assessmentAssignment", "assessmentAssignment.assessment", "assessmentAssignment.classEntity", "student"})
    List<AssessmentEnrollment> findByAssessmentAssignment_IdAndDeletedAtIsNull(UUID assessmentAssignmentId);
    default List<AssessmentEnrollment> findByAssessmentAssignment_Id(UUID assessmentAssignmentId) {
        return findByAssessmentAssignment_IdAndDeletedAtIsNull(assessmentAssignmentId);
    }
    List<AssessmentEnrollment> findByAssessmentAssignment_IdAndStudent_IdIn(UUID assessmentAssignmentId, Collection<UUID> studentIds);
    List<AssessmentEnrollment> findByAssessmentAssignment_IdInAndStudent_IdAndDeletedAtIsNull(Collection<UUID> assessmentAssignmentIds,
                                                                                               UUID studentId);
    @EntityGraph(attributePaths = {"assessmentAssignment", "assessmentAssignment.assessment", "assessmentAssignment.classEntity", "student"})
    List<AssessmentEnrollment> findByAssessmentAssignment_ClassEntity_IdAndDeletedAtIsNull(UUID classId);
    @EntityGraph(attributePaths = {"assessmentAssignment", "assessmentAssignment.assessment", "assessmentAssignment.classEntity", "student"})
    Optional<AssessmentEnrollment> findByIdAndDeletedAtIsNull(UUID id);

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
          and aa.published = true
          and a.deletedAt is null
          and s.deletedAt is null
          and (:subjectId is null or s.id = :subjectId)
          and (:applyFrom = false or aa.dueTime >= :fromDate)
          and (:applyTo = false or aa.dueTime <= :toDate)
    """)
    List<AssessmentEnrollment> findStudentHistory(@Param("studentId") UUID studentId,
                                                  @Param("subjectId") UUID subjectId,
                                                  @Param("applyFrom") boolean applyFrom,
                                                  @Param("fromDate") Instant fromDate,
                                                  @Param("applyTo") boolean applyTo,
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
          and aa.published = true
          and a.deletedAt is null
          and s.deletedAt is null
    """)
    List<AssessmentEnrollment> findStudentHistoryByAssessment(@Param("studentId") UUID studentId,
                                                              @Param("assessmentId") UUID assessmentId);
}
