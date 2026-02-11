package zw.co.zivai.core_backend.repositories.user;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import zw.co.zivai.core_backend.models.lms.User;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    List<User> findByRoles_Code(String code);
    Optional<User> findByEmailAndDeletedAtIsNull(String email);
    List<User> findByRoles_CodeAndDeletedAtIsNull(String code);
    List<User> findAllByDeletedAtIsNull();
    Optional<User> findByIdAndDeletedAtIsNull(UUID id);
    long countByDeletedAtIsNull();
    long countByDeletedAtIsNullAndActiveTrue();
    long countByDeletedAtIsNullAndRoles_Code(String code);
}
