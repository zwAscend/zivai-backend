package zw.co.zivai.core_backend.controllers.calendar;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import zw.co.zivai.core_backend.dtos.calendar.CalendarEventDto;
import zw.co.zivai.core_backend.dtos.calendar.CalendarEventRequest;
import zw.co.zivai.core_backend.services.calendar.CalendarEventService;

@RestController
@RequestMapping("/api/calendar/events")
@RequiredArgsConstructor
public class CalendarEventController {
    private final CalendarEventService calendarEventService;

    @GetMapping
    public List<CalendarEventDto> list(@RequestParam(required = false) String start,
                                       @RequestParam(required = false) String end,
                                       @RequestParam(required = false) UUID studentId) {
        Instant startInstant = parseInstant(start);
        Instant endInstant = parseInstant(end);
        return calendarEventService.list(startInstant, endInstant, studentId);
    }

    @GetMapping("/subject/{subjectId}")
    public List<CalendarEventDto> listBySubject(@PathVariable UUID subjectId,
                                                @RequestParam(required = false) UUID studentId) {
        return calendarEventService.listBySubject(subjectId, studentId);
    }

    @GetMapping("/upcoming")
    public List<CalendarEventDto> upcoming(@RequestParam(defaultValue = "10") int limit,
                                           @RequestParam(required = false) UUID studentId) {
        return calendarEventService.upcoming(limit, studentId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CalendarEventDto create(@RequestBody CalendarEventRequest request) {
        return calendarEventService.create(request);
    }

    @PutMapping("/{eventId}")
    public CalendarEventDto update(@PathVariable UUID eventId, @RequestBody CalendarEventRequest request) {
        return calendarEventService.update(eventId, request);
    }

    @DeleteMapping("/{eventId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID eventId) {
        calendarEventService.delete(eventId);
    }

    @PostMapping("/bulk")
    @ResponseStatus(HttpStatus.CREATED)
    public List<CalendarEventDto> bulkCreate(@RequestBody List<CalendarEventRequest> requests) {
        return requests.stream().map(calendarEventService::create).toList();
    }

    private Instant parseInstant(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(value);
        } catch (Exception ex) {
            return java.time.LocalDateTime.parse(value)
                .atZone(java.time.ZoneId.systemDefault())
                .toInstant();
        }
    }
}
