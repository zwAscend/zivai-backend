package zw.co.zivai.core_backend.services.development;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import zw.co.zivai.core_backend.dtos.development.AssignPlanRequest;
import zw.co.zivai.core_backend.dtos.development.CreatePlanRequest;
import zw.co.zivai.core_backend.dtos.development.DevelopmentAttributeDto;
import zw.co.zivai.core_backend.dtos.development.DevelopmentPlanDto;
import zw.co.zivai.core_backend.dtos.development.MasterySignalsSummaryDto;
import zw.co.zivai.core_backend.dtos.common.PageResponse;
import zw.co.zivai.core_backend.dtos.development.PlanDto;
import zw.co.zivai.core_backend.dtos.development.StudentAttributeUpdateRequest;
import zw.co.zivai.core_backend.dtos.development.UpdatePlanProgressRequest;
import zw.co.zivai.core_backend.dtos.development.UpdateStudentPlanRequest;
import zw.co.zivai.core_backend.exceptions.BadRequestException;
import zw.co.zivai.core_backend.exceptions.NotFoundException;
import zw.co.zivai.core_backend.models.lms.Plan;
import zw.co.zivai.core_backend.models.lms.PlanSkill;
import zw.co.zivai.core_backend.models.lms.PlanStep;
import zw.co.zivai.core_backend.models.lms.PlanSubskill;
import zw.co.zivai.core_backend.models.lms.Skill;
import zw.co.zivai.core_backend.models.lms.StudentAttribute;
import zw.co.zivai.core_backend.models.lms.StudentPlan;
import zw.co.zivai.core_backend.models.lms.StudentSubjectEnrolment;
import zw.co.zivai.core_backend.models.lms.Subject;
import zw.co.zivai.core_backend.models.lms.User;
import zw.co.zivai.core_backend.repositories.development.PlanRepository;
import zw.co.zivai.core_backend.repositories.development.PlanSkillRepository;
import zw.co.zivai.core_backend.repositories.development.PlanStepRepository;
import zw.co.zivai.core_backend.repositories.development.PlanSubskillRepository;
import zw.co.zivai.core_backend.repositories.subject.SkillRepository;
import zw.co.zivai.core_backend.repositories.development.StudentAttributeRepository;
import zw.co.zivai.core_backend.repositories.development.StudentPlanRepository;
import zw.co.zivai.core_backend.repositories.classroom.StudentSubjectEnrolmentRepository;
import zw.co.zivai.core_backend.repositories.subject.SubjectRepository;
import zw.co.zivai.core_backend.repositories.classroom.EnrolmentRepository;
import zw.co.zivai.core_backend.repositories.user.UserRepository;
import zw.co.zivai.core_backend.services.students.StudentService;

@Service
@RequiredArgsConstructor
public class DevelopmentService {
    private final SkillRepository skillRepository;
    private final StudentAttributeRepository studentAttributeRepository;
    private final PlanRepository planRepository;
    private final PlanStepRepository planStepRepository;
    private final PlanSkillRepository planSkillRepository;
    private final PlanSubskillRepository planSubskillRepository;
    private final StudentPlanRepository studentPlanRepository;
    private final StudentSubjectEnrolmentRepository studentSubjectEnrolmentRepository;
    private final SubjectRepository subjectRepository;
    private final EnrolmentRepository enrolmentRepository;
    private final UserRepository userRepository;
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
        return planRepository.findBySubject_Id(subjectId).stream()
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

    public List<DevelopmentPlanDto> getStudentPlans(UUID studentId, String status) {
        List<StudentPlan> plans = studentPlanRepository.findByStudent_Id(studentId).stream()
            .filter(plan -> plan.getDeletedAt() == null)
            .toList();
        if (status != null && !status.isBlank()) {
            String normalized = normalizeStatus(status);
            plans = plans.stream()
                .filter(plan -> normalized.equalsIgnoreCase(normalizeStatus(plan.getStatus())))
                .toList();
        }
        return plans.stream()
            .sorted(Comparator.comparing(StudentPlan::getCreatedAt).reversed())
            .map(this::toDevelopmentPlanDto)
            .toList();
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

        if (classSubjectId != null) {
            List<UUID> studentIds = studentSubjectEnrolmentRepository
                .findByClassSubject_IdAndDeletedAtIsNull(classSubjectId).stream()
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
            resultPage = subjectId == null
                ? (normalizedStatus == null
                    ? studentPlanRepository.findByStudent_IdInAndDeletedAtIsNull(studentIds, pageable)
                    : studentPlanRepository.findByStudent_IdInAndStatusIgnoreCaseAndDeletedAtIsNull(studentIds, normalizedStatus, pageable))
                : (normalizedStatus == null
                    ? studentPlanRepository.findByStudent_IdInAndSubject_IdAndDeletedAtIsNull(studentIds, subjectId, pageable)
                    : studentPlanRepository.findByStudent_IdInAndSubject_IdAndStatusIgnoreCaseAndDeletedAtIsNull(studentIds, subjectId, normalizedStatus, pageable));
        } else if (subjectId != null) {
            resultPage = normalizedStatus == null
                ? studentPlanRepository.findBySubject_IdAndDeletedAtIsNull(subjectId, pageable)
                : studentPlanRepository.findBySubject_IdAndStatusIgnoreCaseAndDeletedAtIsNull(subjectId, normalizedStatus, pageable);
        } else {
            resultPage = normalizedStatus == null
                ? studentPlanRepository.findByDeletedAtIsNull(pageable)
                : studentPlanRepository.findByStatusIgnoreCaseAndDeletedAtIsNull(normalizedStatus, pageable);
        }

        List<DevelopmentPlanDto> items = resultPage.getContent().stream()
            .map(this::toDevelopmentPlanDto)
            .toList();

        return PageResponse.<DevelopmentPlanDto>builder()
            .items(items)
            .page(resultPage.getNumber())
            .size(resultPage.getSize())
            .totalItems(resultPage.getTotalElements())
            .totalPages(resultPage.getTotalPages())
            .build();
    }

    public DevelopmentPlanDto getStudentPlan(UUID studentId, UUID subjectId) {
        List<StudentPlan> plans = studentPlanRepository.findByStudent_IdAndSubject_IdOrderByCreatedAtDesc(studentId, subjectId)
            .stream()
            .filter(plan -> plan.getDeletedAt() == null)
            .toList();
        StudentPlan plan = plans.stream()
            .filter(StudentPlan::isCurrent)
            .findFirst()
            .orElseGet(() -> plans.stream()
                .findFirst()
                .orElseThrow(() -> new NotFoundException("No plan found for student and subject")));
        return toDevelopmentPlanDto(plan);
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
        return toDevelopmentPlanDto(saved);
    }

    public DevelopmentPlanDto updatePlanProgress(UUID studentId, UUID planId, UpdatePlanProgressRequest request) {
        StudentPlan studentPlan = studentPlanRepository.findByStudent_IdAndPlan_Id(studentId, planId)
            .orElseThrow(() -> new NotFoundException("Student plan not found"));
        if (studentPlan.getDeletedAt() != null) {
            throw new NotFoundException("Student plan not found");
        }
        applyProgressUpdate(studentPlan, request);
        return toDevelopmentPlanDto(studentPlanRepository.save(studentPlan));
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
        return toDevelopmentPlanDto(studentPlanRepository.save(studentPlan));
    }

    public DevelopmentPlanDto getStudentPlanById(UUID studentPlanId) {
        StudentPlan plan = studentPlanRepository.findById(studentPlanId)
            .orElseThrow(() -> new NotFoundException("Student plan not found: " + studentPlanId));
        if (plan.getDeletedAt() != null) {
            throw new NotFoundException("Student plan not found: " + studentPlanId);
        }
        return toDevelopmentPlanDto(plan);
    }

    public DevelopmentPlanDto updateStudentPlan(UUID studentPlanId, UpdateStudentPlanRequest request) {
        StudentPlan studentPlan = studentPlanRepository.findById(studentPlanId)
            .orElseThrow(() -> new NotFoundException("Student plan not found: " + studentPlanId));
        if (studentPlan.getDeletedAt() != null) {
            throw new NotFoundException("Student plan not found: " + studentPlanId);
        }
        if (request == null) {
            return toDevelopmentPlanDto(studentPlan);
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

        return toDevelopmentPlanDto(studentPlanRepository.save(studentPlan));
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

    public MasterySignalsSummaryDto getMasterySignalsSummary(UUID subjectId, UUID classId, UUID classSubjectId) {
        List<zw.co.zivai.core_backend.dtos.students.StudentDto> students = studentService.list(subjectId, classId, classSubjectId);
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

    private PlanDto toPlanDto(Plan plan) {
        List<PlanStep> steps = planStepRepository.findByPlan_IdOrderByStepOrderAsc(plan.getId());
        List<PlanSkill> skills = planSkillRepository.findByPlan_Id(plan.getId());

        List<PlanDto.PlanSkillDto> skillDtos = new ArrayList<>();
        for (PlanSkill skill : skills) {
            List<PlanSubskill> subskills = planSubskillRepository.findByPlanSkill_Id(skill.getId());
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
            .sorted(Comparator.comparing(PlanStep::getStepOrder))
            .map(step -> PlanDto.PlanStepDto.builder()
                .title(step.getTitle())
                .type(step.getStepType())
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

    private DevelopmentPlanDto toDevelopmentPlanDto(StudentPlan plan) {
        PlanDto planDto = toPlanDto(plan.getPlan());
        List<DevelopmentPlanDto.StudentSkillProgressDto> progress = new ArrayList<>();
        if (plan.getPlan() != null && plan.getSubject() != null && plan.getStudent() != null) {
            Map<String, Skill> skillsByName = skillRepository.findBySubject_Id(plan.getSubject().getId()).stream()
                .filter(skill -> skill.getName() != null && !skill.getName().isBlank())
                .collect(Collectors.toMap(
                    skill -> skill.getName().trim().toLowerCase(Locale.ROOT),
                    skill -> skill,
                    (left, right) -> left
                ));
            Map<UUID, StudentAttribute> attributesBySkillId = studentAttributeRepository
                .findByStudent_IdAndSkill_Subject_Id(plan.getStudent().getId(), plan.getSubject().getId()).stream()
                .filter(attribute -> attribute.getSkill() != null)
                .collect(Collectors.toMap(
                    attribute -> attribute.getSkill().getId(),
                    attribute -> attribute,
                    (left, right) -> left
                ));

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
}
