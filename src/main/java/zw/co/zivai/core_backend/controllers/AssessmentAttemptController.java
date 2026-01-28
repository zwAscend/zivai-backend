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
import zw.co.zivai.core_backend.dtos.CreateAssessmentAttemptRequest;
import zw.co.zivai.core_backend.models.lms.AssessmentAttempt;
import zw.co.zivai.core_backend.services.AssessmentAttemptService;

@RestController
@RequestMapping("/api/assessment-attempts")
@RequiredArgsConstructor
public class AssessmentAttemptController {
    private final AssessmentAttemptService assessmentAttemptService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AssessmentAttempt create(@RequestBody CreateAssessmentAttemptRequest request) {
        return assessmentAttemptService.create(request);
    }

    @GetMapping
    public List<AssessmentAttempt> list() {
        return assessmentAttemptService.list();
    }

    @GetMapping("/{id}")
    public AssessmentAttempt get(@PathVariable UUID id) {
        return assessmentAttemptService.get(id);
    }
}
