package zw.co.zivai.core_backend.services.assessments;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import zw.co.zivai.core_backend.dtos.assessments.AssessmentEnrollmentSummaryDto;
import zw.co.zivai.core_backend.dtos.assessments.CreateAssessmentEnrollmentRequest;
import zw.co.zivai.core_backend.exceptions.BadRequestException;
import zw.co.zivai.core_backend.exceptions.NotFoundException;
import zw.co.zivai.core_backend.models.lms.AssessmentEnrollment;
import zw.co.zivai.core_backend.models.lms.AssessmentAssignment;
import zw.co.zivai.core_backend.models.lms.User;
import zw.co.zivai.core_backend.repositories.assessments.AssessmentEnrollmentRepository;
import zw.co.zivai.core_backend.repositories.assessments.AssessmentAssignmentRepository;
import zw.co.zivai.core_backend.repositories.user.UserRepository;

@Service
@RequiredArgsConstructor
public class AssessmentEnrollmentService {
    private final AssessmentEnrollmentRepository assessmentEnrollmentRepository;
    private final AssessmentAssignmentRepository assessmentAssignmentRepository;
    private final UserRepository userRepository;

    public AssessmentEnrollment create(CreateAssessmentEnrollmentRequest request) {
        if (request.getAssessmentAssignmentId() == null) {
            throw new BadRequestException("assessmentAssignmentId is required");
        }
        if (request.getStudentId() == null) {
            throw new BadRequestException("studentId is required");
        }
        AssessmentAssignment assignment = assessmentAssignmentRepository.findById(request.getAssessmentAssignmentId())
            .orElseThrow(() -> new NotFoundException("Assessment assignment not found: " + request.getAssessmentAssignmentId()));
        User student = userRepository.findById(request.getStudentId())
            .orElseThrow(() -> new NotFoundException("Student not found: " + request.getStudentId()));

        AssessmentEnrollment enrollment = new AssessmentEnrollment();
        enrollment.setAssessmentAssignment(assignment);
        enrollment.setStudent(student);
        if (request.getStatusCode() == null || request.getStatusCode().isBlank()) {
            enrollment.setStatusCode("assigned");
        } else {
            enrollment.setStatusCode(request.getStatusCode());
        }

        return assessmentEnrollmentRepository.save(enrollment);
    }

    public List<AssessmentEnrollment> list() {
        return assessmentEnrollmentRepository.findAll();
    }

    public List<AssessmentEnrollmentSummaryDto> listSummary(UUID assignmentId, UUID studentId, UUID classId) {
        List<AssessmentEnrollment> enrollments;
        if (assignmentId != null && studentId != null) {
            enrollments = assessmentEnrollmentRepository
                .findByAssessmentAssignment_IdAndStudent_Id(assignmentId, studentId)
                .map(List::of)
                .orElseGet(List::of);
        } else if (assignmentId != null) {
            enrollments = assessmentEnrollmentRepository.findByAssessmentAssignment_Id(assignmentId);
        } else if (studentId != null) {
            enrollments = assessmentEnrollmentRepository.findByStudent_Id(studentId);
        } else if (classId != null) {
            enrollments = assessmentEnrollmentRepository.findByAssessmentAssignment_ClassEntity_Id(classId);
        } else {
            enrollments = assessmentEnrollmentRepository.findAll();
        }

        return enrollments.stream()
            .map(this::toSummaryDto)
            .toList();
    }

    public AssessmentEnrollment get(UUID id) {
        return assessmentEnrollmentRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Assessment enrollment not found: " + id));
    }

    private AssessmentEnrollmentSummaryDto toSummaryDto(AssessmentEnrollment enrollment) {
        AssessmentAssignment assignment = enrollment.getAssessmentAssignment();
        var assessment = assignment.getAssessment();
        var classEntity = assignment.getClassEntity();
        var student = enrollment.getStudent();

        return AssessmentEnrollmentSummaryDto.builder()
            .id(enrollment.getId().toString())
            .statusCode(enrollment.getStatusCode())
            .studentId(student.getId().toString())
            .studentFirstName(student.getFirstName())
            .studentLastName(student.getLastName())
            .studentEmail(student.getEmail())
            .assignmentId(assignment.getId().toString())
            .assessmentId(assessment.getId().toString())
            .assessmentName(assessment.getName())
            .classId(classEntity != null ? classEntity.getId().toString() : null)
            .className(classEntity != null ? classEntity.getName() : null)
            .dueTime(assignment.getDueTime())
            .published(assignment.isPublished())
            .createdAt(enrollment.getCreatedAt())
            .updatedAt(enrollment.getUpdatedAt())
            .build();
    }
}
