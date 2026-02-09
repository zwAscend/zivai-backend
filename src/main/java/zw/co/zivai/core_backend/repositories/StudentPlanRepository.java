package zw.co.zivai.core_backend.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import zw.co.zivai.core_backend.models.lms.StudentPlan;

public interface StudentPlanRepository extends JpaRepository<StudentPlan, UUID> {
    List<StudentPlan> findByStudent_Id(UUID studentId);
    List<StudentPlan> findByStudent_IdAndSubject_Id(UUID studentId, UUID subjectId);
    List<StudentPlan> findByStudent_IdAndSubject_IdOrderByCreatedAtDesc(UUID studentId, UUID subjectId);
    Optional<StudentPlan> findFirstByStudent_IdAndSubject_IdAndCurrentTrue(UUID studentId, UUID subjectId);
    Optional<StudentPlan> findByIdAndStudent_Id(UUID id, UUID studentId);
    Optional<StudentPlan> findByStudent_IdAndPlan_Id(UUID studentId, UUID planId);

    Page<StudentPlan> findByDeletedAtIsNull(Pageable pageable);
    Page<StudentPlan> findByStatusIgnoreCaseAndDeletedAtIsNull(String status, Pageable pageable);
    Page<StudentPlan> findBySubject_IdAndDeletedAtIsNull(UUID subjectId, Pageable pageable);
    Page<StudentPlan> findBySubject_IdAndStatusIgnoreCaseAndDeletedAtIsNull(UUID subjectId, String status, Pageable pageable);
    Page<StudentPlan> findByStudent_IdInAndDeletedAtIsNull(List<UUID> studentIds, Pageable pageable);
    Page<StudentPlan> findByStudent_IdInAndStatusIgnoreCaseAndDeletedAtIsNull(List<UUID> studentIds, String status, Pageable pageable);
    Page<StudentPlan> findByStudent_IdInAndSubject_IdAndDeletedAtIsNull(List<UUID> studentIds, UUID subjectId, Pageable pageable);
    Page<StudentPlan> findByStudent_IdInAndSubject_IdAndStatusIgnoreCaseAndDeletedAtIsNull(List<UUID> studentIds, UUID subjectId, String status, Pageable pageable);
}
