package zw.co.zivai.core_backend.repositories;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import zw.co.zivai.core_backend.models.lms.GradingOverride;

public interface GradingOverrideRepository extends JpaRepository<GradingOverride, UUID> {
    Optional<GradingOverride> findTopByAttemptAnswer_IdOrderByOverriddenAtDesc(UUID attemptAnswerId);
}
