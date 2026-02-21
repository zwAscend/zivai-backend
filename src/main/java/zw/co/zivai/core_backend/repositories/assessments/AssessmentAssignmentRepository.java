package zw.co.zivai.core_backend.repositories.assessments;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import zw.co.zivai.core_backend.models.lms.AssessmentAssignment;

public interface AssessmentAssignmentRepository extends JpaRepository<AssessmentAssignment, UUID> {
    List<AssessmentAssignment> findByAssessment_Id(UUID assessmentId);
    List<AssessmentAssignment> findByClassEntity_Id(UUID classId);
    List<AssessmentAssignment> findByPublished(boolean published);

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
          and (:status is null or lower(a.status) = lower(:status))
          and (:search is null or lower(a.name) like lower(concat('%', :search, '%')))
          and (:fromDate is null or aa.dueTime >= :fromDate)
          and (:toDate is null or aa.dueTime <= :toDate)
    """)
    List<AssessmentAssignment> findTeacherAssignments(
        @Param("teacherId") UUID teacherId,
        @Param("classIds") List<UUID> classIds,
        @Param("hasClassIds") boolean hasClassIds,
        @Param("subjectId") UUID subjectId,
        @Param("status") String status,
        @Param("search") String search,
        @Param("fromDate") java.time.Instant fromDate,
        @Param("toDate") java.time.Instant toDate
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
