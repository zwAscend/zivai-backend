package zw.co.zivai.core_backend.repositories.assessments;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import zw.co.zivai.core_backend.models.lms.assessments.AssessmentAssignment;

public interface AssessmentAssignmentRepository extends JpaRepository<AssessmentAssignment, UUID> {
    @EntityGraph(attributePaths = {"assessment", "assessment.subject", "classEntity", "assignedBy"})
    List<AssessmentAssignment> findByDeletedAtIsNull();
    @EntityGraph(attributePaths = {"assessment", "assessment.subject", "classEntity", "assignedBy"})
    List<AssessmentAssignment> findByAssessment_IdAndDeletedAtIsNull(UUID assessmentId);
    default List<AssessmentAssignment> findByAssessment_Id(UUID assessmentId) {
        return findByAssessment_IdAndDeletedAtIsNull(assessmentId);
    }
    @EntityGraph(attributePaths = {"assessment", "assessment.subject", "classEntity", "assignedBy"})
    List<AssessmentAssignment> findByClassEntity_IdAndDeletedAtIsNull(UUID classId);
    @EntityGraph(attributePaths = {"assessment", "assessment.subject", "classEntity", "assignedBy"})
    List<AssessmentAssignment> findByPublishedAndDeletedAtIsNull(boolean published);
    @EntityGraph(attributePaths = {"assessment", "assessment.subject", "classEntity", "assignedBy"})
    java.util.Optional<AssessmentAssignment> findByIdAndDeletedAtIsNull(UUID id);

    @EntityGraph(attributePaths = {"assessment", "assessment.subject", "classEntity", "assignedBy"})
    @Query("""
        select distinct aa
        from AssessmentAssignment aa
        join aa.assessment a
        left join aa.classEntity ce
        where aa.deletedAt is null
          and a.deletedAt is null
          and a.subject.deletedAt is null
          and (
                aa.assignedBy.id = :teacherId
                or (:hasClassIds = true and ce.id in :classIds)
              )
          and (:hasSubjectId = false or a.subject.id = :subjectId)
          and (:hasStatus = false or lower(a.status) = :status)
          and (:hasSearch = false or lower(a.name) like concat('%', :search, '%'))
          and (:hasFromDate = false or aa.dueTime >= :fromDate)
          and (:hasToDate = false or aa.dueTime <= :toDate)
    """)
    List<AssessmentAssignment> findTeacherAssignments(
        @Param("teacherId") UUID teacherId,
        @Param("classIds") List<UUID> classIds,
        @Param("hasClassIds") boolean hasClassIds,
        @Param("subjectId") UUID subjectId,
        @Param("hasSubjectId") boolean hasSubjectId,
        @Param("status") String status,
        @Param("hasStatus") boolean hasStatus,
        @Param("search") String search,
        @Param("hasSearch") boolean hasSearch,
        @Param("fromDate") java.time.Instant fromDate,
        @Param("hasFromDate") boolean hasFromDate,
        @Param("toDate") java.time.Instant toDate,
        @Param("hasToDate") boolean hasToDate
    );

    @EntityGraph(attributePaths = {"assessment", "assessment.subject", "classEntity", "assignedBy"})
    @Query("""
        select distinct aa
        from AssessmentAssignment aa
        join aa.assessment a
        left join aa.classEntity ce
        where aa.deletedAt is null
          and a.deletedAt is null
          and a.subject.deletedAt is null
          and (
                aa.assignedBy.id = :teacherId
                or (:hasClassIds = true and ce.id in :classIds)
              )
          and (:subjectId is null or a.subject.id = :subjectId)
    """)
    List<AssessmentAssignment> findTeacherAssignmentsForProfile(
        @Param("teacherId") UUID teacherId,
        @Param("classIds") List<UUID> classIds,
        @Param("hasClassIds") boolean hasClassIds,
        @Param("subjectId") UUID subjectId
    );
}
