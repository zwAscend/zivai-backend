package zw.co.zivai.core_backend.repositories.calendar;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import zw.co.zivai.core_backend.models.lms.CalendarEvent;

public interface CalendarEventRepository extends JpaRepository<CalendarEvent, UUID> {
    @EntityGraph(attributePaths = {"subject", "classEntity", "createdBy"})
    List<CalendarEvent> findByDeletedAtIsNullOrderByStartTimeAsc();
    @EntityGraph(attributePaths = {"subject", "classEntity", "createdBy"})
    List<CalendarEvent> findByDeletedAtIsNullAndStartTimeBetweenOrderByStartTimeAsc(Instant start, Instant end);
    @EntityGraph(attributePaths = {"subject", "classEntity", "createdBy"})
    List<CalendarEvent> findByDeletedAtIsNullAndSubject_IdOrderByStartTimeAsc(UUID subjectId);
    @EntityGraph(attributePaths = {"subject", "classEntity", "createdBy"})
    List<CalendarEvent> findByDeletedAtIsNullAndStartTimeAfterOrderByStartTimeAsc(Instant start);
}
