package zw.co.zivai.core_backend.services;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import zw.co.zivai.core_backend.dtos.AssessmentResultDto;
import zw.co.zivai.core_backend.dtos.CreateAssessmentResultRequest;
import zw.co.zivai.core_backend.dtos.UpdateAssessmentResultRequest;
import zw.co.zivai.core_backend.exceptions.BadRequestException;
import zw.co.zivai.core_backend.exceptions.NotFoundException;
import zw.co.zivai.core_backend.models.lms.AssessmentAssignment;
import zw.co.zivai.core_backend.models.lms.AssessmentAttempt;
import zw.co.zivai.core_backend.models.lms.AssessmentResult;
import zw.co.zivai.core_backend.models.lms.AttemptAnswer;
import zw.co.zivai.core_backend.models.lms.User;
import zw.co.zivai.core_backend.repositories.AssessmentAssignmentRepository;
import zw.co.zivai.core_backend.repositories.AssessmentAttemptRepository;
import zw.co.zivai.core_backend.repositories.AssessmentEnrollmentRepository;
import zw.co.zivai.core_backend.repositories.AssessmentRepository;
import zw.co.zivai.core_backend.repositories.AssessmentResultRepository;
import zw.co.zivai.core_backend.repositories.AttemptAnswerRepository;
import zw.co.zivai.core_backend.repositories.UserRepository;

@Service
@RequiredArgsConstructor
public class AssessmentResultService {
    private final AssessmentResultRepository assessmentResultRepository;
    private final AssessmentAssignmentRepository assessmentAssignmentRepository;
    private final AssessmentRepository assessmentRepository;
    private final AssessmentEnrollmentRepository assessmentEnrollmentRepository;
    private final AssessmentAttemptRepository assessmentAttemptRepository;
    private final AttemptAnswerRepository attemptAnswerRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public List<AssessmentResultDto> list(UUID assessmentId, UUID studentId) {
        List<AssessmentAssignment> assignments = resolveAssignments(assessmentId, studentId);
        List<AssessmentResult> results = new ArrayList<>();

        for (AssessmentAssignment assignment : assignments) {
            if (studentId != null) {
                results.addAll(assessmentResultRepository
                    .findByAssessmentAssignment_IdAndStudent_Id(assignment.getId(), studentId));
            } else {
                results.addAll(assessmentResultRepository.findByAssessmentAssignment_Id(assignment.getId()));
            }
        }

        String assessmentIdForDto = assessmentId.toString();
        return results.stream()
            .map(result -> toDto(result, assessmentIdForDto, null))
            .toList();
    }

    public AssessmentResultDto create(UUID assessmentId, CreateAssessmentResultRequest request) {
        if (request.getStudent() == null) {
            throw new BadRequestException("Student is required");
        }

        UUID studentId = request.getStudent();
        AssessmentAssignment assignment = resolveSingleAssignment(assessmentId, studentId);
        User student = userRepository.findById(studentId)
            .orElseThrow(() -> new NotFoundException("Student not found: " + studentId));

        AssessmentResult result = assessmentResultRepository
            .findFirstByAssessmentAssignment_IdAndStudent_Id(assignment.getId(), studentId)
            .orElseGet(AssessmentResult::new);

        result.setAssessmentAssignment(assignment);
        result.setStudent(student);
        applyUpdates(result, request.getExpectedMark(), request.getActualMark(), request.getGrade(),
            request.getFeedback(), request.getSubmittedDate(), request.getStatus());

        linkLatestAttempt(result, assignment, studentId);

        AssessmentResult saved = assessmentResultRepository.save(result);
        return toDto(saved, assessmentId.toString(), request.getExternalAssessmentData());
    }

    public AssessmentResultDto update(UUID assessmentId, UUID resultId, UpdateAssessmentResultRequest request) {
        AssessmentResult result = assessmentResultRepository.findById(resultId)
            .orElseThrow(() -> new NotFoundException("Assessment result not found: " + resultId));

        applyUpdates(result, request.getExpectedMark(), request.getActualMark(), request.getGrade(),
            request.getFeedback(), request.getSubmittedDate(), request.getStatus());

        AssessmentResult saved = assessmentResultRepository.save(result);
        return toDto(saved, assessmentId.toString(), request.getExternalAssessmentData());
    }

    public void delete(UUID resultId) {
        AssessmentResult result = assessmentResultRepository.findById(resultId)
            .orElseThrow(() -> new NotFoundException("Assessment result not found: " + resultId));
        assessmentResultRepository.delete(result);
    }

    private void applyUpdates(AssessmentResult result,
                              Double expectedMark,
                              Double actualMark,
                              String grade,
                              String feedback,
                              Instant submittedDate,
                              String status) {
        if (expectedMark != null) {
            result.setExpectedMark(expectedMark);
        }
        if (actualMark != null) {
            result.setActualMark(actualMark);
        }
        if (grade != null) {
            result.setGrade(grade);
        }
        if (feedback != null) {
            result.setFeedback(feedback);
        }
        if (submittedDate != null) {
            result.setSubmittedAt(submittedDate);
        } else if (result.getSubmittedAt() == null) {
            result.setSubmittedAt(Instant.now());
        }
        if (result.getActualMark() != null || result.getGrade() != null) {
            result.setGradedAt(Instant.now());
        }
        if (status != null) {
            result.setStatus(status);
        } else if (result.getStatus() == null) {
            result.setStatus(result.getActualMark() != null ? "published" : "draft");
        }
    }

    private void linkLatestAttempt(AssessmentResult result, AssessmentAssignment assignment, UUID studentId) {
        assessmentEnrollmentRepository.findByAssessmentAssignment_IdAndStudent_Id(assignment.getId(), studentId)
            .flatMap(enrollment -> assessmentAttemptRepository
                .findTopByAssessmentEnrollment_IdOrderByAttemptNumberDesc(enrollment.getId()))
            .ifPresent(attempt -> {
                result.setFinalizedAttempt(attempt);
                if (result.getActualMark() != null) {
                    attempt.setFinalScore(result.getActualMark());
                }
                if (result.getGrade() != null) {
                    attempt.setFinalGrade(result.getGrade());
                }
                if (attempt.getGradingStatusCode() == null) {
                    attempt.setGradingStatusCode("auto_graded");
                }
                assessmentAttemptRepository.save(attempt);
            });
    }

    private List<AssessmentAssignment> resolveAssignments(UUID assessmentId, UUID studentId) {
        Optional<AssessmentAssignment> directAssignment = assessmentAssignmentRepository.findById(assessmentId);
        if (directAssignment.isPresent()) {
            return List.of(directAssignment.get());
        }

        if (assessmentRepository.findById(assessmentId).isEmpty()) {
            throw new NotFoundException("Assessment or assignment not found: " + assessmentId);
        }

        List<AssessmentAssignment> assignments = assessmentAssignmentRepository.findByAssessment_Id(assessmentId);
        if (assignments.isEmpty()) {
            return assignments;
        }

        if (studentId != null) {
            List<UUID> classIds = assessmentEnrollmentRepository.findByStudent_Id(studentId).stream()
                .map(enrollment -> enrollment.getAssessmentAssignment().getClassEntity())
                .filter(classEntity -> classEntity != null)
                .map(classEntity -> classEntity.getId())
                .toList();

            if (!classIds.isEmpty()) {
                List<AssessmentAssignment> scoped = assignments.stream()
                    .filter(assignment -> assignment.getClassEntity() != null && classIds.contains(assignment.getClassEntity().getId()))
                    .toList();
                if (!scoped.isEmpty()) {
                    return scoped;
                }
            }
        }

        return assignments;
    }

    private AssessmentAssignment resolveSingleAssignment(UUID assessmentId, UUID studentId) {
        List<AssessmentAssignment> assignments = resolveAssignments(assessmentId, studentId);
        return assignments.stream()
            .sorted(Comparator.comparing(AssessmentAssignment::getDueTime, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(AssessmentAssignment::getCreatedAt))
            .findFirst()
            .orElseThrow(() -> new NotFoundException("Assessment assignment not found for assessment: " + assessmentId));
    }

    private AssessmentResultDto toDto(AssessmentResult result, String assessmentIdForDto, Object externalOverride) {
        Object externalAssessmentData = externalOverride != null ? externalOverride : extractExternalData(result);

        return AssessmentResultDto.builder()
            .id(result.getId().toString())
            .student(result.getStudent().getId().toString())
            .assessment(assessmentIdForDto)
            .expectedMark(result.getExpectedMark())
            .actualMark(result.getActualMark())
            .grade(result.getGrade())
            .feedback(result.getFeedback())
            .submittedDate(result.getSubmittedAt())
            .createdAt(result.getCreatedAt())
            .updatedAt(result.getUpdatedAt())
            .externalAssessmentData(externalAssessmentData)
            .build();
    }

    private Object extractExternalData(AssessmentResult result) {
        AssessmentAttempt attempt = result.getFinalizedAttempt();
        if (attempt == null) {
            return null;
        }
        Optional<AttemptAnswer> answer = attemptAnswerRepository.findFirstByAssessmentAttempt_IdOrderByCreatedAtAsc(attempt.getId());
        if (answer.isEmpty() || answer.get().getExternalAssessmentData() == null) {
            return null;
        }

        String raw = answer.get().getExternalAssessmentData();
        try {
            return objectMapper.readValue(raw, Object.class);
        } catch (Exception ex) {
            return raw;
        }
    }
}
