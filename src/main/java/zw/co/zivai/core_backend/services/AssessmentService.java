package zw.co.zivai.core_backend.services;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import zw.co.zivai.core_backend.dtos.CreateAssessmentRequest;
import zw.co.zivai.core_backend.exceptions.NotFoundException;
import zw.co.zivai.core_backend.models.lms.Assessment;
import zw.co.zivai.core_backend.models.lms.Resource;
import zw.co.zivai.core_backend.models.lms.School;
import zw.co.zivai.core_backend.models.lms.Subject;
import zw.co.zivai.core_backend.models.lms.User;
import zw.co.zivai.core_backend.repositories.AssessmentRepository;
import zw.co.zivai.core_backend.repositories.ResourceRepository;
import zw.co.zivai.core_backend.repositories.SchoolRepository;
import zw.co.zivai.core_backend.repositories.SubjectRepository;
import zw.co.zivai.core_backend.repositories.UserRepository;

@Service
@RequiredArgsConstructor
public class AssessmentService {
    private final AssessmentRepository assessmentRepository;
    private final SchoolRepository schoolRepository;
    private final SubjectRepository subjectRepository;
    private final ResourceRepository resourceRepository;
    private final UserRepository userRepository;

    public Assessment create(CreateAssessmentRequest request) {
        School school = schoolRepository.findById(request.getSchoolId())
            .orElseThrow(() -> new NotFoundException("School not found: " + request.getSchoolId()));
        Subject subject = subjectRepository.findById(request.getSubjectId())
            .orElseThrow(() -> new NotFoundException("Subject not found: " + request.getSubjectId()));
        User createdBy = userRepository.findById(request.getCreatedBy())
            .orElseThrow(() -> new NotFoundException("User not found: " + request.getCreatedBy()));
        User modifiedBy = userRepository.findById(request.getLastModifiedBy())
            .orElseThrow(() -> new NotFoundException("User not found: " + request.getLastModifiedBy()));

        Resource resource = null;
        if (request.getResourceId() != null) {
            resource = resourceRepository.findById(request.getResourceId())
                .orElseThrow(() -> new NotFoundException("Resource not found: " + request.getResourceId()));
        }

        Assessment assessment = new Assessment();
        assessment.setSchool(school);
        assessment.setSubject(subject);
        assessment.setName(request.getName());
        assessment.setDescription(request.getDescription());
        assessment.setAssessmentType(request.getAssessmentType());
        assessment.setVisibility(request.getVisibility());
        assessment.setTimeLimitMin(request.getTimeLimitMin());
        assessment.setAttemptsAllowed(request.getAttemptsAllowed());
        assessment.setMaxScore(request.getMaxScore());
        assessment.setWeightPct(request.getWeightPct());
        assessment.setResource(resource);
        assessment.setAiEnhanced(request.isAiEnhanced());
        assessment.setStatus(request.getStatus());
        assessment.setCreatedBy(createdBy);
        assessment.setLastModifiedBy(modifiedBy);

        return assessmentRepository.save(assessment);
    }

    public List<Assessment> list(UUID subjectId) {
        if (subjectId == null) {
            return assessmentRepository.findAll();
        }
        return assessmentRepository.findBySubject_Id(subjectId);
    }

    public Assessment get(UUID id) {
        return assessmentRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Assessment not found: " + id));
    }
}
