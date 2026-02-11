package zw.co.zivai.core_backend.repositories.development;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import zw.co.zivai.core_backend.models.lms.PlanSubskill;

public interface PlanSubskillRepository extends JpaRepository<PlanSubskill, UUID> {
    List<PlanSubskill> findByPlanSkill_Id(UUID planSkillId);
}
