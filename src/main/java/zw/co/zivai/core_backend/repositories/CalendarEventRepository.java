package zw.co.zivai.core_backend.repositories;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import zw.co.zivai.core_backend.models.lms.CalendarEvent;

public interface CalendarEventRepository extends JpaRepository<CalendarEvent, UUID> {
    List<CalendarEvent> findByStartTimeBetween(Instant start, Instant end);
    List<CalendarEvent> findBySubject_Id(UUID subjectId);
    List<CalendarEvent> findByStartTimeAfterOrderByStartTimeAsc(Instant start);
}
