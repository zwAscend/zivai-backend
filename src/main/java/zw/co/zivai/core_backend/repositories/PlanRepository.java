package zw.co.zivai.core_backend.repositories;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import zw.co.zivai.core_backend.models.lms.Plan;

public interface PlanRepository extends JpaRepository<Plan, UUID> {
    List<Plan> findBySubject_Id(UUID subjectId);
}
