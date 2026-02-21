package zw.co.zivai.core_backend.repositories.classroom;

import java.util.Optional;
import java.util.UUID;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import zw.co.zivai.core_backend.models.lms.ClassEntity;

public interface ClassRepository extends JpaRepository<ClassEntity, UUID> {
    Optional<ClassEntity> findByCode(String code);
    Optional<ClassEntity> findByCodeAndDeletedAtIsNull(String code);
    List<ClassEntity> findAllByDeletedAtIsNull();
    List<ClassEntity> findByIdInAndDeletedAtIsNull(List<UUID> ids);
    Optional<ClassEntity> findByIdAndDeletedAtIsNull(UUID id);
    long countByDeletedAtIsNull();
}
