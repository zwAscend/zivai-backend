package zw.co.zivai.core_backend.repositories.assessments;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import zw.co.zivai.core_backend.models.lms.Assessment;

public interface AssessmentRepository extends JpaRepository<Assessment, UUID> {
    @EntityGraph(attributePaths = {"school", "subject", "resource", "createdBy", "lastModifiedBy"})
    List<Assessment> findBySubject_IdAndDeletedAtIsNull(UUID subjectId);

    @EntityGraph(attributePaths = {"school", "subject", "resource", "createdBy", "lastModifiedBy"})
    List<Assessment> findBySubject_IdAndStatusAndDeletedAtIsNull(UUID subjectId, String status);

    @EntityGraph(attributePaths = {"school", "subject", "resource", "createdBy", "lastModifiedBy"})
    List<Assessment> findByStatusAndDeletedAtIsNull(String status);

    @EntityGraph(attributePaths = {"school", "subject", "resource", "createdBy", "lastModifiedBy"})
    List<Assessment> findByDeletedAtIsNull();

    @EntityGraph(attributePaths = {"school", "subject", "resource", "createdBy", "lastModifiedBy"})
    Optional<Assessment> findByIdAndDeletedAtIsNull(UUID id);
}
