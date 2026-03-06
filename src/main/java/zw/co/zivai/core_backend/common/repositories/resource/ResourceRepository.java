package zw.co.zivai.core_backend.common.repositories.resource;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;

import zw.co.zivai.core_backend.common.models.lms.resources.Resource;

public interface ResourceRepository extends JpaRepository<Resource, UUID> {
    @EntityGraph(attributePaths = {"subject", "uploadedBy"})
    List<Resource> findBySubject_IdAndDeletedAtIsNull(UUID subjectId);
    @EntityGraph(attributePaths = {"subject", "uploadedBy"})
    List<Resource> findBySubject_IdAndStatusAndDeletedAtIsNull(UUID subjectId, String status);
    @EntityGraph(attributePaths = {"subject", "uploadedBy"})
    List<Resource> findByStatusAndDeletedAtIsNull(String status);
    @EntityGraph(attributePaths = {"subject", "uploadedBy"})
    List<Resource> findAllByOrderByCreatedAtDesc(Pageable pageable);
    @EntityGraph(attributePaths = {"subject", "uploadedBy"})
    List<Resource> findByDeletedAtIsNull();
    @EntityGraph(attributePaths = {"subject", "uploadedBy"})
    java.util.Optional<Resource> findByIdAndDeletedAtIsNull(UUID id);
}
