package zw.co.zivai.core_backend.repositories.development;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import zw.co.zivai.core_backend.models.lms.development.PlanStep;

public interface PlanStepRepository extends JpaRepository<PlanStep, UUID> {
    List<PlanStep> findByPlan_IdOrderByStepOrderAsc(UUID planId);
    List<PlanStep> findByPlan_IdInOrderByPlan_IdAscStepOrderAsc(List<UUID> planIds);
    Optional<PlanStep> findByIdAndPlan_Id(UUID id, UUID planId);
}
