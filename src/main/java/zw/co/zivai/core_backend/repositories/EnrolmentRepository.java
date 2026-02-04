package zw.co.zivai.core_backend.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import zw.co.zivai.core_backend.models.lms.Enrolment;

public interface EnrolmentRepository extends JpaRepository<Enrolment, UUID> {
    Optional<Enrolment> findByClassEntity_IdAndStudent_Id(UUID classId, UUID studentId);
    List<Enrolment> findByStudent_Id(UUID studentId);
}
