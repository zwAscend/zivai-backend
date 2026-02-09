package zw.co.zivai.core_backend.controllers;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import zw.co.zivai.core_backend.dtos.AssignPlanRequest;
import zw.co.zivai.core_backend.dtos.CreatePlanRequest;
import zw.co.zivai.core_backend.dtos.DevelopmentAttributeDto;
import zw.co.zivai.core_backend.dtos.DevelopmentPlanDto;
import zw.co.zivai.core_backend.dtos.MasterySignalsSummaryDto;
import zw.co.zivai.core_backend.dtos.PageResponse;
import zw.co.zivai.core_backend.dtos.PlanDto;
import zw.co.zivai.core_backend.dtos.StudentAttributeUpdateRequest;
import zw.co.zivai.core_backend.dtos.UpdatePlanProgressRequest;
import zw.co.zivai.core_backend.dtos.UpdateStudentPlanRequest;
import zw.co.zivai.core_backend.services.DevelopmentService;

@RestController
@RequestMapping("/api/development")
@RequiredArgsConstructor
public class DevelopmentController {
    private final DevelopmentService developmentService;

    @GetMapping("/attributes/subject/{subjectId}")
    public List<DevelopmentAttributeDto> getSubjectAttributes(@PathVariable UUID subjectId) {
        return developmentService.getSubjectAttributes(subjectId);
    }

    @GetMapping("/attributes/student/{studentId}/subject/{subjectId}")
    public Map<String, Object> getStudentAttributes(@PathVariable UUID studentId, @PathVariable UUID subjectId) {
        return developmentService.getStudentAttributes(studentId, subjectId);
    }

    @PutMapping("/attributes/student/{studentId}")
    public Map<String, Object> updateStudentAttributes(@PathVariable UUID studentId,
                                                       @RequestBody List<StudentAttributeUpdateRequest> updates) {
        developmentService.updateStudentAttributes(studentId, updates);
        return Map.of("message", "Student attributes updated");
    }

    @PostMapping("/attributes/subject")
    @ResponseStatus(HttpStatus.CREATED)
    public DevelopmentAttributeDto createSubjectAttribute(@RequestBody DevelopmentAttributeDto request) {
        return developmentService.createSubjectAttribute(request);
    }

    @GetMapping("/plans/subject/{subjectId}")
    public List<PlanDto> getSubjectPlans(@PathVariable UUID subjectId) {
        return developmentService.getSubjectPlans(subjectId);
    }

    @PostMapping("/plans/subject")
    @ResponseStatus(HttpStatus.CREATED)
    public PlanDto createSubjectPlan(@RequestBody CreatePlanRequest request) {
        return developmentService.createSubjectPlan(request);
    }

    @GetMapping("/plans/student/{studentId}")
    public List<DevelopmentPlanDto> getStudentPlans(@PathVariable UUID studentId,
                                                    @RequestParam(value = "status", required = false) String status) {
        return developmentService.getStudentPlans(studentId, status);
    }

    @GetMapping("/plans")
    public PageResponse<DevelopmentPlanDto> listStudentPlans(@RequestParam(required = false) UUID subjectId,
                                                             @RequestParam(required = false) UUID classId,
                                                             @RequestParam(required = false) UUID classSubjectId,
                                                             @RequestParam(required = false) String status,
                                                             @RequestParam(defaultValue = "0") int page,
                                                             @RequestParam(defaultValue = "20") int size) {
        return developmentService.listStudentPlans(subjectId, classId, classSubjectId, status, page, size);
    }

    @GetMapping("/plans/student/{studentId}/subject/{subjectId}")
    public DevelopmentPlanDto getStudentPlan(@PathVariable UUID studentId, @PathVariable UUID subjectId) {
        return developmentService.getStudentPlan(studentId, subjectId);
    }

    @PostMapping("/plans/student/{studentId}/assign")
    @ResponseStatus(HttpStatus.CREATED)
    public DevelopmentPlanDto assignPlan(@PathVariable UUID studentId, @RequestBody AssignPlanRequest request) {
        return developmentService.assignPlan(studentId, request);
    }

    @PutMapping("/plans/student/{studentId}/{planId}/progress")
    public DevelopmentPlanDto updatePlanProgress(@PathVariable UUID studentId,
                                                 @PathVariable UUID planId,
                                                 @RequestBody UpdatePlanProgressRequest request) {
        return developmentService.updatePlanProgress(studentId, planId, request);
    }

    @PatchMapping("/plans/{studentPlanId}/progress")
    public DevelopmentPlanDto updatePlanProgressById(@PathVariable UUID studentPlanId,
                                                     @RequestBody Map<String, Object> request) {
        return developmentService.updatePlanProgressByStudentPlanId(studentPlanId, request);
    }

    @GetMapping("/plans/student-plan/{studentPlanId}")
    public DevelopmentPlanDto getStudentPlanById(@PathVariable UUID studentPlanId) {
        return developmentService.getStudentPlanById(studentPlanId);
    }

    @PutMapping("/plans/student-plan/{studentPlanId}")
    public DevelopmentPlanDto updateStudentPlan(@PathVariable UUID studentPlanId,
                                                @RequestBody UpdateStudentPlanRequest request) {
        return developmentService.updateStudentPlan(studentPlanId, request);
    }

    @DeleteMapping("/plans/student-plan/{studentPlanId}")
    public Map<String, Object> deleteStudentPlan(@PathVariable UUID studentPlanId) {
        developmentService.deleteStudentPlan(studentPlanId);
        return Map.of("message", "Student plan deleted");
    }

    @GetMapping("/mastery-signals")
    public MasterySignalsSummaryDto getMasterySignalsSummary(@RequestParam(required = false) UUID subjectId,
                                                             @RequestParam(required = false) UUID classId,
                                                             @RequestParam(required = false) UUID classSubjectId) {
        return developmentService.getMasterySignalsSummary(subjectId, classId, classSubjectId);
    }
}
