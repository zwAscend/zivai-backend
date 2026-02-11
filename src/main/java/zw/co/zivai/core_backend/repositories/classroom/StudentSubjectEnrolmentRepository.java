package zw.co.zivai.core_backend.repositories.classroom;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import zw.co.zivai.core_backend.models.lms.StudentSubjectEnrolment;

public interface StudentSubjectEnrolmentRepository extends JpaRepository<StudentSubjectEnrolment, UUID> {
    List<StudentSubjectEnrolment> findByStudent_IdAndDeletedAtIsNull(UUID studentId);
    List<StudentSubjectEnrolment> findByClassSubject_IdAndDeletedAtIsNull(UUID classSubjectId);
    List<StudentSubjectEnrolment> findByClassSubject_Subject_IdAndDeletedAtIsNull(UUID subjectId);
}
