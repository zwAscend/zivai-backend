package zw.co.zivai.core_backend.common.services.assessments;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import zw.co.zivai.core_backend.common.dtos.assessments.CreateAttemptAnswerRequest;
import zw.co.zivai.core_backend.common.exceptions.NotFoundException;
import zw.co.zivai.core_backend.common.models.lms.assessments.AssessmentAttempt;
import zw.co.zivai.core_backend.common.models.lms.assessments.AssessmentQuestion;
import zw.co.zivai.core_backend.common.models.lms.assessments.AttemptAnswer;
import zw.co.zivai.core_backend.common.models.lms.resources.Resource;
import zw.co.zivai.core_backend.common.repositories.assessments.AssessmentAttemptRepository;
import zw.co.zivai.core_backend.common.repositories.assessments.AttemptAnswerRepository;
import zw.co.zivai.core_backend.common.repositories.resource.ResourceRepository;
import zw.co.zivai.core_backend.common.repositories.assessments.AssessmentQuestionRepository;

@Service
@RequiredArgsConstructor
public class AttemptAnswerService {
    private final AttemptAnswerRepository attemptAnswerRepository;
    private final AssessmentAttemptRepository assessmentAttemptRepository;
    private final AssessmentQuestionRepository assessmentQuestionRepository;
    private final ResourceRepository resourceRepository;
    private final ObjectMapper objectMapper;

    public AttemptAnswer create(CreateAttemptAnswerRequest request) {
        AssessmentAttempt attempt = assessmentAttemptRepository.findById(request.getAssessmentAttemptId())
            .orElseThrow(() -> new NotFoundException("Assessment attempt not found: " + request.getAssessmentAttemptId()));
        AssessmentQuestion question = assessmentQuestionRepository.findById(request.getAssessmentQuestionId())
            .orElseThrow(() -> new NotFoundException("Assessment question not found: " + request.getAssessmentQuestionId()));

        Resource handwriting = null;
        if (request.getHandwritingResourceId() != null) {
            handwriting = resourceRepository.findById(request.getHandwritingResourceId())
                .orElseThrow(() -> new NotFoundException("Resource not found: " + request.getHandwritingResourceId()));
        }

        AttemptAnswer answer = new AttemptAnswer();
        answer.setAssessmentAttempt(attempt);
        answer.setAssessmentQuestion(question);
        answer.setStudentAnswerText(request.getStudentAnswerText());
        answer.setStudentAnswerBlob(parseJsonNode(request.getStudentAnswerBlob()));
        answer.setHandwritingResource(handwriting);
        answer.setOcrText(request.getOcrText());
        answer.setOcrConfidence(request.getOcrConfidence());
        answer.setOcrEngine(request.getOcrEngine());
        answer.setOcrLanguage(request.getOcrLanguage());
        answer.setOcrMetadata(parseJsonNode(request.getOcrMetadata()));
        answer.setAiScore(request.getAiScore());
        answer.setHumanScore(request.getHumanScore());
        answer.setMaxScore(request.getMaxScore());
        answer.setAiConfidence(request.getAiConfidence());
        answer.setRequiresReview(request.isRequiresReview());
        answer.setFeedbackText(request.getFeedbackText());
        answer.setAnswerTraceId(request.getAnswerTraceId());

        return attemptAnswerRepository.save(answer);
    }

    public List<AttemptAnswer> list() {
        return attemptAnswerRepository.findAll();
    }

    public AttemptAnswer get(UUID id) {
        return attemptAnswerRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Attempt answer not found: " + id));
    }

    private JsonNode parseJsonNode(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(value);
        } catch (Exception ex) {
            return objectMapper.getNodeFactory().textNode(value);
        }
    }
}
