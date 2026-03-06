package zw.co.zivai.core_backend.common.services.assessments;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import zw.co.zivai.core_backend.common.dtos.assessments.CreateAssessmentAttemptRequest;
import zw.co.zivai.core_backend.common.exceptions.NotFoundException;
import zw.co.zivai.core_backend.common.models.lms.assessments.AssessmentAttempt;
import zw.co.zivai.core_backend.common.models.lms.assessments.AssessmentEnrollment;
import zw.co.zivai.core_backend.common.repositories.assessments.AssessmentAttemptRepository;
import zw.co.zivai.core_backend.common.repositories.assessments.AssessmentEnrollmentRepository;

@Service
@RequiredArgsConstructor
public class AssessmentAttemptService {
    private final AssessmentAttemptRepository assessmentAttemptRepository;
    private final AssessmentEnrollmentRepository assessmentEnrollmentRepository;

    public AssessmentAttempt create(CreateAssessmentAttemptRequest request) {
        AssessmentEnrollment enrollment = assessmentEnrollmentRepository.findById(request.getAssessmentEnrollmentId())
            .orElseThrow(() -> new NotFoundException("Assessment enrollment not found: " + request.getAssessmentEnrollmentId()));

        AssessmentAttempt attempt = new AssessmentAttempt();
        attempt.setAssessmentEnrollment(enrollment);
        attempt.setAttemptNumber(request.getAttemptNumber());
        attempt.setGradingStatusCode(request.getGradingStatusCode());
        attempt.setMaxScore(request.getMaxScore());

        return assessmentAttemptRepository.save(attempt);
    }

    public List<AssessmentAttempt> list() {
        return assessmentAttemptRepository.findAll();
    }

    public AssessmentAttempt get(UUID id) {
        return assessmentAttemptRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Assessment attempt not found: " + id));
    }
}
