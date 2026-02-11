package zw.co.zivai.core_backend.repositories.development;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import zw.co.zivai.core_backend.models.lms.PlanSkill;

public interface PlanSkillRepository extends JpaRepository<PlanSkill, UUID> {
    List<PlanSkill> findByPlan_Id(UUID planId);
}
