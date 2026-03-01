package zw.co.zivai.core_backend.repositories.classroom;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import zw.co.zivai.core_backend.models.lms.students.StudentSubjectEnrolment;
import zw.co.zivai.core_backend.models.lms.users.User;

public interface StudentSubjectEnrolmentRepository extends JpaRepository<StudentSubjectEnrolment, UUID> {
    @EntityGraph(attributePaths = {"student", "classSubject", "classSubject.subject"})
    List<StudentSubjectEnrolment> findByDeletedAtIsNull();

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

    @Query("""
        select distinct s
        from StudentSubjectEnrolment e
        join e.student s
        where e.deletedAt is null
          and s.deletedAt is null
          and e.classSubject.id = :classSubjectId
    """)
    List<User> findDistinctStudentsByClassSubjectId(@Param("classSubjectId") UUID classSubjectId);

    @Query("""
        select distinct s
        from StudentSubjectEnrolment e
        join e.student s
        join e.classSubject cs
        where e.deletedAt is null
          and s.deletedAt is null
          and cs.subject.id = :subjectId
    """)
    List<User> findDistinctStudentsBySubjectId(@Param("subjectId") UUID subjectId);
}
