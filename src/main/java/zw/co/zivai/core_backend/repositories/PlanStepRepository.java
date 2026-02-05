package zw.co.zivai.core_backend.repositories;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import zw.co.zivai.core_backend.models.lms.PlanStep;

public interface PlanStepRepository extends JpaRepository<PlanStep, UUID> {
    List<PlanStep> findByPlan_IdOrderByStepOrderAsc(UUID planId);
}
