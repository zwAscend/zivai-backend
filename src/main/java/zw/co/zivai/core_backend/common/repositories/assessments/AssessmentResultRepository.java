package zw.co.zivai.core_backend.common.repositories.assessments;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import zw.co.zivai.core_backend.common.models.lms.assessments.AssessmentResult;

public interface AssessmentResultRepository extends JpaRepository<AssessmentResult, UUID> {
    List<AssessmentResult> findByAssessmentAssignment_Id(UUID assessmentAssignmentId);
    List<AssessmentResult> findByAssessmentAssignment_IdAndStudent_Id(UUID assessmentAssignmentId, UUID studentId);
    @EntityGraph(attributePaths = {"student", "assessmentAssignment", "assessmentAssignment.assessment", "finalizedAttempt"})
    List<AssessmentResult> findByAssessmentAssignment_IdAndDeletedAtIsNull(UUID assessmentAssignmentId);
    @EntityGraph(attributePaths = {"student", "assessmentAssignment", "assessmentAssignment.assessment", "finalizedAttempt"})
    List<AssessmentResult> findByAssessmentAssignment_IdInAndDeletedAtIsNull(Collection<UUID> assessmentAssignmentIds);
    @EntityGraph(attributePaths = {"student", "assessmentAssignment", "assessmentAssignment.assessment", "finalizedAttempt"})
    List<AssessmentResult> findByAssessmentAssignment_IdInAndStudent_IdAndDeletedAtIsNull(Collection<UUID> assessmentAssignmentIds,
                                                                                           UUID studentId);
    Optional<AssessmentResult> findFirstByAssessmentAssignment_IdAndStudent_Id(UUID assessmentAssignmentId, UUID studentId);
    List<AssessmentResult> findByStudent_Id(UUID studentId);

    @Query("""
        select ar
        from AssessmentResult ar
        join fetch ar.assessmentAssignment aa
        where ar.student.id = :studentId
          and aa.id in :assignmentIds
          and ar.deletedAt is null
          and aa.deletedAt is null
    """)
    List<AssessmentResult> findForStudentHistory(@Param("studentId") UUID studentId,
                                                 @Param("assignmentIds") Collection<UUID> assignmentIds);

    @EntityGraph(attributePaths = {
        "student",
        "assessmentAssignment",
        "assessmentAssignment.assessment",
        "assessmentAssignment.assessment.subject",
        "assessmentAssignment.classEntity",
        "finalizedAttempt"
    })
    @Query("""
        select ar
        from AssessmentResult ar
        join ar.assessmentAssignment aa
        join aa.assessment a
        left join a.subject s
        left join aa.classEntity ce
        where ar.deletedAt is null
          and aa.deletedAt is null
          and a.deletedAt is null
          and (:subjectId is null or s.id = :subjectId)
          and (:classId is null or ce.id = :classId)
    """)
    List<AssessmentResult> findForClassReport(@Param("subjectId") UUID subjectId,
                                              @Param("classId") UUID classId);
}
