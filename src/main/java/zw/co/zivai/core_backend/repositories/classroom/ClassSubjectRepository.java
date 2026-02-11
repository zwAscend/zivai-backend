package zw.co.zivai.core_backend.repositories.classroom;

import java.util.UUID;
import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import zw.co.zivai.core_backend.models.lms.ClassSubject;

public interface ClassSubjectRepository extends JpaRepository<ClassSubject, UUID> {
    long countByDeletedAtIsNull();
    List<ClassSubject> findAllByDeletedAtIsNull();
    List<ClassSubject> findBySubject_IdAndDeletedAtIsNull(UUID subjectId);
    List<ClassSubject> findBySubject_IdAndTeacher_IdAndDeletedAtIsNull(UUID subjectId, UUID teacherId);
    List<ClassSubject> findByClassEntity_IdAndDeletedAtIsNull(UUID classId);
    @EntityGraph(attributePaths = {"subject", "classEntity"})
    List<ClassSubject> findByClassEntity_IdInAndDeletedAtIsNull(List<UUID> classIds);
    List<ClassSubject> findByClassEntity_IdAndSubject_IdAndDeletedAtIsNull(UUID classId, UUID subjectId);
}
