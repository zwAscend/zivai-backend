package zw.co.zivai.core_backend.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import zw.co.zivai.core_backend.models.lms.AssessmentEnrollment;

public interface AssessmentEnrollmentRepository extends JpaRepository<AssessmentEnrollment, UUID> {
    Optional<AssessmentEnrollment> findByAssessmentAssignment_IdAndStudent_Id(UUID assessmentAssignmentId, UUID studentId);
    List<AssessmentEnrollment> findByStudent_Id(UUID studentId);
    List<AssessmentEnrollment> findByAssessmentAssignment_Id(UUID assessmentAssignmentId);
    List<AssessmentEnrollment> findByAssessmentAssignment_ClassEntity_Id(UUID classId);
}
