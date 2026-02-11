package zw.co.zivai.core_backend.controllers.assessments;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import zw.co.zivai.core_backend.dtos.assessments.AssessmentResultDto;
import zw.co.zivai.core_backend.dtos.assessments.CreateAssessmentResultRequest;
import zw.co.zivai.core_backend.dtos.assessments.UpdateAssessmentResultRequest;
import zw.co.zivai.core_backend.services.assessments.AssessmentResultService;

@RestController
@RequestMapping("/api/assessments/{assessmentId}/results")
@RequiredArgsConstructor
public class AssessmentResultController {
    private final AssessmentResultService assessmentResultService;

    @GetMapping
    public List<AssessmentResultDto> list(@PathVariable UUID assessmentId,
                                          @RequestParam(required = false) UUID studentId) {
        return assessmentResultService.list(assessmentId, studentId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AssessmentResultDto create(@PathVariable UUID assessmentId,
                                      @RequestBody CreateAssessmentResultRequest request) {
        return assessmentResultService.create(assessmentId, request);
    }

    @PutMapping("/{resultId}")
    public AssessmentResultDto update(@PathVariable UUID assessmentId,
                                      @PathVariable UUID resultId,
                                      @RequestBody UpdateAssessmentResultRequest request) {
        return assessmentResultService.update(assessmentId, resultId, request);
    }

    @DeleteMapping("/{resultId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID resultId) {
        assessmentResultService.delete(resultId);
    }
}
