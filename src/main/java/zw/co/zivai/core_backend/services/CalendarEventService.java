package zw.co.zivai.core_backend.services;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import zw.co.zivai.core_backend.dtos.CalendarEventDto;
import zw.co.zivai.core_backend.dtos.CalendarEventRequest;
import zw.co.zivai.core_backend.exceptions.NotFoundException;
import zw.co.zivai.core_backend.models.lms.CalendarEvent;
import zw.co.zivai.core_backend.models.lms.ClassEntity;
import zw.co.zivai.core_backend.models.lms.School;
import zw.co.zivai.core_backend.models.lms.Subject;
import zw.co.zivai.core_backend.models.lms.User;
import zw.co.zivai.core_backend.repositories.CalendarEventRepository;
import zw.co.zivai.core_backend.repositories.ClassRepository;
import zw.co.zivai.core_backend.repositories.SchoolRepository;
import zw.co.zivai.core_backend.repositories.SubjectRepository;
import zw.co.zivai.core_backend.repositories.UserRepository;

@Service
@RequiredArgsConstructor
public class CalendarEventService {
    private final CalendarEventRepository calendarEventRepository;
    private final SchoolRepository schoolRepository;
    private final SubjectRepository subjectRepository;
    private final ClassRepository classRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public List<CalendarEventDto> list(Instant start, Instant end) {
        List<CalendarEvent> events;
        if (start != null && end != null) {
            events = calendarEventRepository.findByStartTimeBetween(start, end);
        } else {
            events = calendarEventRepository.findAll();
        }
        return events.stream().map(this::toDto).toList();
    }

    public List<CalendarEventDto> listBySubject(UUID subjectId) {
        return calendarEventRepository.findBySubject_Id(subjectId).stream()
            .map(this::toDto)
            .toList();
    }

    public List<CalendarEventDto> upcoming(int limit) {
        List<CalendarEvent> events = calendarEventRepository.findByStartTimeAfterOrderByStartTimeAsc(Instant.now());
        if (limit > 0 && events.size() > limit) {
            events = events.subList(0, limit);
        }
        return events.stream().map(this::toDto).toList();
    }

    public CalendarEventDto create(CalendarEventRequest request) {
        CalendarEvent event = new CalendarEvent();
        applyRequest(event, request);
        return toDto(calendarEventRepository.save(event));
    }

    public CalendarEventDto update(UUID eventId, CalendarEventRequest request) {
        CalendarEvent event = calendarEventRepository.findById(eventId)
            .orElseThrow(() -> new NotFoundException("Calendar event not found: " + eventId));
        applyRequest(event, request);
        return toDto(calendarEventRepository.save(event));
    }

    public void delete(UUID eventId) {
        CalendarEvent event = calendarEventRepository.findById(eventId)
            .orElseThrow(() -> new NotFoundException("Calendar event not found: " + eventId));
        calendarEventRepository.delete(event);
    }

    private void applyRequest(CalendarEvent event, CalendarEventRequest request) {
        if (event.getSchool() == null) {
            event.setSchool(resolveSchool());
        }
        if (event.getCreatedBy() == null) {
            event.setCreatedBy(resolveUser());
        }

        event.setTitle(request.getTitle());
        event.setDescription(request.getDescription());
        event.setStartTime(parseInstant(request.getStart()));
        event.setEndTime(parseOptionalInstant(request.getEnd()));
        event.setAllDay(request.isAllDay());
        event.setEventType("lecture".equalsIgnoreCase(request.getType()) ? "lesson" : request.getType());
        event.setLocation(request.getLocation());

        if (request.getSubjectId() != null && !request.getSubjectId().isBlank()) {
            Subject subject = subjectRepository.findById(UUID.fromString(request.getSubjectId()))
                .orElseThrow(() -> new NotFoundException("Subject not found: " + request.getSubjectId()));
            event.setSubject(subject);
        } else {
            event.setSubject(null);
        }

        if (request.getClassId() != null && !request.getClassId().isBlank()) {
            ClassEntity classEntity = classRepository.findById(UUID.fromString(request.getClassId()))
                .orElseThrow(() -> new NotFoundException("Class not found: " + request.getClassId()));
            event.setClassEntity(classEntity);
        } else {
            event.setClassEntity(null);
        }

        event.setRecurring(toJsonNode(request.getRecurring()));
        event.setReminders(toJsonNode(request.getReminders()));
    }

    private CalendarEventDto toDto(CalendarEvent event) {
        String type = "lecture".equalsIgnoreCase(event.getEventType()) ? "lesson" : event.getEventType();
        return CalendarEventDto.builder()
            .id(event.getId().toString())
            .title(event.getTitle())
            .description(event.getDescription())
            .start(event.getStartTime())
            .end(event.getEndTime())
            .allDay(event.isAllDay())
            .type(type)
            .subjectId(event.getSubject() != null ? event.getSubject().getId().toString() : null)
            .subjectName(event.getSubject() != null ? event.getSubject().getName() : null)
            .location(event.getLocation())
            .recurring(event.getRecurring())
            .reminders(event.getReminders())
            .createdBy(event.getCreatedBy() != null ? event.getCreatedBy().getId().toString() : null)
            .createdAt(event.getCreatedAt())
            .updatedAt(event.getUpdatedAt())
            .build();
    }

    private School resolveSchool() {
        return schoolRepository.findByCode("ZVHS")
            .orElseGet(() -> schoolRepository.findAll().stream()
                .findFirst()
                .orElseThrow(() -> new NotFoundException("No school found")));
    }

    private User resolveUser() {
        return userRepository.findByEmail("teacher@zivai.local")
            .orElseGet(() -> userRepository.findAll().stream()
                .findFirst()
                .orElseThrow(() -> new NotFoundException("No user found")));
    }

    private Instant parseInstant(String value) {
        if (value == null || value.isBlank()) {
            return Instant.now();
        }
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException ex) {
            LocalDateTime local = LocalDateTime.parse(value);
            return local.atZone(ZoneId.systemDefault()).toInstant();
        }
    }

    private Instant parseOptionalInstant(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return parseInstant(value);
    }

    private JsonNode toJsonNode(Object value) {
        if (value == null) {
            return null;
        }
        return objectMapper.valueToTree(value);
    }
}
