package zw.co.zivai.core_backend.services;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import zw.co.zivai.core_backend.dtos.GradingStatsDto;
import zw.co.zivai.core_backend.dtos.ReviewSubmissionRequest;
import zw.co.zivai.core_backend.dtos.SubmissionDetailDto;
import zw.co.zivai.core_backend.dtos.SubmissionSummaryDto;
import zw.co.zivai.core_backend.exceptions.BadRequestException;
import zw.co.zivai.core_backend.exceptions.NotFoundException;
import zw.co.zivai.core_backend.models.lms.AnswerAttachment;
import zw.co.zivai.core_backend.models.lms.Assessment;
import zw.co.zivai.core_backend.models.lms.AssessmentAssignment;
import zw.co.zivai.core_backend.models.lms.AssessmentAttempt;
import zw.co.zivai.core_backend.models.lms.AssessmentEnrollment;
import zw.co.zivai.core_backend.models.lms.AssessmentQuestion;
import zw.co.zivai.core_backend.models.lms.AssessmentResult;
import zw.co.zivai.core_backend.models.lms.AttemptAnswer;
import zw.co.zivai.core_backend.models.lms.GradingOverride;
import zw.co.zivai.core_backend.models.lms.Question;
import zw.co.zivai.core_backend.models.lms.User;
import zw.co.zivai.core_backend.repositories.AnswerAttachmentRepository;
import zw.co.zivai.core_backend.repositories.AssessmentAssignmentRepository;
import zw.co.zivai.core_backend.repositories.AssessmentAttemptRepository;
import zw.co.zivai.core_backend.repositories.AssessmentEnrollmentRepository;
import zw.co.zivai.core_backend.repositories.AssessmentQuestionRepository;
import zw.co.zivai.core_backend.repositories.AssessmentRepository;
import zw.co.zivai.core_backend.repositories.AssessmentResultRepository;
import zw.co.zivai.core_backend.repositories.AttemptAnswerRepository;
import zw.co.zivai.core_backend.repositories.GradingOverrideRepository;
import zw.co.zivai.core_backend.repositories.QuestionRepository;
import zw.co.zivai.core_backend.repositories.UserRepository;

@Service
@RequiredArgsConstructor
public class SubmissionService {
    private final AssessmentAssignmentRepository assessmentAssignmentRepository;
    private final AssessmentRepository assessmentRepository;
    private final AssessmentEnrollmentRepository assessmentEnrollmentRepository;
    private final AssessmentAttemptRepository assessmentAttemptRepository;
    private final AssessmentQuestionRepository assessmentQuestionRepository;
    private final AttemptAnswerRepository attemptAnswerRepository;
    private final AnswerAttachmentRepository answerAttachmentRepository;
    private final AssessmentResultRepository assessmentResultRepository;
    private final QuestionRepository questionRepository;
    private final GradingOverrideRepository gradingOverrideRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public SubmissionDetailDto submit(UUID assessmentId,
                                      UUID studentId,
                                      String submissionType,
                                      String textContent,
                                      String externalAssessmentData,
                                      UUID resultId,
                                      String originalFilename,
                                      String fileType,
                                      MultipartFile file) {
        if (assessmentId == null || studentId == null) {
            throw new BadRequestException("assessmentId and studentId are required");
        }
        if (submissionType == null) {
            throw new BadRequestException("submissionType is required");
        }

        User student = userRepository.findById(studentId)
            .orElseThrow(() -> new NotFoundException("Student not found: " + studentId));

        AssessmentAssignment assignment = resolveSingleAssignment(assessmentId, studentId);
        Assessment assessment = assignment.getAssessment();

        AssessmentEnrollment enrollment = assessmentEnrollmentRepository
            .findByAssessmentAssignment_IdAndStudent_Id(assignment.getId(), studentId)
            .orElseGet(() -> createEnrollment(assignment, student));

        AssessmentAttempt attempt = new AssessmentAttempt();
        attempt.setAssessmentEnrollment(enrollment);
        attempt.setAttemptNumber(nextAttemptNumber(enrollment.getId()));
        attempt.setStartedAt(Instant.now());
        attempt.setSubmittedAt(Instant.now());
        attempt.setMaxScore(assessment.getMaxScore());
        attempt.setSubmissionType(submissionType);

        if (originalFilename != null) {
            attempt.setOriginalFilename(originalFilename);
        } else if (file != null && file.getOriginalFilename() != null) {
            attempt.setOriginalFilename(file.getOriginalFilename());
        }
        if (fileType != null) {
            attempt.setFileType(fileType);
        } else if (file != null && file.getContentType() != null) {
            attempt.setFileType(file.getContentType());
        }
        if (file != null) {
            attempt.setFileSizeBytes(file.getSize());
        }

        ExternalAssessmentMetrics metrics = parseExternalAssessmentData(externalAssessmentData);
        if (metrics != null) {
            attempt.setTotalScore(metrics.marksAchieved);
            attempt.setAiConfidence(metrics.confidence);
            if (metrics.totalPossible != null) {
                attempt.setMaxScore(metrics.totalPossible);
            }
        }

        if (resultId != null) {
            assessmentResultRepository.findById(resultId).ifPresent(result -> {
                attempt.setFinalScore(result.getActualMark());
                attempt.setFinalGrade(result.getGrade());
                if (attempt.getTotalScore() == null) {
                    attempt.setTotalScore(result.getActualMark());
                }
            });
        }

        if (attempt.getTotalScore() != null) {
            attempt.setGradingStatusCode("auto_graded");
        } else {
            attempt.setGradingStatusCode("pending");
        }

        AssessmentAttempt savedAttempt = assessmentAttemptRepository.save(attempt);
        enrollment.setStatusCode("completed");
        assessmentEnrollmentRepository.save(enrollment);

        AssessmentQuestion assessmentQuestion = ensureAssessmentQuestion(assessment);

        AttemptAnswer answer = new AttemptAnswer();
        answer.setAssessmentAttempt(savedAttempt);
        answer.setAssessmentQuestion(assessmentQuestion);
        answer.setStudentAnswerText(textContent);
        answer.setTextContent(textContent);
        answer.setSubmissionType(submissionType);
        answer.setExternalAssessmentData(parseJsonNode(externalAssessmentData));
        answer.setMaxScore(attempt.getMaxScore() != null ? attempt.getMaxScore() : assessment.getMaxScore());

        if (metrics != null) {
            answer.setAiScore(metrics.marksAchieved);
            answer.setAiConfidence(metrics.confidence);
            answer.setFeedbackText(metrics.overallFeedback);
            answer.setGradedAt(Instant.now());
        }

        AttemptAnswer savedAnswer = attemptAnswerRepository.save(answer);

        if (file != null) {
            AnswerAttachment attachment = new AnswerAttachment();
            attachment.setAttemptAnswer(savedAnswer);
            attachment.setFileName(file.getOriginalFilename());
            attachment.setMimeType(file.getContentType());
            attachment.setSizeBytes(file.getSize());
            answerAttachmentRepository.save(attachment);
        }

        if (resultId != null) {
            assessmentResultRepository.findById(resultId).ifPresent(result -> {
                result.setFinalizedAttempt(savedAttempt);
                if (result.getSubmittedAt() == null) {
                    result.setSubmittedAt(savedAttempt.getSubmittedAt());
                }
                assessmentResultRepository.save(result);
            });
        }

        return toDetailDto(savedAttempt);
    }

    public SubmissionDetailDto getSubmission(UUID submissionId) {
        AssessmentAttempt attempt = assessmentAttemptRepository.findById(submissionId)
            .orElseThrow(() -> new NotFoundException("Submission not found: " + submissionId));
        return toDetailDto(attempt);
    }

    public List<SubmissionSummaryDto> getStudentSubmissions(UUID studentId) {
        List<AssessmentAttempt> attempts = assessmentAttemptRepository.findByAssessmentEnrollment_Student_Id(studentId).stream()
            .filter(attempt -> attempt.getSubmittedAt() != null)
            .toList();
        return attempts.stream()
            .map(this::toSummaryDto)
            .toList();
    }

    public List<SubmissionDetailDto> getPendingSubmissions() {
        List<AssessmentAttempt> attempts = assessmentAttemptRepository.findAll().stream()
            .filter(attempt -> attempt.getSubmittedAt() != null)
            .filter(attempt -> !"reviewed".equalsIgnoreCase(attempt.getGradingStatusCode()))
            .toList();

        return attempts.stream()
            .map(this::toDetailDto)
            .toList();
    }

    public GradingStatsDto getStats(UUID subjectId) {
        List<AssessmentAttempt> attempts = assessmentAttemptRepository.findAll();
        if (subjectId != null) {
            attempts = attempts.stream()
                .filter(attempt -> {
                    AssessmentAssignment assignment = attempt.getAssessmentEnrollment().getAssessmentAssignment();
                    return assignment.getAssessment().getSubject().getId().equals(subjectId);
                })
                .toList();
        }

        long total = attempts.size();
        long autoGraded = attempts.stream().filter(a -> "auto_graded".equalsIgnoreCase(a.getGradingStatusCode())).count();
        long reviewed = attempts.stream().filter(a -> "reviewed".equalsIgnoreCase(a.getGradingStatusCode())).count();

        double avgScore = attempts.stream()
            .filter(a -> a.getTotalScore() != null)
            .mapToDouble(AssessmentAttempt::getTotalScore)
            .average()
            .orElse(0.0);

        double avgConfidence = attempts.stream()
            .filter(a -> a.getAiConfidence() != null)
            .mapToDouble(AssessmentAttempt::getAiConfidence)
            .average()
            .orElse(0.0);

        return GradingStatsDto.builder()
            .totalSubmissions(total)
            .autoGradedCount(autoGraded)
            .teacherReviewedCount(reviewed)
            .averageScore(avgScore)
            .averageConfidence(avgConfidence)
            .build();
    }

    public SubmissionDetailDto reviewSubmission(UUID submissionId, ReviewSubmissionRequest request) {
        AssessmentAttempt attempt = assessmentAttemptRepository.findById(submissionId)
            .orElseThrow(() -> new NotFoundException("Submission not found: " + submissionId));

        Double finalScore = request.getFinalScore();
        String finalGrade = request.getFinalGrade();

        if (finalScore == null && attempt.getTotalScore() == null) {
            throw new BadRequestException("finalScore is required when no auto-graded score exists");
        }

        if (finalScore != null) {
            attempt.setFinalScore(finalScore);
        }
        if (finalGrade != null) {
            attempt.setFinalGrade(finalGrade);
        }
        attempt.setGradingStatusCode("reviewed");
        assessmentAttemptRepository.save(attempt);

        AttemptAnswer answer = attemptAnswerRepository.findFirstByAssessmentAttempt_IdOrderByCreatedAtAsc(attempt.getId())
            .orElseThrow(() -> new NotFoundException("Attempt answer not found for submission: " + submissionId));

        Double overrideScore = finalScore != null ? finalScore : attempt.getTotalScore();

        GradingOverride override = new GradingOverride();
        override.setAttemptAnswer(answer);
        override.setTeacher(attempt.getAssessmentEnrollment().getAssessmentAssignment().getAssignedBy());
        override.setOldScore(attempt.getTotalScore());
        override.setNewScore(overrideScore);
        override.setReason(request.getFeedbackAdjustment());
        gradingOverrideRepository.save(override);

        AssessmentResult result = findResultForAttempt(attempt);
        if (result != null) {
            result.setActualMark(overrideScore);
            if (finalGrade != null) {
                result.setGrade(finalGrade);
            }
            result.setGradedAt(Instant.now());
            result.setStatus("published");
            assessmentResultRepository.save(result);
        }

        return toDetailDto(attempt);
    }

    private AssessmentEnrollment createEnrollment(AssessmentAssignment assignment, User student) {
        AssessmentEnrollment enrollment = new AssessmentEnrollment();
        enrollment.setAssessmentAssignment(assignment);
        enrollment.setStudent(student);
        enrollment.setStatusCode("assigned");
        return assessmentEnrollmentRepository.save(enrollment);
    }

    private int nextAttemptNumber(UUID enrollmentId) {
        return assessmentAttemptRepository.findTopByAssessmentEnrollment_IdOrderByAttemptNumberDesc(enrollmentId)
            .map(attempt -> attempt.getAttemptNumber() + 1)
            .orElse(1);
    }

    private AssessmentAssignment resolveSingleAssignment(UUID assessmentId, UUID studentId) {
        Optional<AssessmentAssignment> directAssignment = assessmentAssignmentRepository.findById(assessmentId);
        if (directAssignment.isPresent()) {
            return directAssignment.get();
        }

        Assessment assessment = assessmentRepository.findById(assessmentId)
            .orElseThrow(() -> new NotFoundException("Assessment or assignment not found: " + assessmentId));

        List<AssessmentAssignment> assignments = assessmentAssignmentRepository.findByAssessment_Id(assessment.getId());
        if (assignments.isEmpty()) {
            throw new NotFoundException("Assessment assignment not found for assessment: " + assessmentId);
        }

        return assignments.stream()
            .sorted(Comparator.comparing(AssessmentAssignment::getDueTime, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(AssessmentAssignment::getCreatedAt))
            .findFirst()
            .orElseThrow(() -> new NotFoundException("Assessment assignment not found for assessment: " + assessmentId));
    }

    private AssessmentQuestion ensureAssessmentQuestion(Assessment assessment) {
        Optional<AssessmentQuestion> existing = assessmentQuestionRepository
            .findFirstByAssessment_IdOrderBySequenceIndexAsc(assessment.getId());
        if (existing.isPresent()) {
            return existing.get();
        }

        Question question = new Question();
        question.setSubject(assessment.getSubject());
        question.setAuthor(assessment.getCreatedBy());
        question.setCode("SUBMISSION");
        question.setStem("Submission content");
        question.setQuestionTypeCode("essay");
        question.setMaxMark(assessment.getMaxScore());
        question.setActive(true);
        Question savedQuestion = questionRepository.save(question);

        AssessmentQuestion assessmentQuestion = new AssessmentQuestion();
        assessmentQuestion.setAssessment(assessment);
        assessmentQuestion.setQuestion(savedQuestion);
        assessmentQuestion.setSequenceIndex(1);
        assessmentQuestion.setPoints(assessment.getMaxScore());
        return assessmentQuestionRepository.save(assessmentQuestion);
    }

    private SubmissionSummaryDto toSummaryDto(AssessmentAttempt attempt) {
        AssessmentAssignment assignment = attempt.getAssessmentEnrollment().getAssessmentAssignment();
        Assessment assessment = assignment.getAssessment();

        return SubmissionSummaryDto.builder()
            .id(attempt.getId().toString())
            .student(attempt.getAssessmentEnrollment().getStudent().getId().toString())
            .assessment(assessment.getId().toString())
            .submissionType(attempt.getSubmissionType())
            .submittedAt(attempt.getSubmittedAt())
            .status(statusForAttempt(attempt))
            .originalFilename(attempt.getOriginalFilename())
            .build();
    }

    private SubmissionDetailDto toDetailDto(AssessmentAttempt attempt) {
        AssessmentAssignment assignment = attempt.getAssessmentEnrollment().getAssessmentAssignment();
        Assessment assessment = assignment.getAssessment();

        AttemptAnswer answer = attemptAnswerRepository.findFirstByAssessmentAttempt_IdOrderByCreatedAtAsc(attempt.getId())
            .orElse(null);

        GradingOverride override = null;
        if (answer != null) {
            override = gradingOverrideRepository.findTopByAttemptAnswer_IdOrderByOverriddenAtDesc(answer.getId())
                .orElse(null);
        }

        Object externalData = parseExternalJson(answer != null ? answer.getExternalAssessmentData() : null);
        Double totalScore = attempt.getTotalScore();
        Double maxScore = assessment.getMaxScore();
        Double percentage = (totalScore != null && maxScore != null && maxScore > 0)
            ? (totalScore / maxScore) * 100
            : null;

        String grade = attempt.getFinalGrade();
        if (grade == null) {
            AssessmentResult result = findResultForAttempt(attempt);
            if (result != null) {
                grade = result.getGrade();
            }
        }

        String feedback = null;
        if (answer != null) {
            feedback = answer.getFeedbackText();
        }
        if (feedback == null) {
            AssessmentResult result = findResultForAttempt(attempt);
            if (result != null) {
                feedback = result.getFeedback();
            }
        }

        SubmissionDetailDto.StudentSummary studentSummary = SubmissionDetailDto.StudentSummary.builder()
            .id(attempt.getAssessmentEnrollment().getStudent().getId().toString())
            .firstName(attempt.getAssessmentEnrollment().getStudent().getFirstName())
            .lastName(attempt.getAssessmentEnrollment().getStudent().getLastName())
            .email(attempt.getAssessmentEnrollment().getStudent().getEmail())
            .build();

        SubmissionDetailDto.AssessmentSummary assessmentSummary = SubmissionDetailDto.AssessmentSummary.builder()
            .id(assessment.getId().toString())
            .name(assessment.getName())
            .description(assessment.getDescription())
            .type(assessment.getAssessmentType())
            .maxScore(assessment.getMaxScore())
            .weight(assessment.getWeightPct())
            .dueDate(assignment.getDueTime())
            .build();

        SubmissionDetailDto.AutoGradingResult autoResult = SubmissionDetailDto.AutoGradingResult.builder()
            .totalScore(totalScore)
            .percentage(percentage)
            .grade(grade)
            .feedback(feedback)
            .breakdown(externalData)
            .confidence(attempt.getAiConfidence())
            .gradedAt(attempt.getSubmittedAt())
            .build();

        SubmissionDetailDto.AutoGrading autoGrading = SubmissionDetailDto.AutoGrading.builder()
            .result(autoResult)
            .build();

        SubmissionDetailDto.TeacherReview teacherReview = null;
        if (override != null || attempt.getFinalScore() != null || attempt.getFinalGrade() != null) {
            Double finalScore = attempt.getFinalScore();
            if (finalScore == null) {
                finalScore = totalScore;
            }
            String finalGrade = attempt.getFinalGrade();
            if (finalGrade == null) {
                finalGrade = grade;
            }
            Double scoreAdjustment = null;
            if (finalScore != null && totalScore != null) {
                scoreAdjustment = finalScore - totalScore;
            }

            SubmissionDetailDto.ReviewAdjustments adjustments = SubmissionDetailDto.ReviewAdjustments.builder()
                .scoreAdjustment(scoreAdjustment)
                .feedbackAdjustment(override != null ? override.getReason() : null)
                .finalScore(finalScore)
                .finalGrade(finalGrade)
                .build();

            teacherReview = SubmissionDetailDto.TeacherReview.builder()
                .reviewed(true)
                .reviewedAt(override != null ? override.getOverriddenAt() : null)
                .adjustments(adjustments)
                .build();
        }

        return SubmissionDetailDto.builder()
            .id(attempt.getId().toString())
            .student(studentSummary)
            .assessment(assessmentSummary)
            .submissionType(attempt.getSubmissionType())
            .submittedAt(attempt.getSubmittedAt())
            .status(statusForAttempt(attempt))
            .autoGrading(autoGrading)
            .teacherReview(teacherReview)
            .submissionContent(resolveSubmissionContent(attempt, answer))
            .externalAssessmentData(externalData)
            .build();
    }

    private String resolveSubmissionContent(AssessmentAttempt attempt, AttemptAnswer answer) {
        if ("text".equalsIgnoreCase(attempt.getSubmissionType())) {
            if (answer != null && answer.getTextContent() != null) {
                return answer.getTextContent();
            }
            return answer != null ? answer.getStudentAnswerText() : null;
        }
        return attempt.getOriginalFilename();
    }

    private String statusForAttempt(AssessmentAttempt attempt) {
        if ("reviewed".equalsIgnoreCase(attempt.getGradingStatusCode())) {
            return "reviewed";
        }
        if ("auto_graded".equalsIgnoreCase(attempt.getGradingStatusCode())) {
            return "graded";
        }
        return "submitted";
    }

    private AssessmentResult findResultForAttempt(AssessmentAttempt attempt) {
        return assessmentResultRepository.findFirstByAssessmentAssignment_IdAndStudent_Id(
            attempt.getAssessmentEnrollment().getAssessmentAssignment().getId(),
            attempt.getAssessmentEnrollment().getStudent().getId()
        ).orElse(null);
    }

    private Object parseExternalJson(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        try {
            return objectMapper.convertValue(node, Object.class);
        } catch (Exception ex) {
            return node.toString();
        }
    }

    private ExternalAssessmentMetrics parseExternalAssessmentData(String externalAssessmentData) {
        JsonNode root = parseJsonNode(externalAssessmentData);
        if (root == null) {
            return null;
        }

        try {
            JsonNode assessmentNode = root.path("assessment");

            Double marksAchieved = toDouble(assessmentNode.path("marks_achieved"));
            Double totalPossible = toDouble(assessmentNode.path("total_possible_marks"));
            Double confidence = toDouble(assessmentNode.path("confidence_assessment_score"));
            String overallFeedback = assessmentNode.path("overall_feedback").isMissingNode()
                ? null
                : assessmentNode.path("overall_feedback").asText();

            return new ExternalAssessmentMetrics(marksAchieved, totalPossible, confidence, overallFeedback);
        } catch (Exception ex) {
            return null;
        }
    }

    private Double toDouble(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        if (node.isNumber()) {
            return node.asDouble();
        }
        if (node.isTextual()) {
            try {
                return Double.parseDouble(node.asText());
            } catch (NumberFormatException ex) {
                return null;
            }
        }
        return null;
    }

    private JsonNode parseJsonNode(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(value);
        } catch (Exception ex) {
            return objectMapper.getNodeFactory().textNode(value);
        }
    }

    private static class ExternalAssessmentMetrics {
        private final Double marksAchieved;
        private final Double totalPossible;
        private final Double confidence;
        private final String overallFeedback;

        private ExternalAssessmentMetrics(Double marksAchieved, Double totalPossible, Double confidence, String overallFeedback) {
            this.marksAchieved = marksAchieved;
            this.totalPossible = totalPossible;
            this.confidence = confidence;
            this.overallFeedback = overallFeedback;
        }
    }
}
