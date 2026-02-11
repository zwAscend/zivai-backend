package zw.co.zivai.core_backend.controllers.assessments;

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
import zw.co.zivai.core_backend.dtos.assessments.CreateAttemptAnswerRequest;
import zw.co.zivai.core_backend.models.lms.AttemptAnswer;
import zw.co.zivai.core_backend.services.assessments.AttemptAnswerService;

@RestController
@RequestMapping("/api/attempt-answers")
@RequiredArgsConstructor
public class AttemptAnswerController {
    private final AttemptAnswerService attemptAnswerService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AttemptAnswer create(@RequestBody CreateAttemptAnswerRequest request) {
        return attemptAnswerService.create(request);
    }

    @GetMapping
    public List<AttemptAnswer> list() {
        return attemptAnswerService.list();
    }

    @GetMapping("/{id}")
    public AttemptAnswer get(@PathVariable UUID id) {
        return attemptAnswerService.get(id);
    }
}
