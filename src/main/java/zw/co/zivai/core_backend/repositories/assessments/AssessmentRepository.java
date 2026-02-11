package zw.co.zivai.core_backend.repositories.assessments;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import zw.co.zivai.core_backend.models.lms.Assessment;

public interface AssessmentRepository extends JpaRepository<Assessment, UUID> {
    java.util.List<Assessment> findBySubject_Id(UUID subjectId);
    java.util.List<Assessment> findBySubject_IdAndStatus(UUID subjectId, String status);
    java.util.List<Assessment> findByStatus(String status);
}
