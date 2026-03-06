package zw.co.zivai.core_backend.common.services.development;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import zw.co.zivai.core_backend.common.dtos.development.AssignPlanRequest;
import zw.co.zivai.core_backend.common.dtos.development.CreatePlanRequest;
import zw.co.zivai.core_backend.common.dtos.development.DevelopmentAttributeDto;
import zw.co.zivai.core_backend.common.dtos.development.DevelopmentPlanDto;
import zw.co.zivai.core_backend.common.dtos.development.MasterySignalsSummaryDto;
import zw.co.zivai.core_backend.common.dtos.common.PageResponse;
import zw.co.zivai.core_backend.common.dtos.development.PlanDto;
import zw.co.zivai.core_backend.common.dtos.development.ReorderStudentPlanStepsRequest;
import zw.co.zivai.core_backend.common.dtos.development.StudentPlanStepRequest;
import zw.co.zivai.core_backend.common.dtos.development.StudentStreakDto;
import zw.co.zivai.core_backend.common.dtos.development.StudentAttributeUpdateRequest;
import zw.co.zivai.core_backend.common.dtos.development.UpdatePlanProgressRequest;
import zw.co.zivai.core_backend.common.dtos.development.UpdatePlanRequest;
import zw.co.zivai.core_backend.common.dtos.development.UpdateStudentPlanRequest;
import zw.co.zivai.core_backend.common.exceptions.BadRequestException;
import zw.co.zivai.core_backend.common.exceptions.NotFoundException;
import zw.co.zivai.core_backend.common.models.lms.development.Plan;
import zw.co.zivai.core_backend.common.models.lms.development.PlanSkill;
import zw.co.zivai.core_backend.common.models.lms.development.PlanStep;
import zw.co.zivai.core_backend.common.models.lms.development.PlanSubskill;
import zw.co.zivai.core_backend.common.models.lms.development.Skill;
import zw.co.zivai.core_backend.common.models.lms.classroom.ClassEntity;
import zw.co.zivai.core_backend.common.models.lms.students.Enrolment;
import zw.co.zivai.core_backend.common.models.lms.students.StudentAttribute;
import zw.co.zivai.core_backend.common.models.lms.students.StudentActivityDay;
import zw.co.zivai.core_backend.common.models.lms.students.StudentPlan;
import zw.co.zivai.core_backend.common.models.lms.students.StudentSubjectEnrolment;
import zw.co.zivai.core_backend.common.models.lms.subjects.Subject;
import zw.co.zivai.core_backend.common.models.lms.users.User;
import zw.co.zivai.core_backend.common.repositories.development.PlanRepository;
import zw.co.zivai.core_backend.common.repositories.development.PlanSkillRepository;
import zw.co.zivai.core_backend.common.repositories.development.PlanStepRepository;
import zw.co.zivai.core_backend.common.repositories.development.PlanSubskillRepository;
import zw.co.zivai.core_backend.common.repositories.subject.SkillRepository;
import zw.co.zivai.core_backend.common.repositories.development.StudentAttributeRepository;
import zw.co.zivai.core_backend.common.repositories.development.StudentActivityDayRepository;
import zw.co.zivai.core_backend.common.repositories.development.StudentPlanRepository;
import zw.co.zivai.core_backend.common.repositories.classroom.StudentSubjectEnrolmentRepository;
import zw.co.zivai.core_backend.common.repositories.subject.SubjectRepository;
import zw.co.zivai.core_backend.common.repositories.classroom.EnrolmentRepository;
import zw.co.zivai.core_backend.common.repositories.user.UserRepository;
import zw.co.zivai.core_backend.common.services.notification.NotificationService;
import zw.co.zivai.core_backend.common.services.students.StudentService;

@Service
@Slf4j
@RequiredArgsConstructor
public class DevelopmentService {
    private final SkillRepository skillRepository;
    private final StudentAttributeRepository studentAttributeRepository;
    private final PlanRepository planRepository;
    private final PlanStepRepository planStepRepository;
    private final PlanSkillRepository planSkillRepository;
    private final PlanSubskillRepository planSubskillRepository;
    private final StudentPlanRepository studentPlanRepository;
    private final StudentActivityDayRepository studentActivityDayRepository;
    private final StudentSubjectEnrolmentRepository studentSubjectEnrolmentRepository;
    private final SubjectRepository subjectRepository;
    private final EnrolmentRepository enrolmentRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final StudentService studentService;

    public List<DevelopmentAttributeDto> getSubjectAttributes(UUID subjectId) {
        return skillRepository.findBySubject_Id(subjectId).stream()
            .map(skill -> DevelopmentAttributeDto.builder()
                .id(skill.getId().toString())
                .name(skill.getName())
                .description(skill.getDescription())
                .category("skill")
                .subjectId(subjectId.toString())
                .attributeId(skill.getCode())
                .build())
            .toList();
    }

    public Map<String, Object> getStudentAttributes(UUID studentId, UUID subjectId) {
        List<StudentAttribute> attributes = studentAttributeRepository.findByStudent_IdAndSkill_Subject_Id(studentId, subjectId);
        Map<String, Object> payload = new HashMap<>();
        for (StudentAttribute attribute : attributes) {
            String key = attribute.getSkill().getName();
            Map<String, Object> values = new HashMap<>();
            values.put("current", attribute.getCurrentScore());
            values.put("potential", attribute.getPotentialScore());
            values.put("lastAssessed", attribute.getLastAssessed());
            payload.put(key, values);
        }
        return payload;
    }

    @Transactional
    public StudentPlan ensureStarterPlanForStudentSubject(UUID studentId, UUID subjectId) {
        if (studentId == null || subjectId == null) {
            throw new BadRequestException("Student and subject are required");
        }

        List<StudentPlan> existingPlans = studentPlanRepository
            .findByStudent_IdAndSubject_IdAndDeletedAtIsNullOrderByCreatedAtDesc(studentId, subjectId);
        if (!existingPlans.isEmpty()) {
            return existingPlans.get(0);
        }

        User student = userRepository.findById(studentId)
            .orElseThrow(() -> new NotFoundException("Student not found: " + studentId));
        Subject subject = subjectRepository.findById(subjectId)
            .orElseThrow(() -> new NotFoundException("Subject not found: " + subjectId));
        return createStarterStudentPlan(student, subject);
    }

    public void updateStudentAttributes(UUID studentId, List<StudentAttributeUpdateRequest> updates) {
        if (updates == null) {
            return;
        }
        User student = userRepository.findById(studentId)
            .orElseThrow(() -> new NotFoundException("Student not found: " + studentId));

        for (StudentAttributeUpdateRequest update : updates) {
            if (update.getAttributeId() == null || update.getAttributeId().isBlank()) {
                continue;
            }
            UUID skillId;
            try {
                skillId = UUID.fromString(update.getAttributeId());
            } catch (IllegalArgumentException ex) {
                throw new BadRequestException("Invalid attributeId: " + update.getAttributeId());
            }
            Skill skill = skillRepository.findById(skillId)
                .orElseThrow(() -> new NotFoundException("Skill not found: " + update.getAttributeId()));

            StudentAttribute attribute = studentAttributeRepository
                .findByStudent_IdAndSkill_Id(studentId, skillId)
                .orElseGet(() -> {
                    StudentAttribute created = new StudentAttribute();
                    created.setStudent(student);
                    created.setSkill(skill);
                    return created;
                });

            if (update.getCurrent() != null) {
                attribute.setCurrentScore(update.getCurrent());
            }
            if (update.getPotential() != null) {
                attribute.setPotentialScore(update.getPotential());
            }
            attribute.setLastAssessed(Instant.now());
            studentAttributeRepository.save(attribute);
        }
    }

    public DevelopmentAttributeDto createSubjectAttribute(DevelopmentAttributeDto request) {
        if (request == null || request.getSubjectId() == null) {
            throw new BadRequestException("subjectId is required");
        }
        UUID subjectId = UUID.fromString(request.getSubjectId());
        Subject subject = subjectRepository.findById(subjectId)
            .orElseThrow(() -> new NotFoundException("Subject not found: " + subjectId));

        String code = request.getAttributeId();
        if (code == null || code.isBlank()) {
            code = request.getName() == null ? "ATTR" : request.getName().trim().replaceAll("\\s+", "_").toUpperCase(Locale.ROOT);
        }

        Skill skill = new Skill();
        skill.setSubject(subject);
        skill.setCode(code);
        skill.setName(request.getName() == null ? "Attribute" : request.getName());
        skill.setDescription(request.getDescription());
        Skill saved = skillRepository.save(skill);

        return DevelopmentAttributeDto.builder()
            .id(saved.getId().toString())
            .name(saved.getName())
            .description(saved.getDescription())
            .category("skill")
            .subjectId(subjectId.toString())
            .attributeId(saved.getCode())
            .build();
    }

    public List<PlanDto> getSubjectPlans(UUID subjectId) {
        return planRepository.findBySubject_IdAndDeletedAtIsNull(subjectId).stream()
            .map(this::toPlanDto)
            .toList();
    }

    public PlanDto createSubjectPlan(CreatePlanRequest request) {
        if (request == null || request.getSubjectId() == null || request.getSubjectId().isBlank()) {
            throw new BadRequestException("subjectId is required");
        }
        Subject subject = subjectRepository.findById(UUID.fromString(request.getSubjectId()))
            .orElseThrow(() -> new NotFoundException("Subject not found: " + request.getSubjectId()));

        Plan plan = new Plan();
        plan.setSubject(subject);
        plan.setName(request.getName() == null ? "Development Plan" : request.getName());
        plan.setDescription(request.getDescription() == null ? "" : request.getDescription());
        plan.setProgress(request.getProgress() == null ? 0.0 : request.getProgress());
        plan.setPotentialOverall(request.getPotentialOverall() == null ? 0.0 : request.getPotentialOverall());
        plan.setEtaDays(request.getEta() == null ? 30 : request.getEta());
        plan.setPerformance(request.getPerformance() == null ? "Average" : request.getPerformance());
        Plan savedPlan = planRepository.save(plan);

        if (request.getSteps() != null) {
            int index = 0;
            for (CreatePlanRequest.PlanStepRequest stepRequest : request.getSteps()) {
                PlanStep step = new PlanStep();
                step.setPlan(savedPlan);
                step.setTitle(stepRequest.getTitle() == null ? "Step" : stepRequest.getTitle());
                step.setStepType(normalizePlanStepType(stepRequest.getType()));
                step.setContent(stepRequest.getContent());
                step.setLink(stepRequest.getLink());
                int order = stepRequest.getOrder() != null ? stepRequest.getOrder() : index + 1;
                step.setStepOrder(order);
                planStepRepository.save(step);
                index++;
            }
        }

        if (request.getSkills() != null) {
            for (CreatePlanRequest.PlanSkillRequest skillRequest : request.getSkills()) {
                PlanSkill planSkill = new PlanSkill();
                planSkill.setPlan(savedPlan);
                planSkill.setName(skillRequest.getName() == null ? "Skill" : skillRequest.getName());
                planSkill.setScore(skillRequest.getScore());
                PlanSkill savedSkill = planSkillRepository.save(planSkill);

                if (skillRequest.getSubskills() != null) {
                    for (CreatePlanRequest.PlanSubskillRequest subskillRequest : skillRequest.getSubskills()) {
                        PlanSubskill subskill = new PlanSubskill();
                        subskill.setPlanSkill(savedSkill);
                        subskill.setName(subskillRequest.getName() == null ? "Subskill" : subskillRequest.getName());
                        subskill.setScore(subskillRequest.getScore());
                        if (subskillRequest.getColor() != null) {
                            subskill.setColor(subskillRequest.getColor());
                        }
                        planSubskillRepository.save(subskill);
                    }
                }
            }
        }

        return toPlanDto(savedPlan);
    }

    public PlanDto updateSubjectPlan(UUID planId, UpdatePlanRequest request) {
        Plan plan = planRepository.findByIdAndDeletedAtIsNull(planId)
            .orElseThrow(() -> new NotFoundException("Plan not found: " + planId));
        if (request == null) {
            return toPlanDto(plan);
        }

        if (request.getSubjectId() != null && !request.getSubjectId().isBlank()) {
            Subject subject = subjectRepository.findById(UUID.fromString(request.getSubjectId()))
                .orElseThrow(() -> new NotFoundException("Subject not found: " + request.getSubjectId()));
            plan.setSubject(subject);
        }
        if (request.getName() != null) {
            plan.setName(request.getName());
        }
        if (request.getDescription() != null) {
            plan.setDescription(request.getDescription());
        }
        if (request.getProgress() != null) {
            plan.setProgress(clampProgress(request.getProgress()));
        }
        if (request.getPotentialOverall() != null) {
            plan.setPotentialOverall(clampProgress(request.getPotentialOverall()));
        }
        if (request.getEta() != null) {
            plan.setEtaDays(Math.max(0, request.getEta()));
        }
        if (request.getPerformance() != null) {
            plan.setPerformance(request.getPerformance());
        }
        planRepository.save(plan);

        if (request.getSteps() != null) {
            replacePlanStepsFromCreate(plan, request.getSteps());
        }
        if (request.getSkills() != null) {
            replacePlanSkillsFromCreate(plan, request.getSkills());
        }

        return toPlanDto(plan);
    }

    public void deleteSubjectPlan(UUID planId) {
        Plan plan = planRepository.findByIdAndDeletedAtIsNull(planId)
            .orElseThrow(() -> new NotFoundException("Plan not found: " + planId));
        plan.setDeletedAt(Instant.now());
        planRepository.save(plan);
    }

    public List<DevelopmentPlanDto> getStudentPlans(UUID studentId, String status) {
        ensureStarterPlansForEnrolments(
            studentSubjectEnrolmentRepository.findByStudent_IdAndDeletedAtIsNull(studentId),
            null
        );
        List<StudentPlan> plans = studentPlanRepository.findByStudent_IdAndDeletedAtIsNullOrderByCreatedAtDesc(studentId);
        if (status != null && !status.isBlank()) {
            String normalized = normalizeStatus(status);
            plans = plans.stream()
                .filter(plan -> normalized.equalsIgnoreCase(normalizeStatus(plan.getStatus())))
                .toList();
        }
        return toDevelopmentPlanDtos(plans);
    }

    public PageResponse<DevelopmentPlanDto> listStudentPlans(UUID subjectId,
                                                             UUID classId,
                                                             UUID classSubjectId,
                                                             String status,
                                                             int page,
                                                             int size) {
        int safePage = Math.max(page, 0);
        int safeSize = size <= 0 ? 20 : Math.min(size, 200);
        PageRequest pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<StudentPlan> resultPage;
        String normalizedStatus = status != null && !status.isBlank() ? normalizeStatus(status) : null;
        List<StudentSubjectEnrolment> enrolmentsToEnsure;

        if (classSubjectId != null) {
            enrolmentsToEnsure = studentSubjectEnrolmentRepository.findByClassSubject_IdAndDeletedAtIsNull(classSubjectId);
            ensureStarterPlansForEnrolments(enrolmentsToEnsure, subjectId);
            List<UUID> studentIds = enrolmentsToEnsure.stream()
                .map(StudentSubjectEnrolment::getStudent)
                .filter(student -> student != null)
                .map(User::getId)
                .distinct()
                .toList();
            if (studentIds.isEmpty()) {
                return emptyPage(safePage, safeSize);
            }
            resultPage = subjectId == null
                ? (normalizedStatus == null
                    ? studentPlanRepository.findByStudent_IdInAndDeletedAtIsNull(studentIds, pageable)
                    : studentPlanRepository.findByStudent_IdInAndStatusIgnoreCaseAndDeletedAtIsNull(studentIds, normalizedStatus, pageable))
                : (normalizedStatus == null
                    ? studentPlanRepository.findByStudent_IdInAndSubject_IdAndDeletedAtIsNull(studentIds, subjectId, pageable)
                    : studentPlanRepository.findByStudent_IdInAndSubject_IdAndStatusIgnoreCaseAndDeletedAtIsNull(studentIds, subjectId, normalizedStatus, pageable));
        } else if (classId != null) {
            List<UUID> studentIds = enrolmentRepository.findByClassEntity_Id(classId).stream()
                .map(enrolment -> enrolment.getStudent())
                .filter(student -> student != null)
                .map(User::getId)
                .distinct()
                .toList();
            if (studentIds.isEmpty()) {
                return emptyPage(safePage, safeSize);
            }
            enrolmentsToEnsure = studentSubjectEnrolmentRepository.findByStudent_IdInAndDeletedAtIsNull(studentIds);
            ensureStarterPlansForEnrolments(enrolmentsToEnsure, subjectId);
            resultPage = subjectId == null
                ? (normalizedStatus == null
                    ? studentPlanRepository.findByStudent_IdInAndDeletedAtIsNull(studentIds, pageable)
                    : studentPlanRepository.findByStudent_IdInAndStatusIgnoreCaseAndDeletedAtIsNull(studentIds, normalizedStatus, pageable))
                : (normalizedStatus == null
                    ? studentPlanRepository.findByStudent_IdInAndSubject_IdAndDeletedAtIsNull(studentIds, subjectId, pageable)
                    : studentPlanRepository.findByStudent_IdInAndSubject_IdAndStatusIgnoreCaseAndDeletedAtIsNull(studentIds, subjectId, normalizedStatus, pageable));
        } else if (subjectId != null) {
            ensureStarterPlansForEnrolments(
                studentSubjectEnrolmentRepository.findByClassSubject_Subject_IdAndDeletedAtIsNull(subjectId),
                subjectId
            );
            resultPage = normalizedStatus == null
                ? studentPlanRepository.findBySubject_IdAndDeletedAtIsNull(subjectId, pageable)
                : studentPlanRepository.findBySubject_IdAndStatusIgnoreCaseAndDeletedAtIsNull(subjectId, normalizedStatus, pageable);
        } else {
            ensureStarterPlansForEnrolments(studentSubjectEnrolmentRepository.findByDeletedAtIsNull(), null);
            resultPage = normalizedStatus == null
                ? studentPlanRepository.findByDeletedAtIsNull(pageable)
                : studentPlanRepository.findByStatusIgnoreCaseAndDeletedAtIsNull(normalizedStatus, pageable);
        }

        List<DevelopmentPlanDto> items = toDevelopmentPlanDtos(resultPage.getContent());

        return PageResponse.<DevelopmentPlanDto>builder()
            .items(items)
            .page(resultPage.getNumber())
            .size(resultPage.getSize())
            .totalItems(resultPage.getTotalElements())
            .totalPages(resultPage.getTotalPages())
            .build();
    }

    public DevelopmentPlanDto getStudentPlan(UUID studentId, UUID subjectId) {
        ensureStarterPlansForEnrolments(
            studentSubjectEnrolmentRepository.findByStudent_IdAndDeletedAtIsNull(studentId),
            subjectId
        );
        List<StudentPlan> plans = studentPlanRepository.findByStudent_IdAndSubject_IdAndDeletedAtIsNullOrderByCreatedAtDesc(studentId, subjectId);
        StudentPlan plan = plans.stream()
            .filter(StudentPlan::isCurrent)
            .findFirst()
            .orElseGet(() -> plans.stream()
                .findFirst()
                .orElseThrow(() -> new NotFoundException("No plan found for student and subject")));
        return toDevelopmentPlanDtos(List.of(plan)).stream()
            .findFirst()
            .orElseThrow(() -> new NotFoundException("No plan found for student and subject"));
    }

    public DevelopmentPlanDto assignPlan(UUID studentId, AssignPlanRequest request) {
        if (request == null || request.getPlanId() == null || request.getPlanId().isBlank()) {
            throw new BadRequestException("planId is required");
        }
        User student = userRepository.findById(studentId)
            .orElseThrow(() -> new NotFoundException("Student not found: " + studentId));
        Plan plan = planRepository.findById(UUID.fromString(request.getPlanId()))
            .orElseThrow(() -> new NotFoundException("Plan not found: " + request.getPlanId()));

        Subject subject = plan.getSubject();
        if (request.getSubjectId() != null && !request.getSubjectId().isBlank()) {
            subject = subjectRepository.findById(UUID.fromString(request.getSubjectId()))
                .orElseThrow(() -> new NotFoundException("Subject not found: " + request.getSubjectId()));
        }

        studentPlanRepository.findByStudent_IdAndSubject_Id(studentId, subject.getId())
            .forEach(existing -> {
                if (existing.isCurrent() && existing.getDeletedAt() == null) {
                    existing.setCurrent(false);
                    studentPlanRepository.save(existing);
                }
            });

        StudentPlan studentPlan = studentPlanRepository.findByStudent_IdAndPlan_Id(studentId, plan.getId())
            .orElseGet(StudentPlan::new);
        studentPlan.setStudent(student);
        studentPlan.setPlan(plan);
        studentPlan.setSubject(subject);
        studentPlan.setStartDate(Instant.now());
        studentPlan.setCurrentProgress(0.0);
        studentPlan.setStatus("active");
        studentPlan.setCurrent(true);
        studentPlan.setDeletedAt(null);
        StudentPlan saved = studentPlanRepository.save(studentPlan);
        notifyPlanAssigned(saved);
        return toDevelopmentPlanDtos(List.of(saved)).stream()
            .findFirst()
            .orElseThrow(() -> new NotFoundException("Student plan not found"));
    }

    public DevelopmentPlanDto updatePlanProgress(UUID studentId, UUID planId, UpdatePlanProgressRequest request) {
        StudentPlan studentPlan = studentPlanRepository.findByStudent_IdAndPlan_Id(studentId, planId)
            .orElseThrow(() -> new NotFoundException("Student plan not found"));
        if (studentPlan.getDeletedAt() != null) {
            throw new NotFoundException("Student plan not found");
        }
        applyProgressUpdate(studentPlan, request);
        StudentPlan saved = studentPlanRepository.save(studentPlan);
        return toDevelopmentPlanDtos(List.of(saved)).stream()
            .findFirst()
            .orElseThrow(() -> new NotFoundException("Student plan not found"));
    }

    public DevelopmentPlanDto updatePlanProgressByStudentPlanId(UUID studentPlanId, Map<String, Object> request) {
        StudentPlan studentPlan = studentPlanRepository.findById(studentPlanId)
            .orElseThrow(() -> new NotFoundException("Student plan not found: " + studentPlanId));
        if (studentPlan.getDeletedAt() != null) {
            throw new NotFoundException("Student plan not found: " + studentPlanId);
        }

        UpdatePlanProgressRequest mapped = new UpdatePlanProgressRequest();
        if (request != null) {
            if (request.containsKey("progress")) {
                mapped.setCurrentProgress(asDouble(request.get("progress")));
            }
            if (request.containsKey("status")) {
                mapped.setStatus(String.valueOf(request.get("status")));
            }
            if (request.containsKey("skillProgress") && request.get("skillProgress") instanceof Map<?, ?> skillMap) {
                List<UpdatePlanProgressRequest.SkillProgressRequest> progressList = new ArrayList<>();
                for (Map.Entry<?, ?> entry : skillMap.entrySet()) {
                    UpdatePlanProgressRequest.SkillProgressRequest item = new UpdatePlanProgressRequest.SkillProgressRequest();
                    item.setSkill(String.valueOf(entry.getKey()));
                    item.setCurrentScore(asDouble(entry.getValue()));
                    progressList.add(item);
                }
                mapped.setSkillProgress(progressList);
            }
        }

        applyProgressUpdate(studentPlan, mapped);
        StudentPlan saved = studentPlanRepository.save(studentPlan);
        return toDevelopmentPlanDtos(List.of(saved)).stream()
            .findFirst()
            .orElseThrow(() -> new NotFoundException("Student plan not found: " + studentPlanId));
    }

    public DevelopmentPlanDto getStudentPlanById(UUID studentPlanId) {
        StudentPlan plan = studentPlanRepository.findById(studentPlanId)
            .orElseThrow(() -> new NotFoundException("Student plan not found: " + studentPlanId));
        if (plan.getDeletedAt() != null) {
            throw new NotFoundException("Student plan not found: " + studentPlanId);
        }
        return toDevelopmentPlanDtos(List.of(plan)).stream()
            .findFirst()
            .orElseThrow(() -> new NotFoundException("Student plan not found: " + studentPlanId));
    }

    public DevelopmentPlanDto updateStudentPlan(UUID studentPlanId, UpdateStudentPlanRequest request) {
        StudentPlan studentPlan = studentPlanRepository.findById(studentPlanId)
            .orElseThrow(() -> new NotFoundException("Student plan not found: " + studentPlanId));
        if (studentPlan.getDeletedAt() != null) {
            throw new NotFoundException("Student plan not found: " + studentPlanId);
        }
        if (request == null) {
            return toDevelopmentPlanDtos(List.of(studentPlan)).stream()
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Student plan not found: " + studentPlanId));
        }

        Plan plan = studentPlan.getPlan();
        if (request.getPlanId() != null && !request.getPlanId().isBlank()) {
            plan = planRepository.findById(UUID.fromString(request.getPlanId()))
                .orElseThrow(() -> new NotFoundException("Plan not found: " + request.getPlanId()));
            studentPlan.setPlan(plan);
        }

        if (request.getSubjectId() != null && !request.getSubjectId().isBlank()) {
            Subject subject = subjectRepository.findById(UUID.fromString(request.getSubjectId()))
                .orElseThrow(() -> new NotFoundException("Subject not found: " + request.getSubjectId()));
            if (plan != null && plan.getSubject() != null && !plan.getSubject().getId().equals(subject.getId())) {
                throw new BadRequestException("Plan does not belong to selected subject");
            }
            studentPlan.setSubject(subject);
        } else if (plan != null && plan.getSubject() != null) {
            studentPlan.setSubject(plan.getSubject());
        }

        if (plan == null) {
            throw new BadRequestException("Student plan has no plan template attached");
        }

        if (request.getCurrentProgress() != null) {
            studentPlan.setCurrentProgress(clampProgress(request.getCurrentProgress()));
        }
        if (request.getStatus() != null) {
            studentPlan.setStatus(normalizeStatus(request.getStatus()));
        }
        if (request.getStartDate() != null) {
            studentPlan.setStartDate(request.getStartDate());
        }
        if (request.getCompletionDate() != null) {
            studentPlan.setCompletionDate(request.getCompletionDate());
        } else if ("completed".equalsIgnoreCase(normalizeStatus(studentPlan.getStatus())) && studentPlan.getCompletionDate() == null) {
            studentPlan.setCompletionDate(Instant.now());
        }

        if (request.getCurrent() != null) {
            boolean makeCurrent = request.getCurrent();
            if (makeCurrent) {
                studentPlanRepository.findByStudent_IdAndSubject_Id(studentPlan.getStudent().getId(), studentPlan.getSubject().getId())
                    .forEach(existing -> {
                        if (existing.isCurrent() && existing.getDeletedAt() == null && !existing.getId().equals(studentPlan.getId())) {
                            existing.setCurrent(false);
                            studentPlanRepository.save(existing);
                        }
                    });
            }
            studentPlan.setCurrent(makeCurrent);
        }

        if (hasPlanTemplateUpdates(request)) {
            if (request.getName() != null) {
                plan.setName(request.getName());
            }
            if (request.getDescription() != null) {
                plan.setDescription(request.getDescription());
            }
            if (request.getProgress() != null) {
                plan.setProgress(clampProgress(request.getProgress()));
            }
            if (request.getPotentialOverall() != null) {
                plan.setPotentialOverall(clampProgress(request.getPotentialOverall()));
            }
            if (request.getEta() != null) {
                plan.setEtaDays(Math.max(0, request.getEta()));
            }
            if (request.getPerformance() != null) {
                plan.setPerformance(request.getPerformance());
            }
            planRepository.save(plan);
        }

        if (request.getSteps() != null) {
            replacePlanSteps(plan, request.getSteps());
        }

        if (request.getSkills() != null) {
            replacePlanSkills(plan, request.getSkills());
        }

        StudentPlan savedPlan = studentPlanRepository.save(studentPlan);
        notifyPlanUpdated(savedPlan, "Plan details were updated.");
        return toDevelopmentPlanDtos(List.of(savedPlan)).stream()
            .findFirst()
            .orElseThrow(() -> new NotFoundException("Student plan not found: " + studentPlanId));
    }

    public void deleteStudentPlan(UUID studentPlanId) {
        StudentPlan studentPlan = studentPlanRepository.findById(studentPlanId)
            .orElseThrow(() -> new NotFoundException("Student plan not found: " + studentPlanId));
        if (studentPlan.getDeletedAt() != null) {
            return;
        }
        studentPlan.setDeletedAt(Instant.now());
        studentPlan.setCurrent(false);
        studentPlanRepository.save(studentPlan);
    }

    public DevelopmentPlanDto addStudentPlanStep(UUID studentPlanId, StudentPlanStepRequest request) {
        StudentPlan studentPlan = getStudentPlanEntity(studentPlanId);
        validateStepRequest(request);

        Plan plan = studentPlan.getPlan();
        List<PlanStep> existingSteps = planStepRepository.findByPlan_IdOrderByStepOrderAsc(plan.getId());

        PlanStep step = new PlanStep();
        step.setPlan(plan);
        step.setTitle(request.getTitle().trim());
        step.setStepType(normalizePlanStepType(request.getType()));
        step.setContent(request.getContent());
        step.setLink(request.getLink());
        step.setStepOrder(existingSteps.size() + 10_000);
        PlanStep savedStep = planStepRepository.save(step);

        int targetIndex = request.getOrder() == null
            ? existingSteps.size()
            : Math.min(Math.max(request.getOrder() - 1, 0), existingSteps.size());
        existingSteps.add(targetIndex, savedStep);
        persistStepOrderingSafely(existingSteps);

        notifyPlanUpdated(studentPlan, "A new step was added.");
        return toDevelopmentPlanDtos(List.of(studentPlanRepository.save(studentPlan))).get(0);
    }

    public DevelopmentPlanDto updateStudentPlanStep(UUID studentPlanId, UUID stepId, StudentPlanStepRequest request) {
        StudentPlan studentPlan = getStudentPlanEntity(studentPlanId);
        validateStepRequest(request);

        PlanStep step = planStepRepository.findByIdAndPlan_Id(stepId, studentPlan.getPlan().getId())
            .orElseThrow(() -> new NotFoundException("Plan step not found: " + stepId));
        step.setTitle(request.getTitle().trim());
        step.setStepType(normalizePlanStepType(request.getType()));
        step.setContent(request.getContent());
        step.setLink(request.getLink());
        List<PlanStep> existingSteps = planStepRepository.findByPlan_IdOrderByStepOrderAsc(studentPlan.getPlan().getId());
        existingSteps.removeIf(existing -> existing.getId().equals(step.getId()));
        int targetIndex = request.getOrder() == null
            ? Math.max(step.getStepOrder() - 1, 0)
            : Math.min(Math.max(request.getOrder() - 1, 0), existingSteps.size());
        existingSteps.add(targetIndex, step);
        persistStepOrderingSafely(existingSteps);

        notifyPlanUpdated(studentPlan, "A step was updated.");
        return toDevelopmentPlanDtos(List.of(studentPlanRepository.save(studentPlan))).get(0);
    }

    public DevelopmentPlanDto deleteStudentPlanStep(UUID studentPlanId, UUID stepId) {
        StudentPlan studentPlan = getStudentPlanEntity(studentPlanId);
        PlanStep step = planStepRepository.findByIdAndPlan_Id(stepId, studentPlan.getPlan().getId())
            .orElseThrow(() -> new NotFoundException("Plan step not found: " + stepId));
        planStepRepository.delete(step);

        List<PlanStep> remainingSteps = planStepRepository.findByPlan_IdOrderByStepOrderAsc(studentPlan.getPlan().getId());
        persistStepOrderingSafely(remainingSteps);
        notifyPlanUpdated(studentPlan, "A step was removed.");
        return toDevelopmentPlanDtos(List.of(studentPlanRepository.save(studentPlan))).get(0);
    }

    public DevelopmentPlanDto reorderStudentPlanSteps(UUID studentPlanId, ReorderStudentPlanStepsRequest request) {
        StudentPlan studentPlan = getStudentPlanEntity(studentPlanId);
        if (request == null || request.getStepIds() == null || request.getStepIds().isEmpty()) {
            throw new BadRequestException("stepIds are required");
        }

        Plan plan = studentPlan.getPlan();
        List<PlanStep> existingSteps = planStepRepository.findByPlan_IdOrderByStepOrderAsc(plan.getId());
        Map<UUID, PlanStep> stepsById = existingSteps.stream()
            .collect(Collectors.toMap(PlanStep::getId, step -> step));

        if (request.getStepIds().size() != existingSteps.size()) {
            throw new BadRequestException("stepIds must include every existing step exactly once");
        }

        Set<UUID> seen = new HashSet<>();
        List<PlanStep> reordered = new ArrayList<>();
        for (String stepIdRaw : request.getStepIds()) {
            UUID stepId;
            try {
                stepId = UUID.fromString(stepIdRaw);
            } catch (IllegalArgumentException ex) {
                throw new BadRequestException("Invalid stepId: " + stepIdRaw);
            }
            if (!seen.add(stepId)) {
                throw new BadRequestException("Duplicate stepId in reorder request: " + stepId);
            }
            PlanStep step = stepsById.get(stepId);
            if (step == null) {
                throw new BadRequestException("stepId does not belong to the plan: " + stepId);
            }
            reordered.add(step);
        }

        persistStepOrderingSafely(reordered);
        notifyPlanUpdated(studentPlan, "Plan steps were reordered.");
        return toDevelopmentPlanDtos(List.of(studentPlanRepository.save(studentPlan))).get(0);
    }

    public DevelopmentPlanDto publishStudentPlan(UUID studentPlanId) {
        StudentPlan studentPlan = getStudentPlanEntity(studentPlanId);
        if (studentPlan.getSubject() == null || studentPlan.getSubject().getId() == null) {
            throw new BadRequestException("Student plan subject is required");
        }

        studentPlanRepository.findByStudent_IdAndSubject_Id(
                studentPlan.getStudent().getId(),
                studentPlan.getSubject().getId()
            )
            .forEach(existing -> {
                if (existing.getDeletedAt() == null && !existing.getId().equals(studentPlan.getId()) && existing.isCurrent()) {
                    existing.setCurrent(false);
                    studentPlanRepository.save(existing);
                }
            });

        studentPlan.setCurrent(true);
        if ("on_hold".equalsIgnoreCase(normalizeStatus(studentPlan.getStatus())) || studentPlan.getStatus() == null) {
            studentPlan.setStatus("active");
        }
        StudentPlan savedPlan = studentPlanRepository.save(studentPlan);
        notifyPlanEvent(
            savedPlan,
            "development_plan_published",
            "Development plan published",
            "Your development plan for " + safeSubjectName(savedPlan) + " is now available."
        );
        return toDevelopmentPlanDtos(List.of(savedPlan)).get(0);
    }

    public DevelopmentPlanDto unpublishStudentPlan(UUID studentPlanId) {
        StudentPlan studentPlan = getStudentPlanEntity(studentPlanId);
        studentPlan.setCurrent(false);
        if ("active".equalsIgnoreCase(normalizeStatus(studentPlan.getStatus()))) {
            studentPlan.setStatus("on_hold");
        }
        StudentPlan savedPlan = studentPlanRepository.save(studentPlan);
        notifyPlanEvent(
            savedPlan,
            "development_plan_unpublished",
            "Development plan updated",
            "Your development plan for " + safeSubjectName(savedPlan) + " is no longer currently assigned."
        );
        return toDevelopmentPlanDtos(List.of(savedPlan)).get(0);
    }

    public List<DevelopmentPlanDto> getPublishedStudentPlans(UUID studentId, UUID subjectId) {
        userRepository.findByIdAndDeletedAtIsNull(studentId)
            .orElseThrow(() -> new NotFoundException("Student not found: " + studentId));

        List<StudentPlan> plans = subjectId == null
            ? studentPlanRepository.findByStudent_IdAndCurrentTrueAndDeletedAtIsNullOrderByCreatedAtDesc(studentId)
            : studentPlanRepository.findByStudent_IdAndSubject_IdAndCurrentTrueAndDeletedAtIsNullOrderByCreatedAtDesc(studentId, subjectId);

        return toDevelopmentPlanDtos(plans);
    }

    public MasterySignalsSummaryDto getMasterySignalsSummary(UUID subjectId, UUID classId, UUID classSubjectId) {
        List<zw.co.zivai.core_backend.common.dtos.students.StudentDto> students = studentService.list(subjectId, classId, classSubjectId);
        if (students.isEmpty()) {
            return MasterySignalsSummaryDto.builder()
                .totalStudents(0)
                .excellent(0)
                .good(0)
                .average(0)
                .needsImprovement(0)
                .averageOverall(0.0)
                .build();
        }

        int excellent = 0;
        int good = 0;
        int average = 0;
        int needsImprovement = 0;
        double totalOverall = 0.0;

        for (var student : students) {
            double overall = student.getOverall();
            totalOverall += overall;
            if (overall >= 85.0) {
                excellent++;
            } else if (overall >= 70.0) {
                good++;
            } else if (overall >= 55.0) {
                average++;
            } else {
                needsImprovement++;
            }
        }

        double averageOverall = Math.round((totalOverall / students.size()) * 10.0) / 10.0;
        return MasterySignalsSummaryDto.builder()
            .totalStudents(students.size())
            .excellent(excellent)
            .good(good)
            .average(average)
            .needsImprovement(needsImprovement)
            .averageOverall(averageOverall)
            .build();
    }

    public MasterySignalsSummaryDto getStudentMasterySignalsSummary(UUID studentId, UUID subjectId) {
        zw.co.zivai.core_backend.common.dtos.students.StudentDto student = studentService.get(studentId);
        double overall = student.getOverall();
        if (subjectId != null) {
            List<StudentAttribute> attributes = studentAttributeRepository.findByStudent_IdAndSkill_Subject_Id(studentId, subjectId);
            if (!attributes.isEmpty()) {
                overall = attributes.stream()
                    .map(StudentAttribute::getCurrentScore)
                    .filter(score -> score != null)
                    .mapToDouble(Double::doubleValue)
                    .average()
                    .orElse(overall);
            }
        }

        int excellent = 0;
        int good = 0;
        int average = 0;
        int needsImprovement = 0;
        if (overall >= 85.0) {
            excellent = 1;
        } else if (overall >= 70.0) {
            good = 1;
        } else if (overall >= 55.0) {
            average = 1;
        } else {
            needsImprovement = 1;
        }

        double normalizedOverall = Math.round(overall * 10.0) / 10.0;
        return MasterySignalsSummaryDto.builder()
            .totalStudents(1)
            .excellent(excellent)
            .good(good)
            .average(average)
            .needsImprovement(needsImprovement)
            .averageOverall(normalizedOverall)
            .build();
    }

    public StudentStreakDto touchStudentStreak(UUID studentId, LocalDate activityDate) {
        User student = userRepository.findByIdAndDeletedAtIsNull(studentId)
            .orElseThrow(() -> new NotFoundException("Student not found: " + studentId));

        LocalDate targetDate = activityDate != null ? activityDate : LocalDate.now(ZoneOffset.UTC);
        StudentActivityDay entry = studentActivityDayRepository
            .findByStudent_IdAndActivityDateAndDeletedAtIsNull(studentId, targetDate)
            .orElseGet(() -> {
                StudentActivityDay created = new StudentActivityDay();
                created.setStudent(student);
                created.setActivityDate(targetDate);
                created.setActivityCount(0);
                return created;
            });

        int nextCount = Math.max(0, entry.getActivityCount() == null ? 0 : entry.getActivityCount()) + 1;
        entry.setActivityCount(nextCount);
        entry.setLastActivityAt(Instant.now());
        studentActivityDayRepository.save(entry);

        return getStudentStreak(studentId, targetDate);
    }

    public StudentStreakDto getStudentStreak(UUID studentId, LocalDate referenceDate) {
        userRepository.findByIdAndDeletedAtIsNull(studentId)
            .orElseThrow(() -> new NotFoundException("Student not found: " + studentId));

        LocalDate pivotDate = referenceDate != null ? referenceDate : LocalDate.now(ZoneOffset.UTC);
        List<StudentActivityDay> activityDays = studentActivityDayRepository
            .findTop365ByStudent_IdAndDeletedAtIsNullOrderByActivityDateDesc(studentId);

        Set<LocalDate> activeDates = new HashSet<>();
        LocalDate lastActiveDate = null;
        for (StudentActivityDay day : activityDays) {
            if (day.getActivityDate() == null) {
                continue;
            }
            activeDates.add(day.getActivityDate());
            if (lastActiveDate == null || day.getActivityDate().isAfter(lastActiveDate)) {
                lastActiveDate = day.getActivityDate();
            }
        }

        boolean activeToday = activeDates.contains(pivotDate);
        LocalDate streakCursor = pivotDate;
        if (!activeToday) {
            LocalDate previousDay = pivotDate.minusDays(1);
            if (!activeDates.contains(previousDay)) {
                return buildStudentStreakDto(studentId, 0, false, lastActiveDate);
            }
            streakCursor = previousDay;
        }

        int streakDays = 0;
        while (activeDates.contains(streakCursor)) {
            streakDays += 1;
            streakCursor = streakCursor.minusDays(1);
        }

        return buildStudentStreakDto(studentId, streakDays, activeToday, lastActiveDate);
    }

    private void notifyPlanAssigned(StudentPlan studentPlan) {
        notifyPlanEvent(
            studentPlan,
            "development_plan_assigned",
            "New development plan assigned",
            "A development plan has been assigned for " + safeSubjectName(studentPlan) + "."
        );
    }

    private void notifyPlanUpdated(StudentPlan studentPlan, String reason) {
        String suffix = (reason == null || reason.isBlank()) ? "" : " " + reason;
        notifyPlanEvent(
            studentPlan,
            "development_plan_updated",
            "Development plan updated",
            "Your development plan for " + safeSubjectName(studentPlan) + " was updated." + suffix
        );
    }

    private void notifyPlanEvent(StudentPlan studentPlan,
                                 String notifType,
                                 String title,
                                 String message) {
        if (studentPlan == null || studentPlan.getStudent() == null || studentPlan.getStudent().getId() == null) {
            return;
        }
        UUID schoolId = resolveStudentSchoolId(studentPlan.getStudent().getId());
        if (schoolId == null) {
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("studentPlanId", studentPlan.getId() == null ? null : studentPlan.getId().toString());
        data.put("planId", studentPlan.getPlan() == null || studentPlan.getPlan().getId() == null ? null : studentPlan.getPlan().getId().toString());
        data.put("subjectId", studentPlan.getSubject() == null || studentPlan.getSubject().getId() == null ? null : studentPlan.getSubject().getId().toString());
        data.put("subjectName", safeSubjectName(studentPlan));
        data.put("status", studentPlan.getStatus());
        data.put("event", notifType);

        try {
            notificationService.createBulk(
                schoolId,
                List.of(studentPlan.getStudent().getId()),
                notifType,
                title,
                message,
                data,
                "medium"
            );
        } catch (RuntimeException ex) {
            log.warn("Failed to create {} notification for student plan {}", notifType, studentPlan.getId(), ex);
        }
    }

    private UUID resolveStudentSchoolId(UUID studentId) {
        return enrolmentRepository.findByStudent_Id(studentId).stream()
            .filter(enrolment -> enrolment.getDeletedAt() == null)
            .map(Enrolment::getClassEntity)
            .filter(classEntity -> classEntity != null && classEntity.getDeletedAt() == null)
            .map(ClassEntity::getSchool)
            .filter(school -> school != null && school.getDeletedAt() == null && school.getId() != null)
            .map(school -> school.getId())
            .findFirst()
            .orElse(null);
    }

    private String safeSubjectName(StudentPlan studentPlan) {
        if (studentPlan != null && studentPlan.getSubject() != null
            && studentPlan.getSubject().getName() != null && !studentPlan.getSubject().getName().isBlank()) {
            return studentPlan.getSubject().getName();
        }
        return "your subject";
    }

    private void applyProgressUpdate(StudentPlan studentPlan, UpdatePlanProgressRequest request) {
        if (request == null) {
            return;
        }
        if (request.getCurrentProgress() != null) {
            studentPlan.setCurrentProgress(request.getCurrentProgress());
        }
        if (request.getStatus() != null) {
            studentPlan.setStatus(normalizeStatus(request.getStatus()));
        }
        if (request.getSkillProgress() != null) {
            for (UpdatePlanProgressRequest.SkillProgressRequest progress : request.getSkillProgress()) {
                if (progress.getSkill() == null) {
                    continue;
                }
                Skill skill = skillRepository.findBySubject_IdAndNameIgnoreCase(
                    studentPlan.getSubject().getId(),
                    progress.getSkill()
                ).orElse(null);
                if (skill == null) {
                    continue;
                }
                StudentAttribute attribute = studentAttributeRepository
                    .findByStudent_IdAndSkill_Id(studentPlan.getStudent().getId(), skill.getId())
                    .orElseGet(() -> {
                        StudentAttribute created = new StudentAttribute();
                        created.setStudent(studentPlan.getStudent());
                        created.setSkill(skill);
                        created.setPotentialScore(progress.getTargetScore() != null ? progress.getTargetScore() : 100.0);
                        return created;
                    });
                if (progress.getCurrentScore() != null) {
                    attribute.setCurrentScore(progress.getCurrentScore());
                }
                if (progress.getTargetScore() != null) {
                    attribute.setPotentialScore(progress.getTargetScore());
                }
                attribute.setLastAssessed(Instant.now());
                studentAttributeRepository.save(attribute);
            }
        }
    }

    private StudentStreakDto buildStudentStreakDto(UUID studentId, int streakDays, boolean activeToday, LocalDate lastActiveDate) {
        int normalizedStreakDays = Math.max(0, streakDays);
        int streakWeeks = normalizedStreakDays == 0 ? 0 : ((normalizedStreakDays - 1) / 7) + 1;
        int level = Math.max(1, streakWeeks == 0 ? 1 : streakWeeks);
        int progressToNextWeek = normalizedStreakDays == 0 ? 0 : (int) Math.round((((normalizedStreakDays - 1) % 7) + 1) / 7.0 * 100.0);

        return StudentStreakDto.builder()
            .studentId(studentId.toString())
            .streakDays(normalizedStreakDays)
            .streakWeeks(streakWeeks)
            .level(level)
            .progressToNextWeek(progressToNextWeek)
            .activeToday(activeToday)
            .lastActiveDate(lastActiveDate)
            .build();
    }

    private static final class PlanSerializationContext {
        private final Map<UUID, List<PlanStep>> stepsByPlanId;
        private final Map<UUID, List<PlanSkill>> skillsByPlanId;
        private final Map<UUID, List<PlanSubskill>> subskillsByPlanSkillId;
        private final Map<UUID, Map<String, Skill>> skillsBySubjectAndName;
        private final Map<String, Map<UUID, StudentAttribute>> attributesByStudentSubjectAndSkillId;

        private PlanSerializationContext(Map<UUID, List<PlanStep>> stepsByPlanId,
                                         Map<UUID, List<PlanSkill>> skillsByPlanId,
                                         Map<UUID, List<PlanSubskill>> subskillsByPlanSkillId,
                                         Map<UUID, Map<String, Skill>> skillsBySubjectAndName,
                                         Map<String, Map<UUID, StudentAttribute>> attributesByStudentSubjectAndSkillId) {
            this.stepsByPlanId = stepsByPlanId;
            this.skillsByPlanId = skillsByPlanId;
            this.subskillsByPlanSkillId = subskillsByPlanSkillId;
            this.skillsBySubjectAndName = skillsBySubjectAndName;
            this.attributesByStudentSubjectAndSkillId = attributesByStudentSubjectAndSkillId;
        }
    }

    private List<DevelopmentPlanDto> toDevelopmentPlanDtos(List<StudentPlan> plans) {
        if (plans == null || plans.isEmpty()) {
            return List.of();
        }
        PlanSerializationContext context = buildPlanSerializationContext(plans);
        return plans.stream()
            .map(plan -> toDevelopmentPlanDto(plan, context))
            .toList();
    }

    private PlanSerializationContext buildPlanSerializationContext(List<StudentPlan> plans) {
        List<UUID> planIds = plans.stream()
            .map(StudentPlan::getPlan)
            .filter(plan -> plan != null && plan.getId() != null)
            .map(Plan::getId)
            .distinct()
            .toList();

        Map<UUID, List<PlanStep>> stepsByPlanId = planIds.isEmpty()
            ? Map.of()
            : planStepRepository.findByPlan_IdInOrderByPlan_IdAscStepOrderAsc(planIds).stream()
                .filter(step -> step.getPlan() != null && step.getPlan().getId() != null)
                .collect(Collectors.groupingBy(step -> step.getPlan().getId()));

        List<PlanSkill> planSkills = planIds.isEmpty()
            ? List.of()
            : planSkillRepository.findByPlan_IdIn(planIds);
        Map<UUID, List<PlanSkill>> skillsByPlanId = planSkills.stream()
            .filter(skill -> skill.getPlan() != null && skill.getPlan().getId() != null)
            .collect(Collectors.groupingBy(skill -> skill.getPlan().getId()));

        List<UUID> planSkillIds = planSkills.stream()
            .map(PlanSkill::getId)
            .filter(id -> id != null)
            .toList();
        Map<UUID, List<PlanSubskill>> subskillsByPlanSkillId = planSkillIds.isEmpty()
            ? Map.of()
            : planSubskillRepository.findByPlanSkill_IdIn(planSkillIds).stream()
                .filter(subskill -> subskill.getPlanSkill() != null && subskill.getPlanSkill().getId() != null)
                .collect(Collectors.groupingBy(subskill -> subskill.getPlanSkill().getId()));

        List<UUID> subjectIds = plans.stream()
            .map(StudentPlan::getSubject)
            .filter(subject -> subject != null && subject.getId() != null)
            .map(Subject::getId)
            .distinct()
            .toList();
        Map<UUID, Map<String, Skill>> skillsBySubjectAndName = subjectIds.isEmpty()
            ? Map.of()
            : skillRepository.findBySubject_IdIn(subjectIds).stream()
                .filter(skill -> skill.getSubject() != null && skill.getSubject().getId() != null)
                .filter(skill -> skill.getName() != null && !skill.getName().isBlank())
                .collect(Collectors.groupingBy(
                    skill -> skill.getSubject().getId(),
                    Collectors.toMap(
                        skill -> skill.getName().trim().toLowerCase(Locale.ROOT),
                        skill -> skill,
                        (left, right) -> left
                    )
                ));

        List<UUID> studentIds = plans.stream()
            .map(StudentPlan::getStudent)
            .filter(student -> student != null && student.getId() != null)
            .map(User::getId)
            .distinct()
            .toList();
        Map<String, Map<UUID, StudentAttribute>> attributesByStudentSubjectAndSkillId =
            (studentIds.isEmpty() || subjectIds.isEmpty())
                ? Map.of()
                : studentAttributeRepository.findByStudent_IdInAndSkill_Subject_IdIn(studentIds, subjectIds).stream()
                    .filter(attribute -> attribute.getStudent() != null && attribute.getStudent().getId() != null)
                    .filter(attribute -> attribute.getSkill() != null && attribute.getSkill().getId() != null)
                    .filter(attribute -> attribute.getSkill().getSubject() != null && attribute.getSkill().getSubject().getId() != null)
                    .collect(Collectors.groupingBy(
                        attribute -> studentSubjectKey(attribute.getStudent().getId(), attribute.getSkill().getSubject().getId()),
                        Collectors.toMap(
                            attribute -> attribute.getSkill().getId(),
                            attribute -> attribute,
                            (left, right) -> left
                        )
                    ));

        return new PlanSerializationContext(
            stepsByPlanId,
            skillsByPlanId,
            subskillsByPlanSkillId,
            skillsBySubjectAndName,
            attributesByStudentSubjectAndSkillId
        );
    }

    private String studentSubjectKey(UUID studentId, UUID subjectId) {
        return studentId + "|" + subjectId;
    }

    private PlanDto toPlanDto(Plan plan) {
        return toPlanDto(plan, null);
    }

    private PlanDto toPlanDto(Plan plan, PlanSerializationContext context) {
        List<PlanStep> steps = context == null
            ? planStepRepository.findByPlan_IdOrderByStepOrderAsc(plan.getId())
            : context.stepsByPlanId.getOrDefault(plan.getId(), List.of());
        List<PlanSkill> skills = context == null
            ? planSkillRepository.findByPlan_Id(plan.getId())
            : context.skillsByPlanId.getOrDefault(plan.getId(), List.of());

        List<PlanDto.PlanSkillDto> skillDtos = new ArrayList<>();
        for (PlanSkill skill : skills) {
            List<PlanSubskill> subskills = context == null
                ? planSubskillRepository.findByPlanSkill_Id(skill.getId())
                : context.subskillsByPlanSkillId.getOrDefault(skill.getId(), List.of());
            List<PlanDto.PlanSubskillDto> subskillDtos = subskills.stream()
                .map(sub -> PlanDto.PlanSubskillDto.builder()
                    .name(sub.getName())
                    .score(sub.getScore())
                    .color(sub.getColor())
                    .build())
                .toList();
            skillDtos.add(PlanDto.PlanSkillDto.builder()
                .name(skill.getName())
                .score(skill.getScore())
                .subskills(subskillDtos)
                .build());
        }

        List<PlanDto.PlanStepDto> stepDtos = steps.stream()
            .sorted(Comparator.comparing(PlanStep::getStepOrder, Comparator.nullsLast(Comparator.naturalOrder())))
            .map(step -> PlanDto.PlanStepDto.builder()
                .id(step.getId() == null ? null : step.getId().toString())
                .title(step.getTitle())
                .type(step.getStepType())
                .content(step.getContent())
                .link(step.getLink())
                .order(step.getStepOrder())
                .additionalResources(List.of())
                .build())
            .toList();

        return PlanDto.builder()
            .id(plan.getId().toString())
            .name(plan.getName())
            .description(plan.getDescription())
            .progress(plan.getProgress() == null ? 0.0 : plan.getProgress())
            .potentialOverall(plan.getPotentialOverall() == null ? 0.0 : plan.getPotentialOverall())
            .eta(plan.getEtaDays() == null ? 0 : plan.getEtaDays())
            .performance(plan.getPerformance())
            .skills(skillDtos)
            .steps(stepDtos)
            .subjectId(plan.getSubject() != null ? plan.getSubject().getId().toString() : null)
            .createdAt(plan.getCreatedAt())
            .updatedAt(plan.getUpdatedAt())
            .build();
    }

    private DevelopmentPlanDto toDevelopmentPlanDto(StudentPlan plan, PlanSerializationContext context) {
        PlanDto planDto = toPlanDto(plan.getPlan(), context);
        List<DevelopmentPlanDto.StudentSkillProgressDto> progress = new ArrayList<>();
        if (plan.getPlan() != null && plan.getSubject() != null && plan.getStudent() != null) {
            Map<String, Skill> skillsByName = context == null
                ? skillRepository.findBySubject_Id(plan.getSubject().getId()).stream()
                    .filter(skill -> skill.getName() != null && !skill.getName().isBlank())
                    .collect(Collectors.toMap(
                        skill -> skill.getName().trim().toLowerCase(Locale.ROOT),
                        skill -> skill,
                        (left, right) -> left
                    ))
                : context.skillsBySubjectAndName.getOrDefault(plan.getSubject().getId(), Map.of());

            Map<UUID, StudentAttribute> attributesBySkillId = context == null
                ? studentAttributeRepository
                    .findByStudent_IdAndSkill_Subject_Id(plan.getStudent().getId(), plan.getSubject().getId()).stream()
                    .filter(attribute -> attribute.getSkill() != null)
                    .collect(Collectors.toMap(
                        attribute -> attribute.getSkill().getId(),
                        attribute -> attribute,
                        (left, right) -> left
                    ))
                : context.attributesByStudentSubjectAndSkillId.getOrDefault(
                    studentSubjectKey(plan.getStudent().getId(), plan.getSubject().getId()),
                    Map.of()
                );

            for (PlanDto.PlanSkillDto skillDto : planDto.getSkills()) {
                if (skillDto.getName() == null || skillDto.getName().isBlank()) {
                    continue;
                }
                Skill skill = skillsByName.get(skillDto.getName().trim().toLowerCase(Locale.ROOT));
                StudentAttribute attribute = skill != null ? attributesBySkillId.get(skill.getId()) : null;
                if (attribute != null) {
                    progress.add(DevelopmentPlanDto.StudentSkillProgressDto.builder()
                        .skill(skillDto.getName())
                        .currentScore(attribute.getCurrentScore())
                        .targetScore(skillDto.getScore())
                        .lastUpdated(attribute.getLastAssessed())
                        .build());
                }
            }
        }

        return DevelopmentPlanDto.builder()
            .id(plan.getId().toString())
            .student(plan.getStudent().getId().toString())
            .plan(planDto)
            .startDate(plan.getStartDate())
            .currentProgress(plan.getCurrentProgress())
            .status(toUiStatus(plan.getStatus()))
            .completionDate(plan.getCompletionDate())
            .skillProgress(progress)
            .createdAt(plan.getCreatedAt())
            .updatedAt(plan.getUpdatedAt())
            .build();
    }

    private String normalizeStatus(String value) {
        if (value == null) {
            return "on_hold";
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "active" -> "active";
            case "completed" -> "completed";
            case "cancelled" -> "cancelled";
            case "on_hold", "on hold" -> "on_hold";
            default -> normalized;
        };
    }

    private String normalizePlanStepType(String value) {
        if (value == null || value.isBlank()) {
            return "document";
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "video", "document", "assessment", "discussion" -> normalized;
            case "reading", "resource", "notes" -> "document";
            case "exercise", "practice", "quiz", "assignment", "test" -> "assessment";
            case "meeting", "collaboration", "group" -> "discussion";
            default -> "document";
        };
    }

    private String toUiStatus(String value) {
        String normalized = normalizeStatus(value);
        return switch (normalized) {
            case "active" -> "Active";
            case "completed" -> "Completed";
            case "cancelled" -> "Cancelled";
            case "on_hold" -> "On Hold";
            default -> value;
        };
    }

    private Double asDouble(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private boolean hasPlanTemplateUpdates(UpdateStudentPlanRequest request) {
        return request.getName() != null
            || request.getDescription() != null
            || request.getProgress() != null
            || request.getPotentialOverall() != null
            || request.getEta() != null
            || request.getPerformance() != null;
    }

    private void replacePlanSteps(Plan plan, List<UpdateStudentPlanRequest.PlanStepRequest> stepRequests) {
        List<PlanStep> existingSteps = planStepRepository.findByPlan_IdOrderByStepOrderAsc(plan.getId());
        if (!existingSteps.isEmpty()) {
            planStepRepository.deleteAll(existingSteps);
        }

        List<PlanStep> nextSteps = new ArrayList<>();
        for (int index = 0; index < stepRequests.size(); index++) {
            UpdateStudentPlanRequest.PlanStepRequest stepRequest = stepRequests.get(index);
            if (stepRequest == null) {
                continue;
            }
            PlanStep step = new PlanStep();
            step.setPlan(plan);
            step.setTitle(stepRequest.getTitle() == null ? "Step" : stepRequest.getTitle());
            step.setStepType(normalizePlanStepType(stepRequest.getType()));
            step.setContent(stepRequest.getContent());
            step.setLink(stepRequest.getLink());
            step.setStepOrder(stepRequest.getOrder() != null ? stepRequest.getOrder() : index + 1);
            nextSteps.add(step);
        }

        if (!nextSteps.isEmpty()) {
            planStepRepository.saveAll(nextSteps);
        }
    }

    private void replacePlanStepsFromCreate(Plan plan, List<CreatePlanRequest.PlanStepRequest> stepRequests) {
        List<PlanStep> existingSteps = planStepRepository.findByPlan_IdOrderByStepOrderAsc(plan.getId());
        if (!existingSteps.isEmpty()) {
            planStepRepository.deleteAll(existingSteps);
        }

        List<PlanStep> nextSteps = new ArrayList<>();
        for (int index = 0; index < stepRequests.size(); index++) {
            CreatePlanRequest.PlanStepRequest stepRequest = stepRequests.get(index);
            if (stepRequest == null) {
                continue;
            }
            PlanStep step = new PlanStep();
            step.setPlan(plan);
            step.setTitle(stepRequest.getTitle() == null ? "Step" : stepRequest.getTitle());
            step.setStepType(normalizePlanStepType(stepRequest.getType()));
            step.setContent(stepRequest.getContent());
            step.setLink(stepRequest.getLink());
            step.setStepOrder(stepRequest.getOrder() != null ? stepRequest.getOrder() : index + 1);
            nextSteps.add(step);
        }

        if (!nextSteps.isEmpty()) {
            planStepRepository.saveAll(nextSteps);
        }
    }

    private void replacePlanSkills(Plan plan, List<UpdateStudentPlanRequest.PlanSkillRequest> skillRequests) {
        List<PlanSkill> existingSkills = planSkillRepository.findByPlan_Id(plan.getId());
        if (!existingSkills.isEmpty()) {
            List<UUID> existingSkillIds = existingSkills.stream()
                .map(PlanSkill::getId)
                .filter(id -> id != null)
                .toList();
            if (!existingSkillIds.isEmpty()) {
                List<PlanSubskill> existingSubskills = planSubskillRepository.findByPlanSkill_IdIn(existingSkillIds);
                if (!existingSubskills.isEmpty()) {
                    planSubskillRepository.deleteAll(existingSubskills);
                }
            }
            planSkillRepository.deleteAll(existingSkills);
        }

        if (skillRequests.isEmpty()) {
            return;
        }

        List<Map.Entry<PlanSkill, UpdateStudentPlanRequest.PlanSkillRequest>> createdSkillPairs = new ArrayList<>();
        for (UpdateStudentPlanRequest.PlanSkillRequest skillRequest : skillRequests) {
            if (skillRequest == null) {
                continue;
            }
            PlanSkill planSkill = new PlanSkill();
            planSkill.setPlan(plan);
            planSkill.setName(skillRequest.getName() == null ? "Skill" : skillRequest.getName());
            planSkill.setScore(skillRequest.getScore());
            PlanSkill savedSkill = planSkillRepository.save(planSkill);
            createdSkillPairs.add(Map.entry(savedSkill, skillRequest));
        }

        List<PlanSubskill> createdSubskills = new ArrayList<>();
        for (Map.Entry<PlanSkill, UpdateStudentPlanRequest.PlanSkillRequest> createdSkillPair : createdSkillPairs) {
            PlanSkill createdSkill = createdSkillPair.getKey();
            UpdateStudentPlanRequest.PlanSkillRequest skillRequest = createdSkillPair.getValue();
            if (skillRequest == null || skillRequest.getSubskills() == null) {
                continue;
            }
            for (UpdateStudentPlanRequest.PlanSubskillRequest subskillRequest : skillRequest.getSubskills()) {
                if (subskillRequest == null) {
                    continue;
                }
                PlanSubskill subskill = new PlanSubskill();
                subskill.setPlanSkill(createdSkill);
                subskill.setName(subskillRequest.getName() == null ? "Subskill" : subskillRequest.getName());
                subskill.setScore(subskillRequest.getScore());
                if (subskillRequest.getColor() != null && !subskillRequest.getColor().isBlank()) {
                    subskill.setColor(subskillRequest.getColor());
                }
                createdSubskills.add(subskill);
            }
        }

        if (!createdSubskills.isEmpty()) {
            planSubskillRepository.saveAll(createdSubskills);
        }
    }

    private void replacePlanSkillsFromCreate(Plan plan, List<CreatePlanRequest.PlanSkillRequest> skillRequests) {
        List<PlanSkill> existingSkills = planSkillRepository.findByPlan_Id(plan.getId());
        if (!existingSkills.isEmpty()) {
            List<UUID> existingSkillIds = existingSkills.stream()
                .map(PlanSkill::getId)
                .filter(id -> id != null)
                .toList();
            if (!existingSkillIds.isEmpty()) {
                List<PlanSubskill> existingSubskills = planSubskillRepository.findByPlanSkill_IdIn(existingSkillIds);
                if (!existingSubskills.isEmpty()) {
                    planSubskillRepository.deleteAll(existingSubskills);
                }
            }
            planSkillRepository.deleteAll(existingSkills);
        }

        if (skillRequests.isEmpty()) {
            return;
        }

        List<Map.Entry<PlanSkill, CreatePlanRequest.PlanSkillRequest>> createdSkillPairs = new ArrayList<>();
        for (CreatePlanRequest.PlanSkillRequest skillRequest : skillRequests) {
            if (skillRequest == null) {
                continue;
            }
            PlanSkill planSkill = new PlanSkill();
            planSkill.setPlan(plan);
            planSkill.setName(skillRequest.getName() == null ? "Skill" : skillRequest.getName());
            planSkill.setScore(skillRequest.getScore());
            PlanSkill savedSkill = planSkillRepository.save(planSkill);
            createdSkillPairs.add(Map.entry(savedSkill, skillRequest));
        }

        List<PlanSubskill> createdSubskills = new ArrayList<>();
        for (Map.Entry<PlanSkill, CreatePlanRequest.PlanSkillRequest> createdSkillPair : createdSkillPairs) {
            PlanSkill createdSkill = createdSkillPair.getKey();
            CreatePlanRequest.PlanSkillRequest skillRequest = createdSkillPair.getValue();
            if (skillRequest == null || skillRequest.getSubskills() == null) {
                continue;
            }
            for (CreatePlanRequest.PlanSubskillRequest subskillRequest : skillRequest.getSubskills()) {
                if (subskillRequest == null) {
                    continue;
                }
                PlanSubskill subskill = new PlanSubskill();
                subskill.setPlanSkill(createdSkill);
                subskill.setName(subskillRequest.getName() == null ? "Subskill" : subskillRequest.getName());
                subskill.setScore(subskillRequest.getScore());
                if (subskillRequest.getColor() != null && !subskillRequest.getColor().isBlank()) {
                    subskill.setColor(subskillRequest.getColor());
                }
                createdSubskills.add(subskill);
            }
        }

        if (!createdSubskills.isEmpty()) {
            planSubskillRepository.saveAll(createdSubskills);
        }
    }

    private StudentPlan getStudentPlanEntity(UUID studentPlanId) {
        StudentPlan studentPlan = studentPlanRepository.findById(studentPlanId)
            .orElseThrow(() -> new NotFoundException("Student plan not found: " + studentPlanId));
        if (studentPlan.getDeletedAt() != null) {
            throw new NotFoundException("Student plan not found: " + studentPlanId);
        }
        if (studentPlan.getPlan() == null || studentPlan.getPlan().getDeletedAt() != null) {
            throw new NotFoundException("Plan template not found for student plan: " + studentPlanId);
        }
        return studentPlan;
    }

    private void validateStepRequest(StudentPlanStepRequest request) {
        if (request == null) {
            throw new BadRequestException("Step request is required");
        }
        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new BadRequestException("Step title is required");
        }
    }

    private void normalizeStepOrders(Plan plan) {
        List<PlanStep> steps = planStepRepository.findByPlan_IdOrderByStepOrderAsc(plan.getId());
        persistStepOrderingSafely(steps);
    }

    private void persistStepOrderingSafely(List<PlanStep> orderedSteps) {
        if (orderedSteps == null || orderedSteps.isEmpty()) {
            return;
        }

        int offset = orderedSteps.size() + 10_000;
        for (int index = 0; index < orderedSteps.size(); index += 1) {
            orderedSteps.get(index).setStepOrder(offset + index);
        }
        planStepRepository.saveAllAndFlush(orderedSteps);

        for (int index = 0; index < orderedSteps.size(); index += 1) {
            orderedSteps.get(index).setStepOrder(index + 1);
        }
        planStepRepository.saveAllAndFlush(orderedSteps);
    }

    private Double clampProgress(Double value) {
        if (value == null) {
            return null;
        }
        if (value < 0) {
            return 0.0;
        }
        if (value > 100) {
            return 100.0;
        }
        return value;
    }

    private PageResponse<DevelopmentPlanDto> emptyPage(int page, int size) {
        return PageResponse.<DevelopmentPlanDto>builder()
            .items(List.of())
            .page(page)
            .size(size)
            .totalItems(0)
            .totalPages(0)
            .build();
    }

    private void ensureStarterPlansForEnrolments(List<StudentSubjectEnrolment> enrolments, UUID subjectFilter) {
        if (enrolments == null || enrolments.isEmpty()) {
            return;
        }

        Map<String, StudentSubjectEnrolment> enrolmentsByKey = new HashMap<>();
        Set<UUID> studentIds = new HashSet<>();

        for (StudentSubjectEnrolment enrolment : enrolments) {
            if (enrolment == null || enrolment.getDeletedAt() != null || enrolment.getStudent() == null) {
                continue;
            }
            Subject subject = enrolment.getClassSubject() != null ? enrolment.getClassSubject().getSubject() : null;
            if (subject == null || subject.getDeletedAt() != null) {
                continue;
            }
            if (subjectFilter != null && !subjectFilter.equals(subject.getId())) {
                continue;
            }

            UUID studentId = enrolment.getStudent().getId();
            if (studentId == null) {
                continue;
            }
            studentIds.add(studentId);
            enrolmentsByKey.putIfAbsent(studentSubjectKey(studentId, subject.getId()), enrolment);
        }

        if (enrolmentsByKey.isEmpty()) {
            return;
        }

        List<StudentPlan> existingPlans = subjectFilter != null
            ? studentPlanRepository.findByStudent_IdInAndSubject_IdAndDeletedAtIsNull(new ArrayList<>(studentIds), subjectFilter)
            : studentPlanRepository.findByStudent_IdInAndDeletedAtIsNull(new ArrayList<>(studentIds));

        Set<String> existingKeys = existingPlans.stream()
            .filter(plan -> plan.getStudent() != null && plan.getStudent().getId() != null)
            .filter(plan -> plan.getSubject() != null && plan.getSubject().getId() != null)
            .map(plan -> studentSubjectKey(plan.getStudent().getId(), plan.getSubject().getId()))
            .collect(Collectors.toSet());

        for (Map.Entry<String, StudentSubjectEnrolment> entry : enrolmentsByKey.entrySet()) {
            if (existingKeys.contains(entry.getKey())) {
                continue;
            }
            StudentSubjectEnrolment enrolment = entry.getValue();
            createStarterStudentPlan(enrolment.getStudent(), enrolment.getClassSubject().getSubject());
        }
    }

    private StudentPlan createStarterStudentPlan(User student, Subject subject) {
        if (student == null || student.getId() == null || subject == null || subject.getId() == null) {
            throw new BadRequestException("Student and subject are required to create a starter plan");
        }

        List<StudentPlan> existingPlans = studentPlanRepository
            .findByStudent_IdAndSubject_IdAndDeletedAtIsNullOrderByCreatedAtDesc(student.getId(), subject.getId());
        if (!existingPlans.isEmpty()) {
            return existingPlans.get(0);
        }

        Plan plan = new Plan();
        plan.setSubject(subject);
        plan.setName(buildStarterPlanName(student, subject));
        plan.setDescription("Starter development plan for " + safeUserName(student) + " in " + safeSubjectName(subject) + ".");
        plan.setProgress(0.0);
        plan.setPotentialOverall(70.0);
        plan.setEtaDays(14);
        plan.setPerformance("Tracking");
        Plan savedPlan = planRepository.save(plan);

        StudentPlan studentPlan = new StudentPlan();
        studentPlan.setStudent(student);
        studentPlan.setPlan(savedPlan);
        studentPlan.setSubject(subject);
        studentPlan.setStartDate(Instant.now());
        studentPlan.setCurrentProgress(0.0);
        studentPlan.setStatus("on_hold");
        studentPlan.setCurrent(false);
        return studentPlanRepository.save(studentPlan);
    }

    private String buildStarterPlanName(User student, Subject subject) {
        return safeUserName(student) + " " + safeSubjectName(subject) + " Development Plan";
    }

    private String safeUserName(User student) {
        String firstName = student.getFirstName() == null ? "" : student.getFirstName().trim();
        String lastName = student.getLastName() == null ? "" : student.getLastName().trim();
        String fullName = (firstName + " " + lastName).trim();
        if (!fullName.isBlank()) {
            return fullName;
        }
        if (student.getUsername() != null && !student.getUsername().isBlank()) {
            return student.getUsername().trim();
        }
        return "Student";
    }

    private String safeSubjectName(Subject subject) {
        if (subject.getName() != null && !subject.getName().isBlank()) {
            return subject.getName().trim();
        }
        if (subject.getCode() != null && !subject.getCode().isBlank()) {
            return subject.getCode().trim();
        }
        return "Subject";
    }
}
