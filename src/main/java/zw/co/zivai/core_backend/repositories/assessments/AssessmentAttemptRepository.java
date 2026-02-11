package zw.co.zivai.core_backend.repositories.assessments;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import zw.co.zivai.core_backend.models.lms.AssessmentAttempt;

public interface AssessmentAttemptRepository extends JpaRepository<AssessmentAttempt, UUID> {
    List<AssessmentAttempt> findByAssessmentEnrollment_Student_Id(UUID studentId);
    List<AssessmentAttempt> findByAssessmentEnrollment_AssessmentAssignment_Id(UUID assessmentAssignmentId);
    Optional<AssessmentAttempt> findTopByAssessmentEnrollment_IdOrderByAttemptNumberDesc(UUID assessmentEnrollmentId);
}
