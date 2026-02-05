package zw.co.zivai.core_backend.repositories;

import java.util.Optional;
import java.util.UUID;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import zw.co.zivai.core_backend.models.lms.School;

public interface SchoolRepository extends JpaRepository<School, UUID> {
    Optional<School> findByCode(String code);
    List<School> findAllByDeletedAtIsNull();
    Optional<School> findByIdAndDeletedAtIsNull(UUID id);
    long countByDeletedAtIsNull();
}
