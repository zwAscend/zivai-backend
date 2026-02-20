package zw.co.zivai.core_backend.repositories.development;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import zw.co.zivai.core_backend.models.lms.StudentActivityDay;

public interface StudentActivityDayRepository extends JpaRepository<StudentActivityDay, UUID> {
    Optional<StudentActivityDay> findByStudent_IdAndActivityDateAndDeletedAtIsNull(UUID studentId, LocalDate activityDate);
    List<StudentActivityDay> findTop365ByStudent_IdAndDeletedAtIsNullOrderByActivityDateDesc(UUID studentId);
}

