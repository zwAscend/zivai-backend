package zw.co.zivai.core_backend.common.repositories.development;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import zw.co.zivai.core_backend.common.models.lms.development.Plan;

public interface PlanRepository extends JpaRepository<Plan, UUID> {
    List<Plan> findBySubject_Id(UUID subjectId);
    List<Plan> findBySubject_IdAndDeletedAtIsNull(UUID subjectId);
    Optional<Plan> findByIdAndDeletedAtIsNull(UUID id);
}
