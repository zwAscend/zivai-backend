package zw.co.zivai.core_backend.repositories;

import java.util.UUID;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import zw.co.zivai.core_backend.models.lms.ClassSubject;

public interface ClassSubjectRepository extends JpaRepository<ClassSubject, UUID> {
    long countByDeletedAtIsNull();
    List<ClassSubject> findAllByDeletedAtIsNull();
    List<ClassSubject> findBySubject_IdAndDeletedAtIsNull(UUID subjectId);
    List<ClassSubject> findByClassEntity_IdAndDeletedAtIsNull(UUID classId);
    List<ClassSubject> findByClassEntity_IdAndSubject_IdAndDeletedAtIsNull(UUID classId, UUID subjectId);
}
