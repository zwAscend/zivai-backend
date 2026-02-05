package zw.co.zivai.core_backend.repositories;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import zw.co.zivai.core_backend.models.lms.StudentSubjectEnrolment;

public interface StudentSubjectEnrolmentRepository extends JpaRepository<StudentSubjectEnrolment, UUID> {
    List<StudentSubjectEnrolment> findByStudent_IdAndDeletedAtIsNull(UUID studentId);
}
