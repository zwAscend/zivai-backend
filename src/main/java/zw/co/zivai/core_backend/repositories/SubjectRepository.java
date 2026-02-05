package zw.co.zivai.core_backend.repositories;

import java.util.Optional;
import java.util.UUID;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import zw.co.zivai.core_backend.models.lms.Subject;

public interface SubjectRepository extends JpaRepository<Subject, UUID> {
    Optional<Subject> findByCode(String code);
    Optional<Subject> findByCodeAndDeletedAtIsNull(String code);
    List<Subject> findAllByDeletedAtIsNull();
    Optional<Subject> findByIdAndDeletedAtIsNull(UUID id);
    long countByDeletedAtIsNull();
    long countByDeletedAtIsNullAndActiveTrue();
}
