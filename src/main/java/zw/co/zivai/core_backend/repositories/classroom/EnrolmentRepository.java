package zw.co.zivai.core_backend.repositories.classroom;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import zw.co.zivai.core_backend.models.lms.Enrolment;

public interface EnrolmentRepository extends JpaRepository<Enrolment, UUID> {
    @EntityGraph(attributePaths = {"student", "classEntity"})
    Optional<Enrolment> findByClassEntity_IdAndStudent_Id(UUID classId, UUID studentId);

    @EntityGraph(attributePaths = {"student", "classEntity"})
    List<Enrolment> findByStudent_Id(UUID studentId);

    @EntityGraph(attributePaths = {"student", "classEntity"})
    List<Enrolment> findByStudent_IdIn(List<UUID> studentIds);

    @EntityGraph(attributePaths = {"student", "classEntity"})
    List<Enrolment> findByClassEntity_Id(UUID classId);

    @EntityGraph(attributePaths = {"student", "classEntity"})
    List<Enrolment> findByClassEntity_IdIn(List<UUID> classIds);
}
