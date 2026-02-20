package zw.co.zivai.core_backend.services.calendar;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import zw.co.zivai.core_backend.dtos.calendar.CalendarEventDto;
import zw.co.zivai.core_backend.dtos.calendar.CalendarEventRequest;
import zw.co.zivai.core_backend.exceptions.NotFoundException;
import zw.co.zivai.core_backend.models.lms.CalendarEvent;
import zw.co.zivai.core_backend.models.lms.ClassSubject;
import zw.co.zivai.core_backend.models.lms.ClassEntity;
import zw.co.zivai.core_backend.models.lms.Enrolment;
import zw.co.zivai.core_backend.models.lms.School;
import zw.co.zivai.core_backend.models.lms.StudentSubjectEnrolment;
import zw.co.zivai.core_backend.models.lms.Subject;
import zw.co.zivai.core_backend.models.lms.User;
import zw.co.zivai.core_backend.repositories.calendar.CalendarEventRepository;
import zw.co.zivai.core_backend.repositories.classroom.ClassRepository;
import zw.co.zivai.core_backend.repositories.classroom.ClassSubjectRepository;
import zw.co.zivai.core_backend.repositories.classroom.EnrolmentRepository;
import zw.co.zivai.core_backend.repositories.classroom.StudentSubjectEnrolmentRepository;
import zw.co.zivai.core_backend.repositories.school.SchoolRepository;
import zw.co.zivai.core_backend.repositories.subject.SubjectRepository;
import zw.co.zivai.core_backend.repositories.user.UserRepository;

@Service
@RequiredArgsConstructor
public class CalendarEventService {
    private final CalendarEventRepository calendarEventRepository;
    private final SchoolRepository schoolRepository;
    private final SubjectRepository subjectRepository;
    private final ClassRepository classRepository;
    private final ClassSubjectRepository classSubjectRepository;
    private final EnrolmentRepository enrolmentRepository;
    private final StudentSubjectEnrolmentRepository studentSubjectEnrolmentRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public List<CalendarEventDto> list(Instant start, Instant end) {
        return list(start, end, null);
    }

    public List<CalendarEventDto> list(Instant start, Instant end, UUID studentId) {
        List<CalendarEvent> events;
        if (start != null && end != null) {
            events = calendarEventRepository.findByDeletedAtIsNullAndStartTimeBetweenOrderByStartTimeAsc(start, end);
        } else {
            events = calendarEventRepository.findByDeletedAtIsNullOrderByStartTimeAsc();
        }
        events = filterByStudentScope(events, studentId);
        return events.stream().map(this::toDto).toList();
    }

    public List<CalendarEventDto> listBySubject(UUID subjectId) {
        return listBySubject(subjectId, null);
    }

    public List<CalendarEventDto> listBySubject(UUID subjectId, UUID studentId) {
        List<CalendarEvent> events = calendarEventRepository.findByDeletedAtIsNullAndSubject_IdOrderByStartTimeAsc(subjectId).stream()
            .toList();
        events = filterByStudentScope(events, studentId);
        return events.stream()
            .map(this::toDto)
            .toList();
    }

    public List<CalendarEventDto> upcoming(int limit) {
        return upcoming(limit, null);
    }

    public List<CalendarEventDto> upcoming(int limit, UUID studentId) {
        List<CalendarEvent> events = calendarEventRepository.findByDeletedAtIsNullAndStartTimeAfterOrderByStartTimeAsc(Instant.now());
        events = filterByStudentScope(events, studentId);
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
        event.setTitle(request.getTitle());
        event.setDescription(request.getDescription());
        event.setStartTime(parseInstant(request.getStart()));
        event.setEndTime(parseOptionalInstant(request.getEnd()));
        event.setAllDay(request.isAllDay());
        event.setEventType("lecture".equalsIgnoreCase(request.getType()) ? "lesson" : request.getType());
        event.setLocation(request.getLocation());

        Subject subject = null;
        if (request.getSubjectId() != null && !request.getSubjectId().isBlank()) {
            subject = subjectRepository.findById(UUID.fromString(request.getSubjectId()))
                .orElseThrow(() -> new NotFoundException("Subject not found: " + request.getSubjectId()));
            event.setSubject(subject);
        } else {
            event.setSubject(null);
        }

        ClassEntity classEntity = null;
        if (request.getClassId() != null && !request.getClassId().isBlank()) {
            classEntity = classRepository.findById(UUID.fromString(request.getClassId()))
                .orElseThrow(() -> new NotFoundException("Class not found: " + request.getClassId()));
            event.setClassEntity(classEntity);
        } else {
            event.setClassEntity(null);
        }

        if (event.getSchool() == null) {
            event.setSchool(resolveSchool(request, classEntity));
        }
        if (event.getCreatedBy() == null) {
            event.setCreatedBy(resolveCreatedBy(request, classEntity, subject));
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

    private School resolveSchool(CalendarEventRequest request, ClassEntity classEntity) {
        if (request.getSchoolId() != null && !request.getSchoolId().isBlank()) {
            return schoolRepository.findById(UUID.fromString(request.getSchoolId()))
                .orElseThrow(() -> new NotFoundException("School not found: " + request.getSchoolId()));
        }
        if (classEntity != null && classEntity.getSchool() != null) {
            return classEntity.getSchool();
        }
        return schoolRepository.findAll().stream()
            .findFirst()
            .orElseThrow(() -> new NotFoundException("No school found"));
    }

    private User resolveCreatedBy(CalendarEventRequest request, ClassEntity classEntity, Subject subject) {
        if (request.getCreatedBy() != null && !request.getCreatedBy().isBlank()) {
            return userRepository.findByIdAndDeletedAtIsNull(UUID.fromString(request.getCreatedBy()))
                .orElseThrow(() -> new NotFoundException("User not found: " + request.getCreatedBy()));
        }

        if (classEntity != null && subject != null) {
            List<ClassSubject> links = classSubjectRepository.findByClassEntity_IdAndSubject_IdAndDeletedAtIsNull(
                classEntity.getId(),
                subject.getId()
            );
            for (ClassSubject link : links) {
                if (link.getTeacher() != null) {
                    return link.getTeacher();
                }
            }
        }

        if (classEntity != null && classEntity.getHomeroomTeacher() != null) {
            return classEntity.getHomeroomTeacher();
        }

        List<User> teachers = userRepository.findByRoles_CodeAndDeletedAtIsNull("teacher");
        if (!teachers.isEmpty()) {
            return teachers.get(0);
        }

        return userRepository.findAllByDeletedAtIsNull().stream()
            .findFirst()
            .orElseThrow(() -> new NotFoundException("No user found"));
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

    private List<CalendarEvent> filterByStudentScope(List<CalendarEvent> events, UUID studentId) {
        if (studentId == null) {
            return events;
        }

        Set<UUID> studentClassIds = enrolmentRepository.findByStudent_Id(studentId).stream()
            .filter(enrolment -> enrolment.getDeletedAt() == null)
            .map(Enrolment::getClassEntity)
            .filter(classEntity -> classEntity != null && classEntity.getDeletedAt() == null)
            .map(ClassEntity::getId)
            .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));

        Set<UUID> studentSubjectIds = studentSubjectEnrolmentRepository.findByStudent_IdAndDeletedAtIsNull(studentId).stream()
            .map(StudentSubjectEnrolment::getClassSubject)
            .filter(classSubject -> classSubject != null && classSubject.getSubject() != null)
            .map(classSubject -> classSubject.getSubject().getId())
            .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));

        return events.stream()
            .filter(event -> isVisibleToStudent(event, studentClassIds, studentSubjectIds))
            .toList();
    }

    private boolean isVisibleToStudent(CalendarEvent event, Set<UUID> studentClassIds, Set<UUID> studentSubjectIds) {
        if (event.isPublic()) {
            return true;
        }
        UUID eventClassId = event.getClassEntity() != null ? event.getClassEntity().getId() : null;
        if (eventClassId != null && studentClassIds.contains(eventClassId)) {
            return true;
        }
        UUID eventSubjectId = event.getSubject() != null ? event.getSubject().getId() : null;
        return eventSubjectId != null && studentSubjectIds.contains(eventSubjectId);
    }
}
