package zw.co.zivai.core_backend.common.services.assessments;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import zw.co.zivai.core_backend.common.dtos.assessments.AssessmentQuestionDto;
import zw.co.zivai.core_backend.common.dtos.assessments.AssessmentWithQuestionsDto;
import zw.co.zivai.core_backend.common.dtos.assessments.CreateAssessmentQuestionRequest;
import zw.co.zivai.core_backend.common.dtos.assessments.CreateAssessmentRequest;
import zw.co.zivai.core_backend.common.dtos.assessments.UpdateAssessmentRequest;
import zw.co.zivai.core_backend.common.exceptions.BadRequestException;
import zw.co.zivai.core_backend.common.exceptions.NotFoundException;
import zw.co.zivai.core_backend.common.models.lms.assessments.Assessment;
import zw.co.zivai.core_backend.common.models.lms.assessments.AssessmentQuestion;
import zw.co.zivai.core_backend.common.models.lms.resources.Question;
import zw.co.zivai.core_backend.common.models.lms.resources.Resource;
import zw.co.zivai.core_backend.common.models.lms.school.School;
import zw.co.zivai.core_backend.common.models.lms.subjects.Subject;
import zw.co.zivai.core_backend.common.models.lms.users.User;
import zw.co.zivai.core_backend.common.repositories.assessments.AssessmentRepository;
import zw.co.zivai.core_backend.common.repositories.assessments.AssessmentQuestionRepository;
import zw.co.zivai.core_backend.common.repositories.assessments.QuestionRepository;
import zw.co.zivai.core_backend.common.repositories.resource.ResourceRepository;
import zw.co.zivai.core_backend.common.repositories.school.SchoolRepository;
import zw.co.zivai.core_backend.common.repositories.subject.SubjectRepository;
import zw.co.zivai.core_backend.common.repositories.user.UserRepository;

@Service
@RequiredArgsConstructor
public class AssessmentService {
    private static final ObjectMapper RUBRIC_JSON_MAPPER = new ObjectMapper();
    private static final Set<String> SUPPORTED_QUESTION_TYPE_CODES = Set.of(
        "short_answer",
        "structured",
        "mcq",
        "true_false",
        "essay"
    );

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
        attachQuestions(assessment, subject, author, questions, order);

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
        assessmentRepository.findById(id).ifPresent(assessment -> {
            if (assessment.getDeletedAt() != null) {
                return;
            }
            assessment.setDeletedAt(Instant.now());
            assessment.setStatus("archived");
            assessmentRepository.save(assessment);
        });
    }

    public List<Assessment> list(UUID subjectId, String status) {
        if (subjectId != null && status != null && !status.isBlank()) {
            return assessmentRepository.findBySubject_IdAndStatusAndDeletedAtIsNull(subjectId, status);
        }
        if (subjectId != null) {
            return assessmentRepository.findBySubject_IdAndDeletedAtIsNull(subjectId);
        }
        if (status != null && !status.isBlank()) {
            return assessmentRepository.findByStatusAndDeletedAtIsNull(status);
        }
        return assessmentRepository.findByDeletedAtIsNull();
    }

    public Assessment get(UUID id) {
        return assessmentRepository.findByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> new NotFoundException("Assessment not found: " + id));
    }

    public AssessmentWithQuestionsDto getWithQuestions(UUID id) {
        Assessment assessment = get(id);
        List<AssessmentQuestionDto> questions = assessmentQuestionRepository.findByAssessment_IdOrderBySequenceIndexAsc(id).stream()
            .map(assessmentQuestion -> {
                Question question = assessmentQuestion.getQuestion();
                return AssessmentQuestionDto.builder()
                    .assessmentQuestionId(assessmentQuestion.getId().toString())
                    .questionId(question.getId().toString())
                    .id(question.getId().toString())
                    .stem(question.getStem())
                    .questionTypeCode(mapQuestionTypeCodeForApi(question.getQuestionTypeCode()))
                    .maxMark(question.getMaxMark())
                    .difficulty(question.getDifficulty() != null ? question.getDifficulty().intValue() : null)
                    .rubricJson(fromRubricJson(question.getRubricJson()))
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
        attachQuestions(assessment, subject, author, questions, 1);
    }

    private void attachQuestions(Assessment assessment,
                                 Subject subject,
                                 User author,
                                 List<CreateAssessmentQuestionRequest> questions,
                                 int startingOrder) {
        if (questions == null || questions.isEmpty()) {
            return;
        }
        List<Question> questionsToPersist = new ArrayList<>();
        for (var questionRequest : questions) {
            validateQuestionRequest(questionRequest);
            questionsToPersist.add(buildQuestion(subject, author, questionRequest));
        }

        List<Question> savedQuestions = questionRepository.saveAll(questionsToPersist);
        List<AssessmentQuestion> links = new ArrayList<>(savedQuestions.size());
        int order = startingOrder;
        for (int index = 0; index < questions.size(); index += 1) {
            CreateAssessmentQuestionRequest questionRequest = questions.get(index);
            Question savedQuestion = savedQuestions.get(index);
            AssessmentQuestion assessmentQuestion = new AssessmentQuestion();
            assessmentQuestion.setAssessment(assessment);
            assessmentQuestion.setQuestion(savedQuestion);
            int sequenceIndex = questionRequest.getSequenceIndex() != null ? questionRequest.getSequenceIndex() : order;
            assessmentQuestion.setSequenceIndex(sequenceIndex);
            assessmentQuestion.setPoints(questionRequest.getPoints() != null ? questionRequest.getPoints() : savedQuestion.getMaxMark());
            links.add(assessmentQuestion);
            order++;
        }
        assessmentQuestionRepository.saveAll(links);
    }

    private Question buildQuestion(Subject subject, User author, CreateAssessmentQuestionRequest questionRequest) {
        Question question = new Question();
        question.setSubject(subject);
        question.setAuthor(author);
        question.setStem(questionRequest.getStem());
        question.setQuestionTypeCode(normalizeQuestionTypeCodeForStorage(questionRequest.getQuestionTypeCode()));
        question.setMaxMark(questionRequest.getMaxMark() != null ? questionRequest.getMaxMark() : 1.0);
        if (questionRequest.getDifficulty() != null) {
            question.setDifficulty(questionRequest.getDifficulty().shortValue());
        }
        question.setRubricJson(toRubricJson(questionRequest.getRubricJson()));
        return question;
    }

    private JsonNode toRubricJson(Object rubricJson) {
        if (rubricJson == null) {
            return null;
        }
        if (rubricJson instanceof JsonNode node) {
            return normalizeRubricJson(node);
        }
        if (rubricJson instanceof String raw) {
            String trimmed = raw.trim();
            if (trimmed.isEmpty()) {
                return null;
            }
            try {
                return normalizeRubricJson(RUBRIC_JSON_MAPPER.readTree(trimmed));
            } catch (Exception ex) {
                throw new BadRequestException("Invalid rubricJson payload.");
            }
        }
        return normalizeRubricJson(RUBRIC_JSON_MAPPER.valueToTree(rubricJson));
    }

    private Object fromRubricJson(JsonNode rubricJson) {
        if (rubricJson == null || rubricJson.isNull()) {
            return null;
        }
        return RUBRIC_JSON_MAPPER.convertValue(rubricJson, Object.class);
    }

    private void validateQuestionRequest(CreateAssessmentQuestionRequest questionRequest) {
        if (questionRequest.getStem() == null || questionRequest.getStem().isBlank()) {
            throw new BadRequestException("Question stem is required");
        }
        if (questionRequest.getQuestionTypeCode() == null || questionRequest.getQuestionTypeCode().isBlank()) {
            throw new BadRequestException("Question type is required");
        }
        String normalizedQuestionType = normalizeQuestionTypeCodeForStorage(questionRequest.getQuestionTypeCode());
        if (!SUPPORTED_QUESTION_TYPE_CODES.contains(normalizedQuestionType)) {
            throw new BadRequestException("Unsupported question type: " + questionRequest.getQuestionTypeCode());
        }
        JsonNode rubricNode = toRubricJson(questionRequest.getRubricJson());
        if (!hasMandatoryAnswer(normalizedQuestionType, rubricNode)) {
            throw new BadRequestException("Each question must include at least one expected/correct answer.");
        }
    }

    private String normalizeQuestionTypeCodeForStorage(String rawQuestionTypeCode) {
        if (rawQuestionTypeCode == null) {
            return null;
        }
        String normalized = rawQuestionTypeCode
            .trim()
            .toLowerCase(Locale.ROOT)
            .replace('-', '_')
            .replace(' ', '_');

        return switch (normalized) {
            case "multiple_choice", "multiplechoice", "mcq" -> "mcq";
            case "truefalse", "boolean", "true_false" -> "true_false";
            case "shortanswer", "short_answer" -> "short_answer";
            case "longanswer", "long_answer" -> "essay";
            default -> normalized;
        };
    }

    private String mapQuestionTypeCodeForApi(String rawQuestionTypeCode) {
        String normalized = normalizeQuestionTypeCodeForStorage(rawQuestionTypeCode);
        if ("mcq".equals(normalized)) {
            return "multiple_choice";
        }
        return normalized;
    }

    private JsonNode normalizeRubricJson(JsonNode rubricNode) {
        if (rubricNode == null || rubricNode.isNull() || !rubricNode.isObject()) {
            return rubricNode;
        }

        ObjectNode objectNode = ((ObjectNode) rubricNode).deepCopy();
        LinkedHashSet<String> mergedPoints = new LinkedHashSet<>();

        JsonNode markingPointsNode = objectNode.path("markingPoints");
        if (markingPointsNode.isArray()) {
            for (JsonNode pointNode : markingPointsNode) {
                String point = pointNode == null ? "" : pointNode.asText("");
                if (!point.isBlank()) {
                    mergedPoints.add(point.trim());
                }
            }
        }

        String markingGuide = objectNode.path("markingGuide").asText("");
        if (!markingGuide.isBlank()) {
            String[] lines = markingGuide.split("\\r?\\n");
            for (String line : lines) {
                String cleaned = line.replaceFirst("^\\s*[-*]\\s*", "").trim();
                if (!cleaned.isBlank()) {
                    mergedPoints.add(cleaned);
                }
            }
        }

        if (!mergedPoints.isEmpty()) {
            ArrayNode normalizedPoints = RUBRIC_JSON_MAPPER.createArrayNode();
            for (String point : mergedPoints) {
                normalizedPoints.add(point);
            }
            objectNode.set("markingPoints", normalizedPoints);
            if (markingGuide.isBlank()) {
                objectNode.put("markingGuide", String.join("\n", mergedPoints));
            }
        }

        return objectNode;
    }

    private boolean hasMandatoryAnswer(String normalizedQuestionType, JsonNode rubricNode) {
        if (rubricNode == null || rubricNode.isNull()) {
            return false;
        }

        if ("mcq".equals(normalizedQuestionType) || "true_false".equals(normalizedQuestionType)) {
            return hasAtLeastOneCorrectAnswer(rubricNode);
        }

        return hasAtLeastOneMarkingPoint(rubricNode) || hasAnyNonBlankText(rubricNode,
            "markingGuide",
            "expectedAnswer",
            "correctAnswer",
            "answer",
            "modelAnswer"
        );
    }

    private boolean hasAtLeastOneCorrectAnswer(JsonNode rubricNode) {
        if (!rubricNode.isObject()) {
            return hasNonBlankText(rubricNode);
        }

        JsonNode answersNode = rubricNode.path("correctAnswers");
        if (answersNode.isArray()) {
            for (JsonNode answerNode : answersNode) {
                if (hasNonBlankText(answerNode)) {
                    return true;
                }
            }
        } else if (hasNonBlankText(answersNode)) {
            return true;
        }

        return hasAnyNonBlankText(rubricNode, "correctAnswer", "expectedAnswer", "answer");
    }

    private boolean hasAtLeastOneMarkingPoint(JsonNode rubricNode) {
        if (!rubricNode.isObject()) {
            return false;
        }
        JsonNode pointsNode = rubricNode.path("markingPoints");
        if (!pointsNode.isArray()) {
            return false;
        }
        for (JsonNode pointNode : pointsNode) {
            if (hasNonBlankText(pointNode)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasAnyNonBlankText(JsonNode rubricNode, String... keys) {
        if (!rubricNode.isObject()) {
            return hasNonBlankText(rubricNode);
        }
        for (String key : keys) {
            if (hasNonBlankText(rubricNode.path(key))) {
                return true;
            }
        }
        return false;
    }

    private boolean hasNonBlankText(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return false;
        }
        if (node.isTextual()) {
            return !node.asText("").isBlank();
        }
        String value = node.asText("");
        return !value.isBlank();
    }

    private School resolveSchool() {
        return schoolRepository.findByCode("ZVHS")
            .orElseGet(() -> schoolRepository.findFirstByDeletedAtIsNullOrderByCreatedAtAsc()
                .orElseThrow(() -> new NotFoundException("No school found")));
    }

    private User resolveUser() {
        return userRepository.findByEmailAndDeletedAtIsNull("teacher@zivai.local")
            .orElseGet(() -> userRepository.findFirstByDeletedAtIsNullOrderByCreatedAtAsc()
                .orElseThrow(() -> new NotFoundException("No user found")));
    }
}
