package zw.co.zivai.core_backend.controllers;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
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
import zw.co.zivai.core_backend.dtos.AssessmentWithQuestionsDto;
import zw.co.zivai.core_backend.dtos.CreateAssessmentRequest;
import zw.co.zivai.core_backend.dtos.UpdateAssessmentRequest;
import zw.co.zivai.core_backend.models.lms.Assessment;
import zw.co.zivai.core_backend.services.AssessmentService;

@RestController
@RequestMapping("/api/assessments")
@RequiredArgsConstructor
public class AssessmentController {
    private final AssessmentService assessmentService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Assessment create(@RequestBody CreateAssessmentRequest request) {
        return assessmentService.create(request);
    }

    @GetMapping
    public List<Assessment> list(@RequestParam(required = false) UUID subjectId,
                                 @RequestParam(required = false) String status) {
        return assessmentService.list(subjectId, status);
    }

    @GetMapping("/subject/{subjectId}")
    public List<Assessment> listBySubject(@PathVariable UUID subjectId,
                                          @RequestParam(required = false) String status) {
        return assessmentService.list(subjectId, status);
    }

    @GetMapping("/{id}")
    public Assessment get(@PathVariable UUID id) {
        return assessmentService.get(id);
    }

    @GetMapping("/{id}/with-questions")
    public AssessmentWithQuestionsDto getWithQuestions(@PathVariable UUID id) {
        return assessmentService.getWithQuestions(id);
    }

    @PutMapping("/{id}")
    public Assessment update(@PathVariable UUID id, @RequestBody UpdateAssessmentRequest request) {
        return assessmentService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public Map<String, String> delete(@PathVariable UUID id) {
        assessmentService.delete(id);
        return Map.of("message", "Assessment archived");
    }
}
