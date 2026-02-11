package zw.co.zivai.core_backend.repositories.subject;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import zw.co.zivai.core_backend.models.lms.Skill;

public interface SkillRepository extends JpaRepository<Skill, UUID> {
    List<Skill> findBySubject_Id(UUID subjectId);
    Optional<Skill> findBySubject_IdAndCode(UUID subjectId, String code);
    Optional<Skill> findBySubject_IdAndNameIgnoreCase(UUID subjectId, String name);
}
