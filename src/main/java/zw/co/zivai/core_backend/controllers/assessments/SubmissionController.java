package zw.co.zivai.core_backend.controllers.assessments;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import zw.co.zivai.core_backend.dtos.assessments.GradingStatsDto;
import zw.co.zivai.core_backend.dtos.assessments.ReviewSubmissionRequest;
import zw.co.zivai.core_backend.dtos.assessments.SubmissionDetailDto;
import zw.co.zivai.core_backend.dtos.assessments.SubmissionReviewDetailDto;
import zw.co.zivai.core_backend.dtos.assessments.SubmissionSummaryDto;
import zw.co.zivai.core_backend.dtos.assessments.SubmitAssessmentAnswersRequest;
import zw.co.zivai.core_backend.services.assessments.SubmissionService;

@RestController
@RequestMapping("/api/submissions")
@RequiredArgsConstructor
public class SubmissionController {
    private final SubmissionService submissionService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public SubmissionDetailDto submit(
        @RequestParam UUID assessmentId,
        @RequestParam UUID studentId,
        @RequestParam String submissionType,
        @RequestParam(required = false) String textContent,
        @RequestParam(required = false) String externalAssessmentData,
        @RequestParam(required = false) UUID result,
        @RequestParam(required = false) String originalFilename,
        @RequestParam(required = false) String fileType,
        @RequestParam(required = false) MultipartFile file
    ) {
        return submissionService.submit(
            assessmentId,
            studentId,
            submissionType,
            textContent,
            externalAssessmentData,
            result,
            originalFilename,
            fileType,
            file
        );
    }

    @PostMapping("/answers")
    @ResponseStatus(HttpStatus.CREATED)
    public SubmissionDetailDto submitAnswers(@RequestBody SubmitAssessmentAnswersRequest request) {
        return submissionService.submitAnswers(request);
    }

    @GetMapping("/{submissionId}")
    public SubmissionDetailDto getSubmission(@PathVariable UUID submissionId) {
        return submissionService.getSubmission(submissionId);
    }

    @GetMapping("/{submissionId}/review-detail")
    public SubmissionReviewDetailDto getSubmissionReviewDetail(@PathVariable UUID submissionId) {
        return submissionService.getSubmissionReviewDetail(submissionId);
    }

    @GetMapping("/student/{studentId}")
    public List<SubmissionSummaryDto> getStudentSubmissions(@PathVariable UUID studentId) {
        return submissionService.getStudentSubmissions(studentId);
    }

    @PutMapping("/{submissionId}/review")
    public SubmissionDetailDto reviewSubmission(@PathVariable UUID submissionId,
                                                @RequestBody ReviewSubmissionRequest request) {
        return submissionService.reviewSubmission(submissionId, request);
    }

    @GetMapping("/teacher/pending")
    public List<SubmissionDetailDto> getPendingSubmissions(@RequestParam(required = false) UUID teacherId,
                                                           @RequestParam(required = false) UUID subjectId,
                                                           @RequestParam(required = false) UUID classId,
                                                           @RequestParam(required = false) UUID assessmentId,
                                                           @RequestParam(required = false) UUID studentId,
                                                           @RequestParam(required = false) String status,
                                                           @RequestParam(required = false) Integer page,
                                                           @RequestParam(required = false) Integer size) {
        return submissionService.getPendingSubmissions(
            teacherId,
            subjectId,
            classId,
            assessmentId,
            studentId,
            status,
            page,
            size
        );
    }

    @GetMapping("/stats")
    public GradingStatsDto getStats(@RequestParam(required = false) UUID subjectId,
                                    @RequestParam(required = false) String timeframe) {
        return submissionService.getStats(subjectId);
    }
}
