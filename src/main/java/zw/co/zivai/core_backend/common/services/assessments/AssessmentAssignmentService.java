package zw.co.zivai.core_backend.common.services.assessments;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import zw.co.zivai.core_backend.common.dtos.assessments.BulkAssessmentEnrollmentRequest;
import zw.co.zivai.core_backend.common.dtos.assessments.CreateAssessmentAssignmentRequest;
import zw.co.zivai.core_backend.common.dtos.assessments.UpdateAssessmentAssignmentRequest;
import zw.co.zivai.core_backend.common.exceptions.BadRequestException;
import zw.co.zivai.core_backend.common.exceptions.NotFoundException;
import zw.co.zivai.core_backend.common.models.lms.assessments.Assessment;
import zw.co.zivai.core_backend.common.models.lms.assessments.AssessmentAssignment;
import zw.co.zivai.core_backend.common.models.lms.assessments.AssessmentEnrollment;
import zw.co.zivai.core_backend.common.models.lms.classroom.ClassEntity;
import zw.co.zivai.core_backend.common.models.lms.students.Enrolment;
import zw.co.zivai.core_backend.common.models.lms.users.User;
import zw.co.zivai.core_backend.common.repositories.assessments.AssessmentAssignmentRepository;
import zw.co.zivai.core_backend.common.repositories.assessments.AssessmentEnrollmentRepository;
import zw.co.zivai.core_backend.common.repositories.assessments.AssessmentRepository;
import zw.co.zivai.core_backend.common.repositories.classroom.ClassRepository;
import zw.co.zivai.core_backend.common.repositories.classroom.EnrolmentRepository;
import zw.co.zivai.core_backend.common.repositories.user.UserRepository;
import zw.co.zivai.core_backend.common.services.notification.NotificationService;

@Service
@Slf4j
@RequiredArgsConstructor
public class AssessmentAssignmentService {
    private final AssessmentAssignmentRepository assessmentAssignmentRepository;
    private final AssessmentRepository assessmentRepository;
    private final ClassRepository classRepository;
    private final UserRepository userRepository;
    private final AssessmentEnrollmentRepository assessmentEnrollmentRepository;
    private final EnrolmentRepository enrolmentRepository;
    private final NotificationService notificationService;

    public AssessmentAssignment create(CreateAssessmentAssignmentRequest request) {
        if (request.getAssessmentId() == null) {
            throw new BadRequestException("assessmentId is required");
        }
        if (request.getAssignedBy() == null) {
            throw new BadRequestException("assignedBy is required");
        }
        Assessment assessment = assessmentRepository.findByIdAndDeletedAtIsNull(request.getAssessmentId())
            .orElseThrow(() -> new NotFoundException("Assessment not found: " + request.getAssessmentId()));
        User assignedBy = userRepository.findByIdAndDeletedAtIsNull(request.getAssignedBy())
            .orElseThrow(() -> new NotFoundException("User not found: " + request.getAssignedBy()));

        ClassEntity classEntity = null;
        if (request.getClassId() != null) {
            classEntity = classRepository.findByIdAndDeletedAtIsNull(request.getClassId())
                .orElseThrow(() -> new NotFoundException("Class not found: " + request.getClassId()));
        }

        AssessmentAssignment assignment = new AssessmentAssignment();
        assignment.setAssessment(assessment);
        assignment.setClassEntity(classEntity);
        assignment.setAssignedBy(assignedBy);
        String title = request.getTitle();
        if (title == null || title.isBlank()) {
            title = assessment.getName();
        }
        assignment.setTitle(title);
        assignment.setInstructions(request.getInstructions());
        assignment.setStartTime(request.getStartTime());
        assignment.setDueTime(request.getDueTime());
        assignment.setPublished(request.isPublished());

        AssessmentAssignment saved = assessmentAssignmentRepository.save(assignment);
        if (saved.isPublished()) {
            notifyAssessmentPublished(saved);
        }
        return saved;
    }

    public List<AssessmentAssignment> list() {
        return assessmentAssignmentRepository.findByDeletedAtIsNull();
    }

    public List<AssessmentAssignment> listByAssessment(UUID assessmentId) {
        return assessmentAssignmentRepository.findByAssessment_IdAndDeletedAtIsNull(assessmentId);
    }

    public List<AssessmentAssignment> listByClass(UUID classId) {
        return assessmentAssignmentRepository.findByClassEntity_IdAndDeletedAtIsNull(classId);
    }

    public List<AssessmentAssignment> listByPublished(boolean published) {
        return assessmentAssignmentRepository.findByPublishedAndDeletedAtIsNull(published);
    }

    public AssessmentAssignment get(UUID id) {
        return assessmentAssignmentRepository.findByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> new NotFoundException("Assessment assignment not found: " + id));
    }

    public AssessmentAssignment update(UUID id, UpdateAssessmentAssignmentRequest request) {
        AssessmentAssignment assignment = get(id);
        Instant previousDueTime = assignment.getDueTime();
        boolean wasPublished = assignment.isPublished();

        if (request.getClassId() != null) {
            ClassEntity classEntity = classRepository.findByIdAndDeletedAtIsNull(request.getClassId())
                .orElseThrow(() -> new NotFoundException("Class not found: " + request.getClassId()));
            assignment.setClassEntity(classEntity);
        }
        if (request.getAssignedBy() != null) {
            User assignedBy = userRepository.findByIdAndDeletedAtIsNull(request.getAssignedBy())
                .orElseThrow(() -> new NotFoundException("User not found: " + request.getAssignedBy()));
            assignment.setAssignedBy(assignedBy);
        }
        if (request.getTitle() != null) {
            assignment.setTitle(request.getTitle());
        }
        if (request.getInstructions() != null) {
            assignment.setInstructions(request.getInstructions());
        }
        if (request.getStartTime() != null) {
            assignment.setStartTime(request.getStartTime());
        }
        if (request.getDueTime() != null) {
            assignment.setDueTime(request.getDueTime());
        }
        if (request.getPublished() != null) {
            assignment.setPublished(request.getPublished());
        }

        AssessmentAssignment saved = assessmentAssignmentRepository.save(assignment);

        if (!wasPublished && saved.isPublished()) {
            notifyAssessmentPublished(saved);
        }
        if (!Objects.equals(previousDueTime, saved.getDueTime())) {
            notifyAssessmentDeadlineChanged(saved, previousDueTime, saved.getDueTime());
        }

        return saved;
    }

    public AssessmentAssignment setPublished(UUID id, boolean published) {
        AssessmentAssignment assignment = get(id);
        boolean wasPublished = assignment.isPublished();
        assignment.setPublished(published);
        AssessmentAssignment saved = assessmentAssignmentRepository.save(assignment);
        if (!wasPublished && published) {
            notifyAssessmentPublished(saved);
        }
        return saved;
    }

    public List<AssessmentEnrollment> enrollStudents(UUID assignmentId, BulkAssessmentEnrollmentRequest request) {
        AssessmentAssignment assignment = get(assignmentId);
        List<UUID> studentIds = request.getStudentIds();
        if (studentIds == null || studentIds.isEmpty()) {
            throw new BadRequestException("studentIds are required");
        }
        String statusCode = request.getStatusCode() != null ? request.getStatusCode() : "assigned";

        List<UUID> uniqueStudentIds = studentIds.stream()
            .filter(Objects::nonNull)
            .distinct()
            .toList();
        if (uniqueStudentIds.isEmpty()) {
            throw new BadRequestException("studentIds are required");
        }

        Map<UUID, AssessmentEnrollment> existingByStudentId = assessmentEnrollmentRepository
            .findByAssessmentAssignment_IdAndStudent_IdIn(assignmentId, uniqueStudentIds)
            .stream()
            .filter(enrollment -> enrollment.getStudent() != null && enrollment.getStudent().getId() != null)
            .collect(Collectors.toMap(
                enrollment -> enrollment.getStudent().getId(),
                Function.identity(),
                (left, right) -> left
            ));

        List<User> students = userRepository.findByIdInAndDeletedAtIsNull(uniqueStudentIds);
        Map<UUID, User> studentById = students.stream()
            .collect(Collectors.toMap(User::getId, Function.identity()));

        List<AssessmentEnrollment> newEnrollments = new ArrayList<>();
        for (UUID studentId : uniqueStudentIds) {
            if (existingByStudentId.containsKey(studentId)) {
                continue;
            }
            User student = studentById.get(studentId);
            if (student == null) {
                throw new NotFoundException("Student not found: " + studentId);
            }
            AssessmentEnrollment enrollment = new AssessmentEnrollment();
            enrollment.setAssessmentAssignment(assignment);
            enrollment.setStudent(student);
            enrollment.setStatusCode(statusCode);
            newEnrollments.add(enrollment);
        }

        List<AssessmentEnrollment> createdEnrollments = newEnrollments.isEmpty()
            ? List.of()
            : assessmentEnrollmentRepository.saveAll(newEnrollments);
        List<AssessmentEnrollment> enrollments = new ArrayList<>(existingByStudentId.values());
        enrollments.addAll(createdEnrollments);

        notifyAssessmentAssigned(assignment, createdEnrollments);
        return enrollments;
    }

    public List<AssessmentEnrollment> enrollClass(UUID assignmentId, UUID classId, String statusCode) {
        AssessmentAssignment assignment = get(assignmentId);
        ClassEntity classEntity = classId != null
            ? classRepository.findByIdAndDeletedAtIsNull(classId)
                .orElseThrow(() -> new NotFoundException("Class not found: " + classId))
            : assignment.getClassEntity();

        if (classEntity == null) {
            throw new NotFoundException("Assignment does not have a class to enroll");
        }

        String status = statusCode != null ? statusCode : "assigned";
        List<Enrolment> enrolments = enrolmentRepository.findByClassEntity_Id(classEntity.getId());
        if (enrolments.isEmpty()) {
            return List.of();
        }

        List<UUID> studentIds = enrolments.stream()
            .map(Enrolment::getStudent)
            .filter(Objects::nonNull)
            .map(User::getId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();

        Map<UUID, AssessmentEnrollment> existingByStudentId = assessmentEnrollmentRepository
            .findByAssessmentAssignment_IdAndStudent_IdIn(assignmentId, studentIds)
            .stream()
            .filter(enrollment -> enrollment.getStudent() != null && enrollment.getStudent().getId() != null)
            .collect(Collectors.toMap(
                enrollment -> enrollment.getStudent().getId(),
                Function.identity(),
                (left, right) -> left
            ));

        List<AssessmentEnrollment> newEnrollments = enrolments.stream()
            .map(Enrolment::getStudent)
            .filter(Objects::nonNull)
            .filter(student -> student.getId() != null)
            .filter(student -> !existingByStudentId.containsKey(student.getId()))
            .map(student -> {
                AssessmentEnrollment created = new AssessmentEnrollment();
                created.setAssessmentAssignment(assignment);
                created.setStudent(student);
                created.setStatusCode(status);
                return created;
            })
            .toList();

        List<AssessmentEnrollment> createdEnrollments = newEnrollments.isEmpty()
            ? List.of()
            : assessmentEnrollmentRepository.saveAll(newEnrollments);
        List<AssessmentEnrollment> enrolled = new ArrayList<>(existingByStudentId.values());
        enrolled.addAll(createdEnrollments);

        notifyAssessmentAssigned(assignment, createdEnrollments);
        return enrolled;
    }

    private void notifyAssessmentAssigned(AssessmentAssignment assignment, List<AssessmentEnrollment> createdEnrollments) {
        if (assignment == null || createdEnrollments == null || createdEnrollments.isEmpty()) {
            return;
        }
        List<UUID> recipientIds = createdEnrollments.stream()
            .map(AssessmentEnrollment::getStudent)
            .filter(Objects::nonNull)
            .map(User::getId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
        if (recipientIds.isEmpty()) {
            return;
        }

        Map<String, Object> data = baseAssessmentNotificationData(assignment);
        data.put("event", "assessment_assigned");

        try {
            notificationService.createBulk(
                assignment.getAssessment().getSchool().getId(),
                recipientIds,
                "assessment_assigned",
                "New assessment assigned",
                "You have a new assessment: " + assignment.getAssessment().getName(),
                data,
                "high"
            );
        } catch (RuntimeException ex) {
            log.warn("Failed to create assessment assigned notifications for assignment {}", assignment.getId(), ex);
        }
    }

    private void notifyAssessmentPublished(AssessmentAssignment assignment) {
        if (assignment == null || !assignment.isPublished()) {
            return;
        }
        List<UUID> recipientIds = assessmentEnrollmentRepository.findByAssessmentAssignment_Id(assignment.getId()).stream()
            .filter(enrollment -> enrollment.getDeletedAt() == null)
            .map(AssessmentEnrollment::getStudent)
            .filter(Objects::nonNull)
            .map(User::getId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
        if (recipientIds.isEmpty()) {
            return;
        }

        Map<String, Object> data = baseAssessmentNotificationData(assignment);
        data.put("event", "assessment_published");

        try {
            notificationService.createBulk(
                assignment.getAssessment().getSchool().getId(),
                recipientIds,
                "assessment_published",
                "Assessment is now live",
                "Your assessment \"" + assignment.getAssessment().getName() + "\" has been published.",
                data,
                "high"
            );
        } catch (RuntimeException ex) {
            log.warn("Failed to create assessment published notifications for assignment {}", assignment.getId(), ex);
        }
    }

    private void notifyAssessmentDeadlineChanged(AssessmentAssignment assignment, Instant previousDueTime, Instant newDueTime) {
        if (assignment == null || Objects.equals(previousDueTime, newDueTime)) {
            return;
        }
        List<UUID> recipientIds = assessmentEnrollmentRepository.findByAssessmentAssignment_Id(assignment.getId()).stream()
            .filter(enrollment -> enrollment.getDeletedAt() == null)
            .map(AssessmentEnrollment::getStudent)
            .filter(Objects::nonNull)
            .map(User::getId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
        if (recipientIds.isEmpty()) {
            return;
        }

        Map<String, Object> data = baseAssessmentNotificationData(assignment);
        data.put("event", "assessment_deadline_changed");
        data.put("previousDueTime", previousDueTime == null ? null : previousDueTime.toString());
        data.put("newDueTime", newDueTime == null ? null : newDueTime.toString());

        try {
            notificationService.createBulk(
                assignment.getAssessment().getSchool().getId(),
                recipientIds,
                "assessment_deadline_changed",
                "Assessment deadline updated",
                "The deadline for \"" + assignment.getAssessment().getName() + "\" has changed.",
                data,
                "high"
            );
        } catch (RuntimeException ex) {
            log.warn("Failed to create assessment deadline notifications for assignment {}", assignment.getId(), ex);
        }
    }

    private Map<String, Object> baseAssessmentNotificationData(AssessmentAssignment assignment) {
        Map<String, Object> data = new HashMap<>();
        data.put("assignmentId", assignment.getId().toString());
        data.put("assessmentId", assignment.getAssessment().getId().toString());
        data.put("assessmentName", assignment.getAssessment().getName());
        data.put("subjectId", assignment.getAssessment().getSubject().getId().toString());
        data.put("subjectName", assignment.getAssessment().getSubject().getName());
        data.put("dueTime", assignment.getDueTime() == null ? null : assignment.getDueTime().toString());
        return data;
    }
}
