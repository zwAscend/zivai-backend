package zw.co.zivai.core_backend.common.services.sync;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import zw.co.zivai.core_backend.common.models.base.BaseEntity;
import zw.co.zivai.core_backend.common.models.lms.assessments.Assessment;
import zw.co.zivai.core_backend.common.models.lms.assessments.AssessmentAssignment;
import zw.co.zivai.core_backend.common.models.lms.assessments.AssessmentAttempt;
import zw.co.zivai.core_backend.common.models.lms.assessments.AssessmentEnrollment;
import zw.co.zivai.core_backend.common.models.lms.assessments.AssessmentQuestion;
import zw.co.zivai.core_backend.common.models.lms.assessments.AssessmentResult;
import zw.co.zivai.core_backend.common.models.lms.assessments.AttemptAnswer;
import zw.co.zivai.core_backend.common.models.lms.development.Plan;
import zw.co.zivai.core_backend.common.models.lms.development.PlanStep;
import zw.co.zivai.core_backend.common.models.lms.resources.MarkingScheme;
import zw.co.zivai.core_backend.common.models.lms.resources.Question;
import zw.co.zivai.core_backend.common.models.lms.resources.Resource;
import zw.co.zivai.core_backend.common.models.lms.resources.Topic;
import zw.co.zivai.core_backend.common.models.lms.resources.TopicResource;
import zw.co.zivai.core_backend.common.models.lms.school.School;
import zw.co.zivai.core_backend.common.models.lms.students.StudentPlan;
import zw.co.zivai.core_backend.common.models.lms.subjects.Subject;
import zw.co.zivai.core_backend.common.models.lms.users.User;

@Service
@RequiredArgsConstructor
public class SyncPayloadMapper {
    private static final Set<Class<? extends BaseEntity>> SUPPORTED_TYPES = Set.of(
        Subject.class,
        Plan.class,
        PlanStep.class,
        StudentPlan.class,
        Assessment.class,
        AssessmentAssignment.class,
        Resource.class,
        Question.class,
        AssessmentQuestion.class,
        AssessmentEnrollment.class,
        AssessmentAttempt.class,
        AttemptAnswer.class,
        AssessmentResult.class,
        TopicResource.class
    );

    private final EntityManager entityManager;
    private final ObjectMapper objectMapper;

    public boolean supportsAggregate(BaseEntity entity) {
        return entity != null && SUPPORTED_TYPES.contains(entity.getClass());
    }

    public boolean supportsAggregateType(String aggregateType) {
        return resolveAggregateType(aggregateType).isPresent();
    }

    public String aggregateTypeOf(BaseEntity entity) {
        return entity.getClass().getSimpleName();
    }

    public Optional<Class<? extends BaseEntity>> resolveAggregateType(String aggregateType) {
        return SUPPORTED_TYPES.stream()
            .filter(type -> type.getSimpleName().equals(aggregateType) || type.getName().equals(aggregateType))
            .findFirst();
    }

    public JsonNode toPayload(BaseEntity entity) {
        if (entity instanceof Subject subject) {
            return subjectPayload(subject);
        }
        if (entity instanceof Plan plan) {
            return planPayload(plan);
        }
        if (entity instanceof PlanStep planStep) {
            return planStepPayload(planStep);
        }
        if (entity instanceof StudentPlan studentPlan) {
            return studentPlanPayload(studentPlan);
        }
        if (entity instanceof Assessment assessment) {
            return assessmentPayload(assessment);
        }
        if (entity instanceof AssessmentAssignment assignment) {
            return assessmentAssignmentPayload(assignment);
        }
        if (entity instanceof Resource resource) {
            return resourcePayload(resource);
        }
        if (entity instanceof Question question) {
            return questionPayload(question);
        }
        if (entity instanceof AssessmentQuestion assessmentQuestion) {
            return assessmentQuestionPayload(assessmentQuestion);
        }
        if (entity instanceof AssessmentEnrollment assessmentEnrollment) {
            return assessmentEnrollmentPayload(assessmentEnrollment);
        }
        if (entity instanceof AssessmentAttempt assessmentAttempt) {
            return assessmentAttemptPayload(assessmentAttempt);
        }
        if (entity instanceof AttemptAnswer attemptAnswer) {
            return attemptAnswerPayload(attemptAnswer);
        }
        if (entity instanceof AssessmentResult assessmentResult) {
            return assessmentResultPayload(assessmentResult);
        }
        if (entity instanceof TopicResource topicResource) {
            return topicResourcePayload(topicResource);
        }
        throw new IllegalArgumentException("Unsupported aggregate type for sync payload: " + entity.getClass().getName());
    }

    public BaseEntity fromPayload(String aggregateType, JsonNode payload, BaseEntity existing) {
        if (Subject.class.getSimpleName().equals(aggregateType)) {
            return hydrateSubject(payload, existing instanceof Subject subject ? subject : new Subject());
        }
        if (Plan.class.getSimpleName().equals(aggregateType)) {
            return hydratePlan(payload, existing instanceof Plan plan ? plan : new Plan());
        }
        if (PlanStep.class.getSimpleName().equals(aggregateType)) {
            return hydratePlanStep(payload, existing instanceof PlanStep planStep ? planStep : new PlanStep());
        }
        if (StudentPlan.class.getSimpleName().equals(aggregateType)) {
            return hydrateStudentPlan(payload, existing instanceof StudentPlan studentPlan ? studentPlan : new StudentPlan());
        }
        if (Assessment.class.getSimpleName().equals(aggregateType)) {
            return hydrateAssessment(payload, existing instanceof Assessment assessment ? assessment : new Assessment());
        }
        if (AssessmentAssignment.class.getSimpleName().equals(aggregateType)) {
            return hydrateAssessmentAssignment(payload, existing instanceof AssessmentAssignment assignment ? assignment : new AssessmentAssignment());
        }
        if (Resource.class.getSimpleName().equals(aggregateType)) {
            return hydrateResource(payload, existing instanceof Resource resource ? resource : new Resource());
        }
        if (Question.class.getSimpleName().equals(aggregateType)) {
            return hydrateQuestion(payload, existing instanceof Question question ? question : new Question());
        }
        if (AssessmentQuestion.class.getSimpleName().equals(aggregateType)) {
            return hydrateAssessmentQuestion(
                payload,
                existing instanceof AssessmentQuestion assessmentQuestion ? assessmentQuestion : new AssessmentQuestion()
            );
        }
        if (AssessmentEnrollment.class.getSimpleName().equals(aggregateType)) {
            return hydrateAssessmentEnrollment(
                payload,
                existing instanceof AssessmentEnrollment assessmentEnrollment ? assessmentEnrollment : new AssessmentEnrollment()
            );
        }
        if (AssessmentAttempt.class.getSimpleName().equals(aggregateType)) {
            return hydrateAssessmentAttempt(
                payload,
                existing instanceof AssessmentAttempt assessmentAttempt ? assessmentAttempt : new AssessmentAttempt()
            );
        }
        if (AttemptAnswer.class.getSimpleName().equals(aggregateType)) {
            return hydrateAttemptAnswer(payload, existing instanceof AttemptAnswer attemptAnswer ? attemptAnswer : new AttemptAnswer());
        }
        if (AssessmentResult.class.getSimpleName().equals(aggregateType)) {
            return hydrateAssessmentResult(payload, existing instanceof AssessmentResult assessmentResult ? assessmentResult : new AssessmentResult());
        }
        if (TopicResource.class.getSimpleName().equals(aggregateType)) {
            return hydrateTopicResource(payload, existing instanceof TopicResource topicResource ? topicResource : new TopicResource());
        }
        throw new IllegalArgumentException("Unsupported aggregate type: " + aggregateType);
    }

    private ObjectNode subjectPayload(Subject subject) {
        ObjectNode node = baseNode(subject);
        node.put("code", subject.getCode());
        node.put("name", subject.getName());
        putNullable(node, "examBoardCode", subject.getExamBoardCode());
        putNullable(node, "description", subject.getDescription());
        node.set("subjectAttributes", subject.getSubjectAttributes());
        node.put("active", subject.isActive());
        return node;
    }

    private ObjectNode planPayload(Plan plan) {
        ObjectNode node = baseNode(plan);
        putRef(node, "subjectId", plan.getSubject() == null ? null : plan.getSubject().getId());
        node.put("name", plan.getName());
        node.put("description", plan.getDescription());
        putNumber(node, "progress", plan.getProgress());
        putNumber(node, "potentialOverall", plan.getPotentialOverall());
        putNumber(node, "etaDays", plan.getEtaDays());
        putNullable(node, "performance", plan.getPerformance());
        return node;
    }

    private ObjectNode planStepPayload(PlanStep planStep) {
        ObjectNode node = baseNode(planStep);
        putRef(node, "planId", planStep.getPlan() == null ? null : planStep.getPlan().getId());
        putNullable(node, "title", planStep.getTitle());
        putNullable(node, "stepType", planStep.getStepType());
        putNullable(node, "content", planStep.getContent());
        putNullable(node, "link", planStep.getLink());
        putNumber(node, "stepOrder", planStep.getStepOrder());
        return node;
    }

    private ObjectNode studentPlanPayload(StudentPlan studentPlan) {
        ObjectNode node = baseNode(studentPlan);
        putRef(node, "studentId", studentPlan.getStudent() == null ? null : studentPlan.getStudent().getId());
        putRef(node, "planId", studentPlan.getPlan() == null ? null : studentPlan.getPlan().getId());
        putRef(node, "subjectId", studentPlan.getSubject() == null ? null : studentPlan.getSubject().getId());
        putInstant(node, "startDate", studentPlan.getStartDate());
        putNumber(node, "currentProgress", studentPlan.getCurrentProgress());
        putRef(node, "activeStepId", studentPlan.getActiveStepId());
        putJson(node, "completedStepIds", studentPlan.getCompletedStepIds());
        putNullable(node, "status", studentPlan.getStatus());
        node.put("current", studentPlan.isCurrent());
        putInstant(node, "completionDate", studentPlan.getCompletionDate());
        return node;
    }

    private ObjectNode assessmentPayload(Assessment assessment) {
        ObjectNode node = baseNode(assessment);
        putRef(node, "schoolId", assessment.getSchool() == null ? null : assessment.getSchool().getId());
        putRef(node, "subjectId", assessment.getSubject() == null ? null : assessment.getSubject().getId());
        putNullable(node, "name", assessment.getName());
        putNullable(node, "description", assessment.getDescription());
        putNullable(node, "assessmentType", assessment.getAssessmentType());
        putNullable(node, "visibility", assessment.getVisibility());
        putNumber(node, "timeLimitMin", assessment.getTimeLimitMin());
        putNumber(node, "attemptsAllowed", assessment.getAttemptsAllowed());
        putNumber(node, "maxScore", assessment.getMaxScore());
        putNumber(node, "weightPct", assessment.getWeightPct());
        putRef(node, "resourceId", assessment.getResource() == null ? null : assessment.getResource().getId());
        node.put("aiEnhanced", assessment.isAiEnhanced());
        putNullable(node, "status", assessment.getStatus());
        putRef(node, "createdById", assessment.getCreatedBy() == null ? null : assessment.getCreatedBy().getId());
        putRef(node, "lastModifiedById", assessment.getLastModifiedBy() == null ? null : assessment.getLastModifiedBy().getId());
        return node;
    }

    private ObjectNode assessmentAssignmentPayload(AssessmentAssignment assignment) {
        ObjectNode node = baseNode(assignment);
        putRef(node, "assessmentId", assignment.getAssessment() == null ? null : assignment.getAssessment().getId());
        putRef(node, "classEntityId", assignment.getClassEntity() == null ? null : assignment.getClassEntity().getId());
        putRef(node, "assignedById", assignment.getAssignedBy() == null ? null : assignment.getAssignedBy().getId());
        putNullable(node, "title", assignment.getTitle());
        putNullable(node, "instructions", assignment.getInstructions());
        putInstant(node, "startTime", assignment.getStartTime());
        putInstant(node, "dueTime", assignment.getDueTime());
        node.put("published", assignment.isPublished());
        return node;
    }

    private ObjectNode resourcePayload(Resource resource) {
        ObjectNode node = baseNode(resource);
        putRef(node, "schoolId", resource.getSchool() == null ? null : resource.getSchool().getId());
        putRef(node, "subjectId", resource.getSubject() == null ? null : resource.getSubject().getId());
        putRef(node, "uploadedById", resource.getUploadedBy() == null ? null : resource.getUploadedBy().getId());
        putNullable(node, "name", resource.getName());
        putNullable(node, "originalName", resource.getOriginalName());
        putNullable(node, "mimeType", resource.getMimeType());
        putNullable(node, "resType", resource.getResType());
        putNumber(node, "sizeBytes", resource.getSizeBytes());
        putNullable(node, "url", resource.getUrl());
        putNullable(node, "storageKey", resource.getStorageKey());
        putNullable(node, "storagePath", resource.getStoragePath());
        putStringArray(node, "tags", resource.getTags());
        putNullable(node, "contentType", resource.getContentType());
        putNullable(node, "contentBody", resource.getContentBody());
        putInstant(node, "publishAt", resource.getPublishAt());
        putNumber(node, "downloads", resource.getDownloads());
        putNumber(node, "displayOrder", resource.getDisplayOrder());
        putNullable(node, "status", resource.getStatus());
        return node;
    }

    private ObjectNode questionPayload(Question question) {
        ObjectNode node = baseNode(question);
        putRef(node, "subjectId", question.getSubject() == null ? null : question.getSubject().getId());
        putRef(node, "topicId", question.getTopic() == null ? null : question.getTopic().getId());
        putRef(node, "authorId", question.getAuthor() == null ? null : question.getAuthor().getId());
        putNullable(node, "code", question.getCode());
        putNullable(node, "stem", question.getStem());
        putNullable(node, "questionTypeCode", question.getQuestionTypeCode());
        putNumber(node, "maxMark", question.getMaxMark());
        putNumber(node, "difficulty", question.getDifficulty());
        putNullable(node, "examStyleCode", question.getExamStyleCode());
        putNumber(node, "sourceYear", question.getSourceYear());
        putJson(node, "rubricJson", question.getRubricJson());
        node.put("active", question.isActive());
        return node;
    }

    private ObjectNode assessmentQuestionPayload(AssessmentQuestion assessmentQuestion) {
        ObjectNode node = baseNode(assessmentQuestion);
        putRef(node, "assessmentId", assessmentQuestion.getAssessment() == null ? null : assessmentQuestion.getAssessment().getId());
        putRef(node, "questionId", assessmentQuestion.getQuestion() == null ? null : assessmentQuestion.getQuestion().getId());
        putNumber(node, "sequenceIndex", assessmentQuestion.getSequenceIndex());
        putNumber(node, "points", assessmentQuestion.getPoints());
        putRef(node, "rubricSchemeId", assessmentQuestion.getRubricScheme() == null ? null : assessmentQuestion.getRubricScheme().getId());
        putNumber(node, "rubricSchemeVersion", assessmentQuestion.getRubricSchemeVersion());
        return node;
    }

    private ObjectNode assessmentEnrollmentPayload(AssessmentEnrollment assessmentEnrollment) {
        ObjectNode node = baseNode(assessmentEnrollment);
        putRef(
            node,
            "assessmentAssignmentId",
            assessmentEnrollment.getAssessmentAssignment() == null ? null : assessmentEnrollment.getAssessmentAssignment().getId()
        );
        putRef(node, "studentId", assessmentEnrollment.getStudent() == null ? null : assessmentEnrollment.getStudent().getId());
        putNullable(node, "statusCode", assessmentEnrollment.getStatusCode());
        return node;
    }

    private ObjectNode assessmentAttemptPayload(AssessmentAttempt assessmentAttempt) {
        ObjectNode node = baseNode(assessmentAttempt);
        putRef(
            node,
            "assessmentEnrollmentId",
            assessmentAttempt.getAssessmentEnrollment() == null ? null : assessmentAttempt.getAssessmentEnrollment().getId()
        );
        putNumber(node, "attemptNumber", assessmentAttempt.getAttemptNumber());
        putInstant(node, "startedAt", assessmentAttempt.getStartedAt());
        putInstant(node, "submittedAt", assessmentAttempt.getSubmittedAt());
        putNumber(node, "totalScore", assessmentAttempt.getTotalScore());
        putNumber(node, "maxScore", assessmentAttempt.getMaxScore());
        putNumber(node, "finalScore", assessmentAttempt.getFinalScore());
        putNullable(node, "finalGrade", assessmentAttempt.getFinalGrade());
        putNullable(node, "submissionType", assessmentAttempt.getSubmissionType());
        putNullable(node, "originalFilename", assessmentAttempt.getOriginalFilename());
        putNullable(node, "fileType", assessmentAttempt.getFileType());
        putNumber(node, "fileSizeBytes", assessmentAttempt.getFileSizeBytes());
        putNullable(node, "storagePath", assessmentAttempt.getStoragePath());
        putNullable(node, "gradingStatusCode", assessmentAttempt.getGradingStatusCode());
        putNumber(node, "aiConfidence", assessmentAttempt.getAiConfidence());
        putNullable(node, "attemptTraceId", assessmentAttempt.getAttemptTraceId());
        return node;
    }

    private ObjectNode attemptAnswerPayload(AttemptAnswer attemptAnswer) {
        ObjectNode node = baseNode(attemptAnswer);
        putRef(node, "assessmentAttemptId", attemptAnswer.getAssessmentAttempt() == null ? null : attemptAnswer.getAssessmentAttempt().getId());
        putRef(node, "assessmentQuestionId", attemptAnswer.getAssessmentQuestion() == null ? null : attemptAnswer.getAssessmentQuestion().getId());
        putNullable(node, "studentAnswerText", attemptAnswer.getStudentAnswerText());
        putJson(node, "studentAnswerBlob", attemptAnswer.getStudentAnswerBlob());
        putNullable(node, "submissionType", attemptAnswer.getSubmissionType());
        putNullable(node, "textContent", attemptAnswer.getTextContent());
        putJson(node, "externalAssessmentData", attemptAnswer.getExternalAssessmentData());
        putRef(node, "handwritingResourceId", attemptAnswer.getHandwritingResource() == null ? null : attemptAnswer.getHandwritingResource().getId());
        putNullable(node, "ocrText", attemptAnswer.getOcrText());
        putNumber(node, "ocrConfidence", attemptAnswer.getOcrConfidence());
        putNullable(node, "ocrEngine", attemptAnswer.getOcrEngine());
        putNullable(node, "ocrLanguage", attemptAnswer.getOcrLanguage());
        putJson(node, "ocrMetadata", attemptAnswer.getOcrMetadata());
        putNumber(node, "aiScore", attemptAnswer.getAiScore());
        putNumber(node, "humanScore", attemptAnswer.getHumanScore());
        putNumber(node, "maxScore", attemptAnswer.getMaxScore());
        putNumber(node, "aiConfidence", attemptAnswer.getAiConfidence());
        node.put("requiresReview", attemptAnswer.isRequiresReview());
        putNullable(node, "feedbackText", attemptAnswer.getFeedbackText());
        putInstant(node, "gradedAt", attemptAnswer.getGradedAt());
        putNullable(node, "answerTraceId", attemptAnswer.getAnswerTraceId());
        return node;
    }

    private ObjectNode assessmentResultPayload(AssessmentResult assessmentResult) {
        ObjectNode node = baseNode(assessmentResult);
        putRef(
            node,
            "assessmentAssignmentId",
            assessmentResult.getAssessmentAssignment() == null ? null : assessmentResult.getAssessmentAssignment().getId()
        );
        putRef(node, "studentId", assessmentResult.getStudent() == null ? null : assessmentResult.getStudent().getId());
        putRef(node, "finalizedAttemptId", assessmentResult.getFinalizedAttempt() == null ? null : assessmentResult.getFinalizedAttempt().getId());
        putNumber(node, "expectedMark", assessmentResult.getExpectedMark());
        putNumber(node, "actualMark", assessmentResult.getActualMark());
        putNullable(node, "grade", assessmentResult.getGrade());
        putNullable(node, "feedback", assessmentResult.getFeedback());
        putInstant(node, "submittedAt", assessmentResult.getSubmittedAt());
        putInstant(node, "gradedAt", assessmentResult.getGradedAt());
        putNullable(node, "status", assessmentResult.getStatus());
        return node;
    }

    private ObjectNode topicResourcePayload(TopicResource topicResource) {
        ObjectNode node = baseNode(topicResource);
        putRef(node, "topicId", topicResource.getTopic() == null ? null : topicResource.getTopic().getId());
        putRef(node, "resourceId", topicResource.getResource() == null ? null : topicResource.getResource().getId());
        putNumber(node, "displayOrder", topicResource.getDisplayOrder());
        return node;
    }

    private Subject hydrateSubject(JsonNode payload, Subject subject) {
        applyBaseFields(subject, payload);
        subject.setCode(text(payload, "code"));
        subject.setName(text(payload, "name"));
        subject.setExamBoardCode(text(payload, "examBoardCode"));
        subject.setDescription(text(payload, "description"));
        subject.setSubjectAttributes(payload.get("subjectAttributes"));
        subject.setActive(bool(payload, "active", true));
        return subject;
    }

    private Plan hydratePlan(JsonNode payload, Plan plan) {
        applyBaseFields(plan, payload);
        plan.setSubject(reference(Subject.class, uuid(payload, "subjectId")));
        plan.setName(text(payload, "name"));
        plan.setDescription(text(payload, "description"));
        plan.setProgress(doubleValue(payload, "progress"));
        plan.setPotentialOverall(doubleValue(payload, "potentialOverall"));
        plan.setEtaDays(intValue(payload, "etaDays"));
        plan.setPerformance(text(payload, "performance"));
        return plan;
    }

    private PlanStep hydratePlanStep(JsonNode payload, PlanStep planStep) {
        applyBaseFields(planStep, payload);
        planStep.setPlan(reference(Plan.class, uuid(payload, "planId")));
        planStep.setTitle(text(payload, "title"));
        planStep.setStepType(text(payload, "stepType"));
        planStep.setContent(text(payload, "content"));
        planStep.setLink(text(payload, "link"));
        planStep.setStepOrder(intValue(payload, "stepOrder"));
        return planStep;
    }

    private StudentPlan hydrateStudentPlan(JsonNode payload, StudentPlan studentPlan) {
        applyBaseFields(studentPlan, payload);
        studentPlan.setStudent(reference(User.class, uuid(payload, "studentId")));
        studentPlan.setPlan(reference(Plan.class, uuid(payload, "planId")));
        studentPlan.setSubject(reference(Subject.class, uuid(payload, "subjectId")));
        studentPlan.setStartDate(instant(payload, "startDate"));
        studentPlan.setCurrentProgress(doubleValue(payload, "currentProgress"));
        studentPlan.setActiveStepId(uuid(payload, "activeStepId"));
        studentPlan.setCompletedStepIds(json(payload, "completedStepIds"));
        studentPlan.setStatus(text(payload, "status"));
        studentPlan.setCurrent(bool(payload, "current", false));
        studentPlan.setCompletionDate(instant(payload, "completionDate"));
        return studentPlan;
    }

    private Assessment hydrateAssessment(JsonNode payload, Assessment assessment) {
        applyBaseFields(assessment, payload);
        assessment.setSchool(reference(School.class, uuid(payload, "schoolId")));
        assessment.setSubject(reference(Subject.class, uuid(payload, "subjectId")));
        assessment.setName(text(payload, "name"));
        assessment.setDescription(text(payload, "description"));
        assessment.setAssessmentType(text(payload, "assessmentType"));
        assessment.setVisibility(text(payload, "visibility"));
        assessment.setTimeLimitMin(intValue(payload, "timeLimitMin"));
        assessment.setAttemptsAllowed(intValue(payload, "attemptsAllowed"));
        assessment.setMaxScore(doubleValue(payload, "maxScore"));
        assessment.setWeightPct(doubleValue(payload, "weightPct"));
        if (payload.has("resourceId")) {
            assessment.setResource(reference(zw.co.zivai.core_backend.common.models.lms.resources.Resource.class, uuid(payload, "resourceId")));
        }
        assessment.setAiEnhanced(bool(payload, "aiEnhanced", false));
        assessment.setStatus(text(payload, "status"));
        assessment.setCreatedBy(reference(User.class, uuid(payload, "createdById")));
        assessment.setLastModifiedBy(reference(User.class, uuid(payload, "lastModifiedById")));
        return assessment;
    }

    private AssessmentAssignment hydrateAssessmentAssignment(JsonNode payload, AssessmentAssignment assignment) {
        applyBaseFields(assignment, payload);
        assignment.setAssessment(reference(Assessment.class, uuid(payload, "assessmentId")));
        assignment.setClassEntity(reference(zw.co.zivai.core_backend.common.models.lms.classroom.ClassEntity.class, uuid(payload, "classEntityId")));
        assignment.setAssignedBy(reference(User.class, uuid(payload, "assignedById")));
        assignment.setTitle(text(payload, "title"));
        assignment.setInstructions(text(payload, "instructions"));
        assignment.setStartTime(instant(payload, "startTime"));
        assignment.setDueTime(instant(payload, "dueTime"));
        assignment.setPublished(bool(payload, "published", false));
        return assignment;
    }

    private Resource hydrateResource(JsonNode payload, Resource resource) {
        applyBaseFields(resource, payload);
        resource.setSchool(reference(School.class, uuid(payload, "schoolId")));
        resource.setSubject(reference(Subject.class, uuid(payload, "subjectId")));
        resource.setUploadedBy(reference(User.class, uuid(payload, "uploadedById")));
        resource.setName(text(payload, "name"));
        resource.setOriginalName(text(payload, "originalName"));
        resource.setMimeType(text(payload, "mimeType"));
        resource.setResType(text(payload, "resType"));
        resource.setSizeBytes(longValue(payload, "sizeBytes"));
        resource.setUrl(text(payload, "url"));
        resource.setStorageKey(text(payload, "storageKey"));
        resource.setStoragePath(text(payload, "storagePath"));
        resource.setTags(textArray(payload, "tags"));
        resource.setContentType(text(payload, "contentType"));
        resource.setContentBody(text(payload, "contentBody"));
        resource.setPublishAt(instant(payload, "publishAt"));
        resource.setDownloads(intValue(payload, "downloads"));
        resource.setDisplayOrder(intValue(payload, "displayOrder"));
        resource.setStatus(text(payload, "status"));
        return resource;
    }

    private Question hydrateQuestion(JsonNode payload, Question question) {
        applyBaseFields(question, payload);
        question.setSubject(reference(Subject.class, uuid(payload, "subjectId")));
        question.setTopic(reference(Topic.class, uuid(payload, "topicId")));
        question.setAuthor(reference(User.class, uuid(payload, "authorId")));
        question.setCode(text(payload, "code"));
        question.setStem(text(payload, "stem"));
        question.setQuestionTypeCode(text(payload, "questionTypeCode"));
        question.setMaxMark(doubleValue(payload, "maxMark"));
        question.setDifficulty(shortValue(payload, "difficulty"));
        question.setExamStyleCode(text(payload, "examStyleCode"));
        question.setSourceYear(shortValue(payload, "sourceYear"));
        question.setRubricJson(json(payload, "rubricJson"));
        question.setActive(bool(payload, "active", true));
        return question;
    }

    private AssessmentQuestion hydrateAssessmentQuestion(JsonNode payload, AssessmentQuestion assessmentQuestion) {
        applyBaseFields(assessmentQuestion, payload);
        assessmentQuestion.setAssessment(reference(Assessment.class, uuid(payload, "assessmentId")));
        assessmentQuestion.setQuestion(reference(Question.class, uuid(payload, "questionId")));
        assessmentQuestion.setSequenceIndex(intValue(payload, "sequenceIndex"));
        assessmentQuestion.setPoints(doubleValue(payload, "points"));
        assessmentQuestion.setRubricScheme(reference(MarkingScheme.class, uuid(payload, "rubricSchemeId")));
        assessmentQuestion.setRubricSchemeVersion(intValue(payload, "rubricSchemeVersion"));
        return assessmentQuestion;
    }

    private AssessmentEnrollment hydrateAssessmentEnrollment(JsonNode payload, AssessmentEnrollment assessmentEnrollment) {
        applyBaseFields(assessmentEnrollment, payload);
        assessmentEnrollment.setAssessmentAssignment(reference(AssessmentAssignment.class, uuid(payload, "assessmentAssignmentId")));
        assessmentEnrollment.setStudent(reference(User.class, uuid(payload, "studentId")));
        assessmentEnrollment.setStatusCode(text(payload, "statusCode"));
        return assessmentEnrollment;
    }

    private AssessmentAttempt hydrateAssessmentAttempt(JsonNode payload, AssessmentAttempt assessmentAttempt) {
        applyBaseFields(assessmentAttempt, payload);
        assessmentAttempt.setAssessmentEnrollment(reference(AssessmentEnrollment.class, uuid(payload, "assessmentEnrollmentId")));
        assessmentAttempt.setAttemptNumber(intValue(payload, "attemptNumber"));
        assessmentAttempt.setStartedAt(instant(payload, "startedAt"));
        assessmentAttempt.setSubmittedAt(instant(payload, "submittedAt"));
        assessmentAttempt.setTotalScore(doubleValue(payload, "totalScore"));
        assessmentAttempt.setMaxScore(doubleValue(payload, "maxScore"));
        assessmentAttempt.setFinalScore(doubleValue(payload, "finalScore"));
        assessmentAttempt.setFinalGrade(text(payload, "finalGrade"));
        assessmentAttempt.setSubmissionType(text(payload, "submissionType"));
        assessmentAttempt.setOriginalFilename(text(payload, "originalFilename"));
        assessmentAttempt.setFileType(text(payload, "fileType"));
        assessmentAttempt.setFileSizeBytes(longValue(payload, "fileSizeBytes"));
        assessmentAttempt.setStoragePath(text(payload, "storagePath"));
        assessmentAttempt.setGradingStatusCode(text(payload, "gradingStatusCode"));
        assessmentAttempt.setAiConfidence(doubleValue(payload, "aiConfidence"));
        assessmentAttempt.setAttemptTraceId(text(payload, "attemptTraceId"));
        return assessmentAttempt;
    }

    private AttemptAnswer hydrateAttemptAnswer(JsonNode payload, AttemptAnswer attemptAnswer) {
        applyBaseFields(attemptAnswer, payload);
        attemptAnswer.setAssessmentAttempt(reference(AssessmentAttempt.class, uuid(payload, "assessmentAttemptId")));
        attemptAnswer.setAssessmentQuestion(reference(AssessmentQuestion.class, uuid(payload, "assessmentQuestionId")));
        attemptAnswer.setStudentAnswerText(text(payload, "studentAnswerText"));
        attemptAnswer.setStudentAnswerBlob(json(payload, "studentAnswerBlob"));
        attemptAnswer.setSubmissionType(text(payload, "submissionType"));
        attemptAnswer.setTextContent(text(payload, "textContent"));
        attemptAnswer.setExternalAssessmentData(json(payload, "externalAssessmentData"));
        attemptAnswer.setHandwritingResource(reference(Resource.class, uuid(payload, "handwritingResourceId")));
        attemptAnswer.setOcrText(text(payload, "ocrText"));
        attemptAnswer.setOcrConfidence(doubleValue(payload, "ocrConfidence"));
        attemptAnswer.setOcrEngine(text(payload, "ocrEngine"));
        attemptAnswer.setOcrLanguage(text(payload, "ocrLanguage"));
        attemptAnswer.setOcrMetadata(json(payload, "ocrMetadata"));
        attemptAnswer.setAiScore(doubleValue(payload, "aiScore"));
        attemptAnswer.setHumanScore(doubleValue(payload, "humanScore"));
        attemptAnswer.setMaxScore(doubleValue(payload, "maxScore"));
        attemptAnswer.setAiConfidence(doubleValue(payload, "aiConfidence"));
        attemptAnswer.setRequiresReview(bool(payload, "requiresReview", false));
        attemptAnswer.setFeedbackText(text(payload, "feedbackText"));
        attemptAnswer.setGradedAt(instant(payload, "gradedAt"));
        attemptAnswer.setAnswerTraceId(text(payload, "answerTraceId"));
        return attemptAnswer;
    }

    private AssessmentResult hydrateAssessmentResult(JsonNode payload, AssessmentResult assessmentResult) {
        applyBaseFields(assessmentResult, payload);
        assessmentResult.setAssessmentAssignment(reference(AssessmentAssignment.class, uuid(payload, "assessmentAssignmentId")));
        assessmentResult.setStudent(reference(User.class, uuid(payload, "studentId")));
        assessmentResult.setFinalizedAttempt(reference(AssessmentAttempt.class, uuid(payload, "finalizedAttemptId")));
        assessmentResult.setExpectedMark(doubleValue(payload, "expectedMark"));
        assessmentResult.setActualMark(doubleValue(payload, "actualMark"));
        assessmentResult.setGrade(text(payload, "grade"));
        assessmentResult.setFeedback(text(payload, "feedback"));
        assessmentResult.setSubmittedAt(instant(payload, "submittedAt"));
        assessmentResult.setGradedAt(instant(payload, "gradedAt"));
        assessmentResult.setStatus(text(payload, "status"));
        return assessmentResult;
    }

    private TopicResource hydrateTopicResource(JsonNode payload, TopicResource topicResource) {
        applyBaseFields(topicResource, payload);
        topicResource.setTopic(reference(Topic.class, uuid(payload, "topicId")));
        topicResource.setResource(reference(Resource.class, uuid(payload, "resourceId")));
        topicResource.setDisplayOrder(intValue(payload, "displayOrder"));
        return topicResource;
    }

    private void applyBaseFields(BaseEntity entity, JsonNode payload) {
        entity.setId(uuid(payload, "id"));
        entity.setOriginNodeId(uuid(payload, "originNodeId"));
        if (payload.has("syncVersion") && !payload.get("syncVersion").isNull()) {
            entity.setSyncVersion(payload.get("syncVersion").asLong());
        }
        entity.setDeletedAt(instant(payload, "deletedAt"));
        entity.setCreatedAt(instant(payload, "createdAt"));
        entity.setUpdatedAt(instant(payload, "updatedAt"));
    }

    private ObjectNode baseNode(BaseEntity entity) {
        ObjectNode node = objectMapper.createObjectNode();
        putRef(node, "id", entity.getId());
        putRef(node, "originNodeId", entity.getOriginNodeId());
        if (entity.getSyncVersion() != null) {
            node.put("syncVersion", entity.getSyncVersion());
        } else {
            node.putNull("syncVersion");
        }
        putInstant(node, "deletedAt", entity.getDeletedAt());
        putInstant(node, "createdAt", entity.getCreatedAt());
        putInstant(node, "updatedAt", entity.getUpdatedAt());
        node.put("_aggregateType", aggregateTypeOf(entity));
        return node;
    }

    private <T> T reference(Class<T> type, UUID id) {
        if (id == null) {
            return null;
        }
        return entityManager.getReference(type, id);
    }

    private UUID uuid(JsonNode payload, String field) {
        if (!payload.has(field) || payload.get(field).isNull() || payload.get(field).asText().isBlank()) {
            return null;
        }
        return objectMapper.convertValue(payload.get(field), UUID.class);
    }

    private String text(JsonNode payload, String field) {
        if (!payload.has(field) || payload.get(field).isNull()) {
            return null;
        }
        return payload.get(field).asText();
    }

    private Instant instant(JsonNode payload, String field) {
        if (!payload.has(field) || payload.get(field).isNull() || payload.get(field).asText().isBlank()) {
            return null;
        }
        return objectMapper.convertValue(payload.get(field), Instant.class);
    }

    private Double doubleValue(JsonNode payload, String field) {
        if (!payload.has(field) || payload.get(field).isNull()) {
            return null;
        }
        return payload.get(field).asDouble();
    }

    private Integer intValue(JsonNode payload, String field) {
        if (!payload.has(field) || payload.get(field).isNull()) {
            return null;
        }
        return payload.get(field).asInt();
    }

    private Long longValue(JsonNode payload, String field) {
        if (!payload.has(field) || payload.get(field).isNull()) {
            return null;
        }
        return payload.get(field).asLong();
    }

    private Short shortValue(JsonNode payload, String field) {
        if (!payload.has(field) || payload.get(field).isNull()) {
            return null;
        }
        return (short) payload.get(field).asInt();
    }

    private JsonNode json(JsonNode payload, String field) {
        if (!payload.has(field) || payload.get(field).isNull()) {
            return null;
        }
        return payload.get(field).deepCopy();
    }

    private String[] textArray(JsonNode payload, String field) {
        if (!payload.has(field) || payload.get(field).isNull() || !payload.get(field).isArray()) {
            return null;
        }
        JsonNode arrayNode = payload.get(field);
        String[] values = new String[arrayNode.size()];
        for (int i = 0; i < arrayNode.size(); i++) {
            values[i] = arrayNode.get(i).isNull() ? null : arrayNode.get(i).asText();
        }
        return values;
    }

    private boolean bool(JsonNode payload, String field, boolean defaultValue) {
        if (!payload.has(field) || payload.get(field).isNull()) {
            return defaultValue;
        }
        return payload.get(field).asBoolean();
    }

    private void putRef(ObjectNode node, String field, UUID value) {
        if (value == null) {
            node.putNull(field);
        } else {
            node.put(field, value.toString());
        }
    }

    private void putNullable(ObjectNode node, String field, String value) {
        if (value == null) {
            node.putNull(field);
        } else {
            node.put(field, value);
        }
    }

    private void putInstant(ObjectNode node, String field, Instant value) {
        if (value == null) {
            node.putNull(field);
        } else {
            node.put(field, value.toString());
        }
    }

    private void putJson(ObjectNode node, String field, JsonNode value) {
        if (value == null) {
            node.putNull(field);
        } else {
            node.set(field, value.deepCopy());
        }
    }

    private void putStringArray(ObjectNode node, String field, String[] values) {
        if (values == null) {
            node.putNull(field);
            return;
        }
        var arrayNode = node.putArray(field);
        for (String value : values) {
            if (value == null) {
                arrayNode.addNull();
            } else {
                arrayNode.add(value);
            }
        }
    }

    private void putNumber(ObjectNode node, String field, Number value) {
        if (value == null) {
            node.putNull(field);
        } else if (value instanceof Integer integer) {
            node.put(field, integer);
        } else if (value instanceof Long longValue) {
            node.put(field, longValue);
        } else if (value instanceof Double doubleValue) {
            node.put(field, doubleValue);
        } else if (value instanceof Short shortValue) {
            node.put(field, shortValue.intValue());
        } else {
            node.put(field, value.doubleValue());
        }
    }
}
