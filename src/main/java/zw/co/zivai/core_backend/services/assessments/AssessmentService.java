package zw.co.zivai.core_backend.services.assessments;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import zw.co.zivai.core_backend.dtos.assessments.AssessmentQuestionDto;
import zw.co.zivai.core_backend.dtos.assessments.AssessmentWithQuestionsDto;
import zw.co.zivai.core_backend.dtos.assessments.CreateAssessmentQuestionRequest;
import zw.co.zivai.core_backend.dtos.assessments.CreateAssessmentRequest;
import zw.co.zivai.core_backend.dtos.assessments.UpdateAssessmentRequest;
import zw.co.zivai.core_backend.exceptions.BadRequestException;
import zw.co.zivai.core_backend.exceptions.NotFoundException;
import zw.co.zivai.core_backend.models.lms.Assessment;
import zw.co.zivai.core_backend.models.lms.AssessmentQuestion;
import zw.co.zivai.core_backend.models.lms.Question;
import zw.co.zivai.core_backend.models.lms.Resource;
import zw.co.zivai.core_backend.models.lms.School;
import zw.co.zivai.core_backend.models.lms.Subject;
import zw.co.zivai.core_backend.models.lms.User;
import zw.co.zivai.core_backend.repositories.assessments.AssessmentRepository;
import zw.co.zivai.core_backend.repositories.assessments.AssessmentQuestionRepository;
import zw.co.zivai.core_backend.repositories.assessments.QuestionRepository;
import zw.co.zivai.core_backend.repositories.resource.ResourceRepository;
import zw.co.zivai.core_backend.repositories.school.SchoolRepository;
import zw.co.zivai.core_backend.repositories.subject.SubjectRepository;
import zw.co.zivai.core_backend.repositories.user.UserRepository;

@Service
@RequiredArgsConstructor
public class AssessmentService {
    private final AssessmentRepository assessmentRepository;
    private final SchoolRepository schoolRepository;
    private final SubjectRepository subjectRepository;
    private final ResourceRepository resourceRepository;
    private final UserRepository userRepository;
    private final QuestionRepository questionRepository;
    private final AssessmentQuestionRepository assessmentQuestionRepository;

    public Assessment create(CreateAssessmentRequest request) {
        if (request.getSubjectId() == null) {
            throw new BadRequestException("Subject is required");
        }
        if (request.getName() == null || request.getName().isBlank()) {
            throw new BadRequestException("Assessment name is required");
        }
        if (request.getAssessmentType() == null || request.getAssessmentType().isBlank()) {
            throw new BadRequestException("Assessment type is required");
        }

        School school = request.getSchoolId() != null
            ? schoolRepository.findById(request.getSchoolId())
                .orElseThrow(() -> new NotFoundException("School not found: " + request.getSchoolId()))
            : resolveSchool();
        Subject subject = subjectRepository.findById(request.getSubjectId())
            .orElseThrow(() -> new NotFoundException("Subject not found: " + request.getSubjectId()));
        User createdBy = request.getCreatedBy() != null
            ? userRepository.findById(request.getCreatedBy())
                .orElseThrow(() -> new NotFoundException("User not found: " + request.getCreatedBy()))
            : resolveUser();
        User modifiedBy = request.getLastModifiedBy() != null
            ? userRepository.findById(request.getLastModifiedBy())
                .orElseThrow(() -> new NotFoundException("User not found: " + request.getLastModifiedBy()))
            : createdBy;

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

        Assessment saved = assessmentRepository.save(assessment);

        attachQuestions(saved, subject, createdBy, request.getQuestions());

        return saved;
    }

    public Assessment update(UUID id, UpdateAssessmentRequest request) {
        Assessment assessment = get(id);

        if (request.getSchoolId() != null) {
            School school = schoolRepository.findById(request.getSchoolId())
                .orElseThrow(() -> new NotFoundException("School not found: " + request.getSchoolId()));
            assessment.setSchool(school);
        }
        if (request.getSubjectId() != null) {
            Subject subject = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> new NotFoundException("Subject not found: " + request.getSubjectId()));
            assessment.setSubject(subject);
        }
        if (request.getName() != null) {
            assessment.setName(request.getName());
        }
        if (request.getDescription() != null) {
            assessment.setDescription(request.getDescription());
        }
        if (request.getAssessmentType() != null) {
            assessment.setAssessmentType(request.getAssessmentType());
        }
        if (request.getVisibility() != null) {
            assessment.setVisibility(request.getVisibility());
        }
        if (request.getTimeLimitMin() != null) {
            assessment.setTimeLimitMin(request.getTimeLimitMin());
        }
        if (request.getAttemptsAllowed() != null) {
            assessment.setAttemptsAllowed(request.getAttemptsAllowed());
        }
        if (request.getMaxScore() != null) {
            assessment.setMaxScore(request.getMaxScore());
        }
        if (request.getWeightPct() != null) {
            assessment.setWeightPct(request.getWeightPct());
        }
        if (request.getResourceId() != null) {
            Resource resource = resourceRepository.findById(request.getResourceId())
                .orElseThrow(() -> new NotFoundException("Resource not found: " + request.getResourceId()));
            assessment.setResource(resource);
        }
        if (request.getAiEnhanced() != null) {
            assessment.setAiEnhanced(request.getAiEnhanced());
        }
        if (request.getStatus() != null) {
            assessment.setStatus(request.getStatus());
        }
        if (request.getLastModifiedBy() != null) {
            User modifiedBy = userRepository.findById(request.getLastModifiedBy())
                .orElseThrow(() -> new NotFoundException("User not found: " + request.getLastModifiedBy()));
            assessment.setLastModifiedBy(modifiedBy);
        }

        Assessment saved = assessmentRepository.save(assessment);

        if (request.getQuestions() != null) {
            assessmentQuestionRepository.deleteByAssessment_Id(saved.getId());
            attachQuestions(saved, saved.getSubject(), saved.getLastModifiedBy(), request.getQuestions());
        }

        return saved;
    }

    public Assessment setStatus(UUID id, String status) {
        Assessment assessment = get(id);
        assessment.setStatus(status);
        return assessmentRepository.save(assessment);
    }

    public AssessmentWithQuestionsDto addQuestions(UUID assessmentId, List<CreateAssessmentQuestionRequest> questions) {
        Assessment assessment = get(assessmentId);
        if (questions == null || questions.isEmpty()) {
            return getWithQuestions(assessmentId);
        }

        int maxSequence = assessmentQuestionRepository.findByAssessment_IdOrderBySequenceIndexAsc(assessmentId).stream()
            .map(AssessmentQuestion::getSequenceIndex)
            .filter(sequence -> sequence != null)
            .max(Integer::compareTo)
            .orElse(0);

        Subject subject = assessment.getSubject();
        User author = assessment.getLastModifiedBy() != null ? assessment.getLastModifiedBy() : assessment.getCreatedBy();

        int order = maxSequence + 1;
        for (var questionRequest : questions) {
            Question question = new Question();
            question.setSubject(subject);
            question.setAuthor(author);
            question.setStem(questionRequest.getStem());
            question.setQuestionTypeCode(questionRequest.getQuestionTypeCode());
            question.setMaxMark(questionRequest.getMaxMark() != null ? questionRequest.getMaxMark() : 1.0);
            if (questionRequest.getDifficulty() != null) {
                question.setDifficulty(questionRequest.getDifficulty().shortValue());
            }
            question.setRubricJson(questionRequest.getRubricJson());
            Question savedQuestion = questionRepository.save(question);

            AssessmentQuestion assessmentQuestion = new AssessmentQuestion();
            assessmentQuestion.setAssessment(assessment);
            assessmentQuestion.setQuestion(savedQuestion);
            int sequenceIndex = questionRequest.getSequenceIndex() != null ? questionRequest.getSequenceIndex() : order;
            assessmentQuestion.setSequenceIndex(sequenceIndex);
            assessmentQuestion.setPoints(questionRequest.getPoints() != null ? questionRequest.getPoints() : savedQuestion.getMaxMark());
            assessmentQuestionRepository.save(assessmentQuestion);
            order++;
        }

        return getWithQuestions(assessmentId);
    }

    public AssessmentWithQuestionsDto replaceQuestions(UUID assessmentId, List<CreateAssessmentQuestionRequest> questions) {
        Assessment assessment = get(assessmentId);
        assessmentQuestionRepository.deleteByAssessment_Id(assessmentId);
        if (questions == null || questions.isEmpty()) {
            return getWithQuestions(assessmentId);
        }

        Subject subject = assessment.getSubject();
        User author = assessment.getLastModifiedBy() != null ? assessment.getLastModifiedBy() : assessment.getCreatedBy();
        attachQuestions(assessment, subject, author, questions);
        return getWithQuestions(assessmentId);
    }

    public void delete(UUID id) {
        Assessment assessment = get(id);
        assessment.setDeletedAt(Instant.now());
        assessment.setStatus("archived");
        assessmentRepository.save(assessment);
    }

    public List<Assessment> list(UUID subjectId, String status) {
        if (subjectId != null && status != null && !status.isBlank()) {
            return assessmentRepository.findBySubject_IdAndStatus(subjectId, status);
        }
        if (subjectId != null) {
            return assessmentRepository.findBySubject_Id(subjectId);
        }
        if (status != null && !status.isBlank()) {
            return assessmentRepository.findByStatus(status);
        }
        return assessmentRepository.findAll();
    }

    public Assessment get(UUID id) {
        return assessmentRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Assessment not found: " + id));
    }

    public AssessmentWithQuestionsDto getWithQuestions(UUID id) {
        Assessment assessment = get(id);
        List<AssessmentQuestionDto> questions = assessmentQuestionRepository.findByAssessment_IdOrderBySequenceIndexAsc(id).stream()
            .map(assessmentQuestion -> {
                Question question = assessmentQuestion.getQuestion();
                return AssessmentQuestionDto.builder()
                    .id(question.getId().toString())
                    .stem(question.getStem())
                    .questionTypeCode(question.getQuestionTypeCode())
                    .maxMark(question.getMaxMark())
                    .difficulty(question.getDifficulty() != null ? question.getDifficulty().intValue() : null)
                    .rubricJson(question.getRubricJson())
                    .sequenceIndex(assessmentQuestion.getSequenceIndex())
                    .points(assessmentQuestion.getPoints())
                    .build();
            })
            .toList();

        return AssessmentWithQuestionsDto.builder()
            .id(assessment.getId().toString())
            .schoolId(assessment.getSchool() != null ? assessment.getSchool().getId().toString() : null)
            .subjectId(assessment.getSubject() != null ? assessment.getSubject().getId().toString() : null)
            .name(assessment.getName())
            .description(assessment.getDescription())
            .assessmentType(assessment.getAssessmentType())
            .visibility(assessment.getVisibility())
            .timeLimitMin(assessment.getTimeLimitMin())
            .attemptsAllowed(assessment.getAttemptsAllowed())
            .maxScore(assessment.getMaxScore())
            .weightPct(assessment.getWeightPct())
            .resourceId(assessment.getResource() != null ? assessment.getResource().getId().toString() : null)
            .aiEnhanced(assessment.isAiEnhanced())
            .status(assessment.getStatus())
            .createdBy(assessment.getCreatedBy() != null ? assessment.getCreatedBy().getId().toString() : null)
            .lastModifiedBy(assessment.getLastModifiedBy() != null ? assessment.getLastModifiedBy().getId().toString() : null)
            .createdAt(assessment.getCreatedAt())
            .updatedAt(assessment.getUpdatedAt())
            .questions(questions)
            .build();
    }

    private void attachQuestions(Assessment assessment, Subject subject, User author, List<CreateAssessmentQuestionRequest> questions) {
        if (questions == null || questions.isEmpty()) {
            return;
        }
        int order = 1;
        for (var questionRequest : questions) {
            if (questionRequest.getStem() == null || questionRequest.getStem().isBlank()) {
                throw new BadRequestException("Question stem is required");
            }
            if (questionRequest.getQuestionTypeCode() == null || questionRequest.getQuestionTypeCode().isBlank()) {
                throw new BadRequestException("Question type is required");
            }
            Question question = new Question();
            question.setSubject(subject);
            question.setAuthor(author);
            question.setStem(questionRequest.getStem());
            question.setQuestionTypeCode(questionRequest.getQuestionTypeCode());
            question.setMaxMark(questionRequest.getMaxMark() != null ? questionRequest.getMaxMark() : 1.0);
            if (questionRequest.getDifficulty() != null) {
                question.setDifficulty(questionRequest.getDifficulty().shortValue());
            }
            question.setRubricJson(questionRequest.getRubricJson());
            Question savedQuestion = questionRepository.save(question);

            AssessmentQuestion assessmentQuestion = new AssessmentQuestion();
            assessmentQuestion.setAssessment(assessment);
            assessmentQuestion.setQuestion(savedQuestion);
            int sequenceIndex = questionRequest.getSequenceIndex() != null ? questionRequest.getSequenceIndex() : order;
            assessmentQuestion.setSequenceIndex(sequenceIndex);
            assessmentQuestion.setPoints(questionRequest.getPoints() != null ? questionRequest.getPoints() : savedQuestion.getMaxMark());
            assessmentQuestionRepository.save(assessmentQuestion);
            order++;
        }
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
}
