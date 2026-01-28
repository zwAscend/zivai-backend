package zw.co.zivai.core_backend.services;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import zw.co.zivai.core_backend.dtos.CreateAssessmentEnrollmentRequest;
import zw.co.zivai.core_backend.exceptions.NotFoundException;
import zw.co.zivai.core_backend.models.lms.AssessmentEnrollment;
import zw.co.zivai.core_backend.models.lms.AssessmentAssignment;
import zw.co.zivai.core_backend.models.lms.User;
import zw.co.zivai.core_backend.repositories.AssessmentEnrollmentRepository;
import zw.co.zivai.core_backend.repositories.AssessmentAssignmentRepository;
import zw.co.zivai.core_backend.repositories.UserRepository;

@Service
@RequiredArgsConstructor
public class AssessmentEnrollmentService {
    private final AssessmentEnrollmentRepository assessmentEnrollmentRepository;
    private final AssessmentAssignmentRepository assessmentAssignmentRepository;
    private final UserRepository userRepository;

    public AssessmentEnrollment create(CreateAssessmentEnrollmentRequest request) {
        AssessmentAssignment assignment = assessmentAssignmentRepository.findById(request.getAssessmentAssignmentId())
            .orElseThrow(() -> new NotFoundException("Assessment assignment not found: " + request.getAssessmentAssignmentId()));
        User student = userRepository.findById(request.getStudentId())
            .orElseThrow(() -> new NotFoundException("Student not found: " + request.getStudentId()));

        AssessmentEnrollment enrollment = new AssessmentEnrollment();
        enrollment.setAssessmentAssignment(assignment);
        enrollment.setStudent(student);
        enrollment.setStatusCode(request.getStatusCode());

        return assessmentEnrollmentRepository.save(enrollment);
    }

    public List<AssessmentEnrollment> list() {
        return assessmentEnrollmentRepository.findAll();
    }

    public AssessmentEnrollment get(UUID id) {
        return assessmentEnrollmentRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Assessment enrollment not found: " + id));
    }
}
