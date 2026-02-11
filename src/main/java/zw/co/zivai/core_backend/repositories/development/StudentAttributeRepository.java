package zw.co.zivai.core_backend.repositories.development;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import zw.co.zivai.core_backend.models.lms.StudentAttribute;

public interface StudentAttributeRepository extends JpaRepository<StudentAttribute, UUID> {
    @EntityGraph(attributePaths = {"skill"})
    List<StudentAttribute> findByStudent_Id(UUID studentId);

    @EntityGraph(attributePaths = {"skill"})
    List<StudentAttribute> findByStudent_IdIn(List<UUID> studentIds);

    @EntityGraph(attributePaths = {"skill"})
    List<StudentAttribute> findByStudent_IdAndSkill_Subject_Id(UUID studentId, UUID subjectId);

    @EntityGraph(attributePaths = {"skill"})
    Optional<StudentAttribute> findByStudent_IdAndSkill_Id(UUID studentId, UUID skillId);
}
