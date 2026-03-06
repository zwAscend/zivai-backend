package zw.co.zivai.core_backend.common.repositories.school;

import java.util.Optional;
import java.util.UUID;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import zw.co.zivai.core_backend.common.models.lms.school.School;

public interface SchoolRepository extends JpaRepository<School, UUID> {
    Optional<School> findByCode(String code);
    List<School> findAllByDeletedAtIsNull();
    Optional<School> findFirstByDeletedAtIsNullOrderByCreatedAtAsc();
    Optional<School> findByIdAndDeletedAtIsNull(UUID id);
    long countByDeletedAtIsNull();
}
