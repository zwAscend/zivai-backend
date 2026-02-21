package zw.co.zivai.core_backend.repositories.classroom;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import zw.co.zivai.core_backend.models.lms.StudentSubjectEnrolment;

public interface StudentSubjectEnrolmentRepository extends JpaRepository<StudentSubjectEnrolment, UUID> {
    @EntityGraph(attributePaths = {"student", "classSubject", "classSubject.subject"})
    List<StudentSubjectEnrolment> findByStudent_IdAndDeletedAtIsNull(UUID studentId);

    @EntityGraph(attributePaths = {"student", "classSubject", "classSubject.subject"})
    List<StudentSubjectEnrolment> findByStudent_IdInAndDeletedAtIsNull(List<UUID> studentIds);

    @EntityGraph(attributePaths = {"student", "classSubject", "classSubject.subject"})
    List<StudentSubjectEnrolment> findByClassSubject_IdAndDeletedAtIsNull(UUID classSubjectId);

    @EntityGraph(attributePaths = {"student", "classSubject", "classSubject.subject"})
    List<StudentSubjectEnrolment> findByClassSubject_IdInAndDeletedAtIsNull(List<UUID> classSubjectIds);

    @EntityGraph(attributePaths = {"student", "classSubject", "classSubject.subject"})
    List<StudentSubjectEnrolment> findByClassSubject_Subject_IdAndDeletedAtIsNull(UUID subjectId);
}
