package zw.co.zivai.core_backend.services;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import zw.co.zivai.core_backend.dtos.BulkAssessmentEnrollmentRequest;
import zw.co.zivai.core_backend.dtos.CreateAssessmentAssignmentRequest;
import zw.co.zivai.core_backend.dtos.UpdateAssessmentAssignmentRequest;
import zw.co.zivai.core_backend.exceptions.BadRequestException;
import zw.co.zivai.core_backend.exceptions.NotFoundException;
import zw.co.zivai.core_backend.models.lms.Assessment;
import zw.co.zivai.core_backend.models.lms.AssessmentAssignment;
import zw.co.zivai.core_backend.models.lms.AssessmentEnrollment;
import zw.co.zivai.core_backend.models.lms.ClassEntity;
import zw.co.zivai.core_backend.models.lms.Enrolment;
import zw.co.zivai.core_backend.models.lms.User;
import zw.co.zivai.core_backend.repositories.AssessmentAssignmentRepository;
import zw.co.zivai.core_backend.repositories.AssessmentEnrollmentRepository;
import zw.co.zivai.core_backend.repositories.AssessmentRepository;
import zw.co.zivai.core_backend.repositories.ClassRepository;
import zw.co.zivai.core_backend.repositories.EnrolmentRepository;
import zw.co.zivai.core_backend.repositories.UserRepository;

@Service
@RequiredArgsConstructor
public class AssessmentAssignmentService {
    private final AssessmentAssignmentRepository assessmentAssignmentRepository;
    private final AssessmentRepository assessmentRepository;
    private final ClassRepository classRepository;
    private final UserRepository userRepository;
    private final AssessmentEnrollmentRepository assessmentEnrollmentRepository;
    private final EnrolmentRepository enrolmentRepository;

    public AssessmentAssignment create(CreateAssessmentAssignmentRequest request) {
        if (request.getAssessmentId() == null) {
            throw new BadRequestException("assessmentId is required");
        }
        if (request.getAssignedBy() == null) {
            throw new BadRequestException("assignedBy is required");
        }
        Assessment assessment = assessmentRepository.findById(request.getAssessmentId())
            .orElseThrow(() -> new NotFoundException("Assessment not found: " + request.getAssessmentId()));
        User assignedBy = userRepository.findById(request.getAssignedBy())
            .orElseThrow(() -> new NotFoundException("User not found: " + request.getAssignedBy()));

        ClassEntity classEntity = null;
        if (request.getClassId() != null) {
            classEntity = classRepository.findById(request.getClassId())
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

        return assessmentAssignmentRepository.save(assignment);
    }

    public List<AssessmentAssignment> list() {
        return assessmentAssignmentRepository.findAll();
    }

    public List<AssessmentAssignment> listByAssessment(UUID assessmentId) {
        return assessmentAssignmentRepository.findByAssessment_Id(assessmentId);
    }

    public List<AssessmentAssignment> listByClass(UUID classId) {
        return assessmentAssignmentRepository.findByClassEntity_Id(classId);
    }

    public List<AssessmentAssignment> listByPublished(boolean published) {
        return assessmentAssignmentRepository.findByPublished(published);
    }

    public AssessmentAssignment get(UUID id) {
        return assessmentAssignmentRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Assessment assignment not found: " + id));
    }

    public AssessmentAssignment update(UUID id, UpdateAssessmentAssignmentRequest request) {
        AssessmentAssignment assignment = get(id);

        if (request.getClassId() != null) {
            ClassEntity classEntity = classRepository.findById(request.getClassId())
                .orElseThrow(() -> new NotFoundException("Class not found: " + request.getClassId()));
            assignment.setClassEntity(classEntity);
        }
        if (request.getAssignedBy() != null) {
            User assignedBy = userRepository.findById(request.getAssignedBy())
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

        return assessmentAssignmentRepository.save(assignment);
    }

    public AssessmentAssignment setPublished(UUID id, boolean published) {
        AssessmentAssignment assignment = get(id);
        assignment.setPublished(published);
        return assessmentAssignmentRepository.save(assignment);
    }

    public List<AssessmentEnrollment> enrollStudents(UUID assignmentId, BulkAssessmentEnrollmentRequest request) {
        AssessmentAssignment assignment = get(assignmentId);
        List<UUID> studentIds = request.getStudentIds();
        if (studentIds == null || studentIds.isEmpty()) {
            throw new BadRequestException("studentIds are required");
        }
        String statusCode = request.getStatusCode() != null ? request.getStatusCode() : "assigned";

        return studentIds.stream()
            .map(studentId -> assessmentEnrollmentRepository
                .findByAssessmentAssignment_IdAndStudent_Id(assignmentId, studentId)
                .orElseGet(() -> {
                    User student = userRepository.findById(studentId)
                        .orElseThrow(() -> new NotFoundException("Student not found: " + studentId));
                    AssessmentEnrollment enrollment = new AssessmentEnrollment();
                    enrollment.setAssessmentAssignment(assignment);
                    enrollment.setStudent(student);
                    enrollment.setStatusCode(statusCode);
                    return assessmentEnrollmentRepository.save(enrollment);
                }))
            .toList();
    }

    public List<AssessmentEnrollment> enrollClass(UUID assignmentId, UUID classId, String statusCode) {
        AssessmentAssignment assignment = get(assignmentId);
        ClassEntity classEntity = classId != null
            ? classRepository.findById(classId)
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

        return enrolments.stream()
            .map(enrolment -> assessmentEnrollmentRepository
                .findByAssessmentAssignment_IdAndStudent_Id(assignmentId, enrolment.getStudent().getId())
                .orElseGet(() -> {
                    AssessmentEnrollment enrollment = new AssessmentEnrollment();
                    enrollment.setAssessmentAssignment(assignment);
                    enrollment.setStudent(enrolment.getStudent());
                    enrollment.setStatusCode(status);
                    return assessmentEnrollmentRepository.save(enrollment);
                }))
            .toList();
    }
}
