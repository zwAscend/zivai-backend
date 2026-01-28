package zw.co.zivai.core_backend.services;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import zw.co.zivai.core_backend.dtos.CreateAssessmentAssignmentRequest;
import zw.co.zivai.core_backend.exceptions.NotFoundException;
import zw.co.zivai.core_backend.models.lms.Assessment;
import zw.co.zivai.core_backend.models.lms.AssessmentAssignment;
import zw.co.zivai.core_backend.models.lms.ClassEntity;
import zw.co.zivai.core_backend.models.lms.User;
import zw.co.zivai.core_backend.repositories.AssessmentAssignmentRepository;
import zw.co.zivai.core_backend.repositories.AssessmentRepository;
import zw.co.zivai.core_backend.repositories.ClassRepository;
import zw.co.zivai.core_backend.repositories.UserRepository;

@Service
@RequiredArgsConstructor
public class AssessmentAssignmentService {
    private final AssessmentAssignmentRepository assessmentAssignmentRepository;
    private final AssessmentRepository assessmentRepository;
    private final ClassRepository classRepository;
    private final UserRepository userRepository;

    public AssessmentAssignment create(CreateAssessmentAssignmentRequest request) {
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
        assignment.setTitle(request.getTitle());
        assignment.setInstructions(request.getInstructions());
        assignment.setStartTime(request.getStartTime());
        assignment.setDueTime(request.getDueTime());
        assignment.setPublished(request.isPublished());

        return assessmentAssignmentRepository.save(assignment);
    }

    public List<AssessmentAssignment> list() {
        return assessmentAssignmentRepository.findAll();
    }

    public AssessmentAssignment get(UUID id) {
        return assessmentAssignmentRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Assessment assignment not found: " + id));
    }
}
