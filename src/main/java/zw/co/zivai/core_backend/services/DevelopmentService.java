package zw.co.zivai.core_backend.services;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import zw.co.zivai.core_backend.dtos.AssignPlanRequest;
import zw.co.zivai.core_backend.dtos.CreatePlanRequest;
import zw.co.zivai.core_backend.dtos.DevelopmentAttributeDto;
import zw.co.zivai.core_backend.dtos.DevelopmentPlanDto;
import zw.co.zivai.core_backend.dtos.PlanDto;
import zw.co.zivai.core_backend.dtos.StudentAttributeUpdateRequest;
import zw.co.zivai.core_backend.dtos.UpdatePlanProgressRequest;
import zw.co.zivai.core_backend.exceptions.BadRequestException;
import zw.co.zivai.core_backend.exceptions.NotFoundException;
import zw.co.zivai.core_backend.models.lms.Plan;
import zw.co.zivai.core_backend.models.lms.PlanSkill;
import zw.co.zivai.core_backend.models.lms.PlanStep;
import zw.co.zivai.core_backend.models.lms.PlanSubskill;
import zw.co.zivai.core_backend.models.lms.Skill;
import zw.co.zivai.core_backend.models.lms.StudentAttribute;
import zw.co.zivai.core_backend.models.lms.StudentPlan;
import zw.co.zivai.core_backend.models.lms.Subject;
import zw.co.zivai.core_backend.models.lms.User;
import zw.co.zivai.core_backend.repositories.PlanRepository;
import zw.co.zivai.core_backend.repositories.PlanSkillRepository;
import zw.co.zivai.core_backend.repositories.PlanStepRepository;
import zw.co.zivai.core_backend.repositories.PlanSubskillRepository;
import zw.co.zivai.core_backend.repositories.SkillRepository;
import zw.co.zivai.core_backend.repositories.StudentAttributeRepository;
import zw.co.zivai.core_backend.repositories.StudentPlanRepository;
import zw.co.zivai.core_backend.repositories.SubjectRepository;
import zw.co.zivai.core_backend.repositories.UserRepository;

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
    private final SubjectRepository subjectRepository;
    private final UserRepository userRepository;

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
        List<StudentPlan> plans = studentPlanRepository.findByStudent_Id(studentId);
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

    public DevelopmentPlanDto getStudentPlan(UUID studentId, UUID subjectId) {
        StudentPlan plan = studentPlanRepository
            .findFirstByStudent_IdAndSubject_IdAndCurrentTrue(studentId, subjectId)
            .orElseGet(() -> studentPlanRepository.findByStudent_IdAndSubject_IdOrderByCreatedAtDesc(studentId, subjectId)
                .stream().findFirst()
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
                if (existing.isCurrent()) {
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
        StudentPlan saved = studentPlanRepository.save(studentPlan);
        return toDevelopmentPlanDto(saved);
    }

    public DevelopmentPlanDto updatePlanProgress(UUID studentId, UUID planId, UpdatePlanProgressRequest request) {
        StudentPlan studentPlan = studentPlanRepository.findByStudent_IdAndPlan_Id(studentId, planId)
            .orElseThrow(() -> new NotFoundException("Student plan not found"));
        applyProgressUpdate(studentPlan, request);
        return toDevelopmentPlanDto(studentPlanRepository.save(studentPlan));
    }

    public DevelopmentPlanDto updatePlanProgressByStudentPlanId(UUID studentPlanId, Map<String, Object> request) {
        StudentPlan studentPlan = studentPlanRepository.findById(studentPlanId)
            .orElseThrow(() -> new NotFoundException("Student plan not found: " + studentPlanId));

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
        if (plan.getPlan() != null) {
            for (PlanDto.PlanSkillDto skillDto : planDto.getSkills()) {
                StudentAttribute attribute = skillRepository.findBySubject_IdAndNameIgnoreCase(
                    plan.getSubject().getId(),
                    skillDto.getName()
                ).map(skill -> studentAttributeRepository.findByStudent_IdAndSkill_Id(plan.getStudent().getId(), skill.getId()).orElse(null))
                .orElse(null);
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
}
