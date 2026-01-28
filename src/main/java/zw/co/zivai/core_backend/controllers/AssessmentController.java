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
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import zw.co.zivai.core_backend.dtos.CreateAssessmentRequest;
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
    public List<Assessment> list() {
        return assessmentService.list();
    }

    @GetMapping("/{id}")
    public Assessment get(@PathVariable UUID id) {
        return assessmentService.get(id);
    }
}
