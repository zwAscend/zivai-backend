package zw.co.zivai.core_backend.repositories.resource;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;

import zw.co.zivai.core_backend.models.lms.Resource;

public interface ResourceRepository extends JpaRepository<Resource, UUID> {
    @EntityGraph(attributePaths = {"subject", "uploadedBy"})
    List<Resource> findBySubject_Id(UUID subjectId);
    @EntityGraph(attributePaths = {"subject", "uploadedBy"})
    List<Resource> findBySubject_IdAndStatus(UUID subjectId, String status);
    @EntityGraph(attributePaths = {"subject", "uploadedBy"})
    List<Resource> findByStatus(String status);
    @EntityGraph(attributePaths = {"subject", "uploadedBy"})
    List<Resource> findAllByOrderByCreatedAtDesc(Pageable pageable);
    @EntityGraph(attributePaths = {"subject", "uploadedBy"})
    List<Resource> findByDeletedAtIsNull();
}
