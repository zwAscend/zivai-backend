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
import zw.co.zivai.core_backend.common.models.lms.development.Plan;
import zw.co.zivai.core_backend.common.models.lms.development.PlanStep;
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
        AssessmentAssignment.class
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

    private void putNumber(ObjectNode node, String field, Number value) {
        if (value == null) {
            node.putNull(field);
        } else if (value instanceof Integer integer) {
            node.put(field, integer);
        } else if (value instanceof Long longValue) {
            node.put(field, longValue);
        } else if (value instanceof Double doubleValue) {
            node.put(field, doubleValue);
        } else {
            node.put(field, value.doubleValue());
        }
    }
}
