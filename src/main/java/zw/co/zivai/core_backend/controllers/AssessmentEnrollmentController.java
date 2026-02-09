package zw.co.zivai.core_backend.controllers;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import zw.co.zivai.core_backend.dtos.AssessmentEnrollmentSummaryDto;
import zw.co.zivai.core_backend.dtos.CreateAssessmentEnrollmentRequest;
import zw.co.zivai.core_backend.models.lms.AssessmentEnrollment;
import zw.co.zivai.core_backend.services.AssessmentEnrollmentService;

@RestController
@RequestMapping("/api/assessment-enrollments")
@RequiredArgsConstructor
public class AssessmentEnrollmentController {
    private final AssessmentEnrollmentService assessmentEnrollmentService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AssessmentEnrollment create(@RequestBody CreateAssessmentEnrollmentRequest request) {
        return assessmentEnrollmentService.create(request);
    }

    @GetMapping
    public List<AssessmentEnrollment> list() {
        return assessmentEnrollmentService.list();
    }

    @GetMapping("/summary")
    public List<AssessmentEnrollmentSummaryDto> listSummary(@RequestParam(required = false) UUID assignmentId,
                                                            @RequestParam(required = false) UUID studentId,
                                                            @RequestParam(required = false) UUID classId) {
        return assessmentEnrollmentService.listSummary(assignmentId, studentId, classId);
    }

    @GetMapping("/{id}")
    public AssessmentEnrollment get(@PathVariable UUID id) {
        return assessmentEnrollmentService.get(id);
    }
}
