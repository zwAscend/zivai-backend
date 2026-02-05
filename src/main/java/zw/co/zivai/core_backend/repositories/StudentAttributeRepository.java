package zw.co.zivai.core_backend.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import zw.co.zivai.core_backend.models.lms.StudentAttribute;

public interface StudentAttributeRepository extends JpaRepository<StudentAttribute, UUID> {
    List<StudentAttribute> findByStudent_Id(UUID studentId);
    List<StudentAttribute> findByStudent_IdAndSkill_Subject_Id(UUID studentId, UUID subjectId);
    Optional<StudentAttribute> findByStudent_IdAndSkill_Id(UUID studentId, UUID skillId);
}
