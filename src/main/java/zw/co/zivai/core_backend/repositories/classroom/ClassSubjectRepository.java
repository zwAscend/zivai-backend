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
    @EntityGraph(attributePaths = {"subject", "classEntity", "teacher"})
    List<ClassSubject> findByTeacher_IdAndDeletedAtIsNull(UUID teacherId);
    List<ClassSubject> findBySubject_IdAndTeacher_IdAndDeletedAtIsNull(UUID subjectId, UUID teacherId);
    @EntityGraph(attributePaths = {"subject", "classEntity", "teacher"})
    List<ClassSubject> findByTeacher_IdAndClassEntity_IdAndDeletedAtIsNull(UUID teacherId, UUID classId);
    @EntityGraph(attributePaths = {"subject", "classEntity", "teacher"})
    List<ClassSubject> findByTeacher_IdAndClassEntity_IdAndSubject_IdAndDeletedAtIsNull(UUID teacherId, UUID classId, UUID subjectId);
    List<ClassSubject> findByClassEntity_IdAndDeletedAtIsNull(UUID classId);
    @EntityGraph(attributePaths = {"subject", "classEntity", "teacher"})
    List<ClassSubject> findByClassEntity_IdInAndDeletedAtIsNull(List<UUID> classIds);
    List<ClassSubject> findByClassEntity_IdAndSubject_IdAndDeletedAtIsNull(UUID classId, UUID subjectId);
}
