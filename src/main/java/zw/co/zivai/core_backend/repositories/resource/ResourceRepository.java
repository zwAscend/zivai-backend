package zw.co.zivai.core_backend.repositories.resource;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;

import zw.co.zivai.core_backend.models.lms.Resource;

public interface ResourceRepository extends JpaRepository<Resource, UUID> {
    List<Resource> findBySubject_Id(UUID subjectId);
    List<Resource> findBySubject_IdAndStatus(UUID subjectId, String status);
    List<Resource> findByStatus(String status);
    List<Resource> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
