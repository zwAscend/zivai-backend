package zw.co.zivai.core_backend.controllers.assessments;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

import lombok.RequiredArgsConstructor;
import zw.co.zivai.core_backend.dtos.assessments.AssessmentEnrollmentSummaryDto;
import zw.co.zivai.core_backend.dtos.assessments.BulkAssessmentEnrollmentRequest;
import zw.co.zivai.core_backend.dtos.assessments.CreateAssessmentAssignmentRequest;
import zw.co.zivai.core_backend.dtos.assessments.UpdateAssessmentAssignmentRequest;
import zw.co.zivai.core_backend.models.lms.AssessmentAssignment;
import zw.co.zivai.core_backend.models.lms.AssessmentEnrollment;
import zw.co.zivai.core_backend.services.assessments.AssessmentAssignmentService;
import zw.co.zivai.core_backend.services.assessments.AssessmentEnrollmentService;

@RestController
@RequestMapping("/api/assessment-assignments")
@RequiredArgsConstructor
public class AssessmentAssignmentController {
    private final AssessmentAssignmentService assessmentAssignmentService;
    private final AssessmentEnrollmentService assessmentEnrollmentService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AssessmentAssignment create(@RequestBody CreateAssessmentAssignmentRequest request) {
        return assessmentAssignmentService.create(request);
    }

    @GetMapping
    public List<AssessmentAssignment> list(@RequestParam(required = false) UUID assessmentId,
                                           @RequestParam(required = false) UUID classId,
                                           @RequestParam(required = false) Boolean published) {
        if (assessmentId != null) {
            return assessmentAssignmentService.listByAssessment(assessmentId);
        }
        if (classId != null) {
            return assessmentAssignmentService.listByClass(classId);
        }
        if (published != null) {
            return assessmentAssignmentService.listByPublished(published);
        }
        return assessmentAssignmentService.list();
    }

    @GetMapping("/{id}")
    public AssessmentAssignment get(@PathVariable UUID id) {
        return assessmentAssignmentService.get(id);
    }

    @PutMapping("/{id}")
    public AssessmentAssignment update(@PathVariable UUID id,
                                       @RequestBody UpdateAssessmentAssignmentRequest request) {
        return assessmentAssignmentService.update(id, request);
    }

    @PostMapping("/{id}/publish")
    public AssessmentAssignment publish(@PathVariable UUID id) {
        return assessmentAssignmentService.setPublished(id, true);
    }

    @PostMapping("/{id}/unpublish")
    public AssessmentAssignment unpublish(@PathVariable UUID id) {
        return assessmentAssignmentService.setPublished(id, false);
    }

    @PostMapping("/{id}/enrollments")
    @ResponseStatus(HttpStatus.CREATED)
    public List<AssessmentEnrollment> enrollStudents(@PathVariable UUID id,
                                                     @RequestBody BulkAssessmentEnrollmentRequest request) {
        return assessmentAssignmentService.enrollStudents(id, request);
    }

    @PostMapping("/{id}/enroll-class")
    @ResponseStatus(HttpStatus.CREATED)
    public List<AssessmentEnrollment> enrollClass(@PathVariable UUID id,
                                                  @RequestParam(required = false) UUID classId,
                                                  @RequestParam(required = false) String statusCode) {
        return assessmentAssignmentService.enrollClass(id, classId, statusCode);
    }

    @GetMapping("/{id}/enrollments")
    public List<AssessmentEnrollmentSummaryDto> listEnrollments(@PathVariable UUID id) {
        return assessmentEnrollmentService.listSummary(id, null, null);
    }
}
