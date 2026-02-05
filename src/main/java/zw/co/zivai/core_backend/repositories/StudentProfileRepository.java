package zw.co.zivai.core_backend.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import zw.co.zivai.core_backend.models.lms.StudentProfile;

public interface StudentProfileRepository extends JpaRepository<StudentProfile, UUID> {
    Optional<StudentProfile> findByUserIdAndDeletedAtIsNull(UUID userId);
    List<StudentProfile> findByUserIdInAndDeletedAtIsNull(List<UUID> userIds);
}
