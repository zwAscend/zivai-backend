package zw.co.zivai.core_backend.common.repositories.assessments;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import zw.co.zivai.core_backend.common.models.lms.assessments.GradingOverride;

public interface GradingOverrideRepository extends JpaRepository<GradingOverride, UUID> {
    Optional<GradingOverride> findTopByAttemptAnswer_IdOrderByOverriddenAtDesc(UUID attemptAnswerId);
}
