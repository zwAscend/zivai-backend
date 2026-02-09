package zw.co.zivai.core_backend.repositories;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import zw.co.zivai.core_backend.models.lms.AssessmentAssignment;

public interface AssessmentAssignmentRepository extends JpaRepository<AssessmentAssignment, UUID> {
    List<AssessmentAssignment> findByAssessment_Id(UUID assessmentId);
    List<AssessmentAssignment> findByClassEntity_Id(UUID classId);
    List<AssessmentAssignment> findByPublished(boolean published);
}
