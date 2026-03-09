package zw.co.zivai.core_backend.common.services.students;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.RequiredArgsConstructor;
import zw.co.zivai.core_backend.common.dtos.students.StudentAssessmentDetailDto;
import zw.co.zivai.core_backend.common.dtos.students.StudentActivityFeedItemDto;
import zw.co.zivai.core_backend.common.dtos.students.StudentAssessmentHistoryItemDto;
import zw.co.zivai.core_backend.common.dtos.students.StudentPracticeAnswerRequest;
import zw.co.zivai.core_backend.common.dtos.students.StudentPracticeAnswerResultDto;
import zw.co.zivai.core_backend.common.dtos.students.StudentPracticeQuestionDto;
import zw.co.zivai.core_backend.common.dtos.students.StudentPracticeSessionDto;
import zw.co.zivai.core_backend.common.dtos.students.StudentPlanRuntimeProgressDto;
import zw.co.zivai.core_backend.common.dtos.students.StudentPlanRuntimeProgressRequest;
import zw.co.zivai.core_backend.common.dtos.students.StartStudentPracticeSessionRequest;
import zw.co.zivai.core_backend.common.dtos.students.StudentDto;
import zw.co.zivai.core_backend.common.dtos.students.StudentChallengeEligibilityDto;
import zw.co.zivai.core_backend.common.dtos.students.StudentSubjectOverviewDto;
import zw.co.zivai.core_backend.common.dtos.students.StudentSubjectOverviewTopicDto;
import zw.co.zivai.core_backend.common.dtos.students.StudentSubjectOverviewUnitDto;
import zw.co.zivai.core_backend.common.dtos.students.StudentTeacherDto;
import zw.co.zivai.core_backend.common.dtos.assessments.TopicAnswerStat;
import zw.co.zivai.core_backend.common.exceptions.BadRequestException;
import zw.co.zivai.core_backend.common.exceptions.NotFoundException;
import zw.co.zivai.core_backend.common.models.lms.classroom.ClassSubject;
import zw.co.zivai.core_backend.common.models.lms.classroom.ClassEntity;
import zw.co.zivai.core_backend.common.models.lms.assessments.Assessment;
import zw.co.zivai.core_backend.common.models.lms.assessments.AssessmentAssignment;
import zw.co.zivai.core_backend.common.models.lms.assessments.AssessmentAttempt;
import zw.co.zivai.core_backend.common.models.lms.assessments.AssessmentEnrollment;
import zw.co.zivai.core_backend.common.models.lms.assessments.AssessmentQuestion;
import zw.co.zivai.core_backend.common.models.lms.assessments.AssessmentResult;
import zw.co.zivai.core_backend.common.models.lms.assessments.AttemptAnswer;
import zw.co.zivai.core_backend.common.models.lms.development.PlanStep;
import zw.co.zivai.core_backend.common.models.lms.resources.Question;
import zw.co.zivai.core_backend.common.models.lms.school.School;
import zw.co.zivai.core_backend.common.models.lms.students.Enrolment;
import zw.co.zivai.core_backend.common.models.lms.students.StudentAttribute;
import zw.co.zivai.core_backend.common.models.lms.students.StudentPlan;
import zw.co.zivai.core_backend.common.models.lms.students.StudentProfile;
import zw.co.zivai.core_backend.common.models.lms.students.StudentSubjectEnrolment;
import zw.co.zivai.core_backend.common.models.lms.subjects.Subject;
import zw.co.zivai.core_backend.common.models.lms.resources.Topic;
import zw.co.zivai.core_backend.common.models.lms.users.User;
import zw.co.zivai.core_backend.common.repositories.assessments.AttemptAnswerRepository;
import zw.co.zivai.core_backend.common.repositories.assessments.AssessmentAssignmentRepository;
import zw.co.zivai.core_backend.common.repositories.assessments.AssessmentAttemptRepository;
import zw.co.zivai.core_backend.common.repositories.assessments.AssessmentEnrollmentRepository;
import zw.co.zivai.core_backend.common.repositories.assessments.AssessmentQuestionRepository;
import zw.co.zivai.core_backend.common.repositories.assessments.AssessmentRepository;
import zw.co.zivai.core_backend.common.repositories.assessments.AssessmentResultRepository;
import zw.co.zivai.core_backend.common.repositories.classroom.ClassSubjectRepository;
import zw.co.zivai.core_backend.common.repositories.classroom.EnrolmentRepository;
import zw.co.zivai.core_backend.common.repositories.assessments.QuestionRepository;
import zw.co.zivai.core_backend.common.repositories.development.PlanStepRepository;
import zw.co.zivai.core_backend.common.repositories.subject.SubjectRepository;
import zw.co.zivai.core_backend.common.repositories.subject.TopicRepository;
import zw.co.zivai.core_backend.common.repositories.development.StudentAttributeRepository;
import zw.co.zivai.core_backend.common.repositories.development.StudentPlanRepository;
import zw.co.zivai.core_backend.common.repositories.school.SchoolRepository;
import zw.co.zivai.core_backend.common.repositories.students.StudentProfileRepository;
import zw.co.zivai.core_backend.common.repositories.classroom.StudentSubjectEnrolmentRepository;
import zw.co.zivai.core_backend.common.repositories.user.UserRepository;

@Service
@RequiredArgsConstructor
public class StudentService {
    private static final int TOPICS_PER_UNIT_FALLBACK = 4;
    private static final int MIN_QUESTIONS_FOR_SUBJECT_CHALLENGE = 6;
    private static final Pattern UNIT_PATTERN = Pattern.compile("unit[\\s\\-_]*(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern UNIT_PREFIX_PATTERN = Pattern.compile("\\bu[\\s\\-_]*(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Instant OPEN_START_INSTANT = Instant.EPOCH;
    private static final Instant OPEN_END_INSTANT = Instant.parse("9999-12-31T23:59:59Z");
    private static final int DEFAULT_PRACTICE_QUESTION_COUNT = 8;
    private static final int MAX_PRACTICE_QUESTION_COUNT = 30;
    private static final Set<String> PRACTICE_SESSION_MODES = Set.of("topic_practice", "topic_challenge", "subject_challenge");

    private final UserRepository userRepository;
    private final SubjectRepository subjectRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final StudentAttributeRepository studentAttributeRepository;
    private final StudentSubjectEnrolmentRepository studentSubjectEnrolmentRepository;
    private final EnrolmentRepository enrolmentRepository;
    private final ClassSubjectRepository classSubjectRepository;
    private final TopicRepository topicRepository;
    private final QuestionRepository questionRepository;
    private final AttemptAnswerRepository attemptAnswerRepository;
    private final AssessmentRepository assessmentRepository;
    private final AssessmentAssignmentRepository assessmentAssignmentRepository;
    private final AssessmentQuestionRepository assessmentQuestionRepository;
    private final AssessmentEnrollmentRepository assessmentEnrollmentRepository;
    private final AssessmentAttemptRepository assessmentAttemptRepository;
    private final AssessmentResultRepository assessmentResultRepository;
    private final PlanStepRepository planStepRepository;
    private final StudentPlanRepository studentPlanRepository;
    private final SchoolRepository schoolRepository;
    private final ObjectMapper objectMapper;

    public List<StudentDto> list() {
        return toStudentDtos(userRepository.findByRoles_CodeAndDeletedAtIsNull("student"));
    }

    public List<StudentDto> list(UUID subjectId, UUID classId, UUID classSubjectId) {
        if (classSubjectId != null) {
            return listByClassSubject(classSubjectId);
        }
        if (classId != null) {
            return listByClass(classId);
        }
        if (subjectId != null) {
            return listBySubject(subjectId);
        }
        return list();
    }

    public StudentDto get(UUID id) {
        User user = userRepository.findByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> new NotFoundException("Student not found: " + id));
        return toStudentDtos(List.of(user)).stream()
            .findFirst()
            .orElseThrow(() -> new NotFoundException("Student not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<StudentAssessmentHistoryItemDto> getStudentAssessments(UUID studentId,
                                                                       String status,
                                                                       UUID subjectId,
                                                                       String from,
                                                                       String to) {
        userRepository.findByIdAndDeletedAtIsNull(studentId)
            .orElseThrow(() -> new NotFoundException("Student not found: " + studentId));

        Instant fromDate = parseOptionalInstant(from, false);
        Instant toDate = parseOptionalInstant(to, true);
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new BadRequestException("from must be before or equal to to");
        }

        boolean applyFrom = fromDate != null;
        boolean applyTo = toDate != null;
        List<AssessmentEnrollment> enrollments = assessmentEnrollmentRepository
            .findStudentHistory(
                studentId,
                subjectId,
                applyFrom,
                applyFrom ? fromDate : OPEN_START_INSTANT,
                applyTo,
                applyTo ? toDate : OPEN_END_INSTANT
            );
        return mapStudentAssessmentHistory(studentId, enrollments, status);
    }

    @Transactional(readOnly = true)
    public StudentAssessmentDetailDto getStudentAssessment(UUID studentId, UUID assessmentId) {
        userRepository.findByIdAndDeletedAtIsNull(studentId)
            .orElseThrow(() -> new NotFoundException("Student not found: " + studentId));

        List<AssessmentEnrollment> enrollments = assessmentEnrollmentRepository
            .findStudentHistoryByAssessment(studentId, assessmentId);
        if (enrollments.isEmpty()) {
            throw new NotFoundException("Assessment history not found for student: " + studentId);
        }

        List<StudentAssessmentHistoryItemDto> history = mapStudentAssessmentHistory(studentId, enrollments, null);
        if (history.isEmpty()) {
            throw new NotFoundException("Assessment history not found for student: " + studentId);
        }

        AssessmentAssignment latestAssignment = enrollments.stream()
            .map(AssessmentEnrollment::getAssessmentAssignment)
            .filter(Objects::nonNull)
            .sorted(Comparator
                .comparing(AssessmentAssignment::getDueTime, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(AssessmentAssignment::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
            .findFirst()
            .orElseThrow(() -> new NotFoundException("Assessment history not found for student: " + studentId));

        StudentAssessmentHistoryItemDto latestHistory = history.get(0);
        return StudentAssessmentDetailDto.builder()
            .studentId(studentId.toString())
            .assessmentId(assessmentId.toString())
            .assessmentName(latestAssignment.getAssessment().getName())
            .assessmentType(latestAssignment.getAssessment().getAssessmentType())
            .subjectId(latestAssignment.getAssessment().getSubject().getId().toString())
            .subjectName(latestAssignment.getAssessment().getSubject().getName())
            .description(latestAssignment.getAssessment().getDescription())
            .maxScore(latestAssignment.getAssessment().getMaxScore())
            .latestStatus(latestHistory.getStatus())
            .latestDueTime(latestHistory.getDueTime())
            .latestScore(latestHistory.getScore())
            .latestGrade(latestHistory.getGrade())
            .latestFeedback(latestHistory.getFeedback())
            .history(history)
            .build();
    }

    @Transactional(readOnly = true)
    public List<StudentActivityFeedItemDto> getStudentActivityFeed(UUID studentId,
                                                                   UUID subjectId,
                                                                   String from,
                                                                   String to,
                                                                   Integer limit) {
        userRepository.findByIdAndDeletedAtIsNull(studentId)
            .orElseThrow(() -> new NotFoundException("Student not found: " + studentId));

        Instant fromDate = parseOptionalInstant(from, false);
        Instant toDate = parseOptionalInstant(to, true);
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new BadRequestException("from must be before or equal to to");
        }

        int safeLimit = limit == null ? 50 : Math.max(1, Math.min(250, limit));

        List<AssessmentAttempt> attempts = assessmentAttemptRepository.findSubmittedHistoryByStudent(
            studentId,
            subjectId,
            fromDate != null,
            fromDate,
            toDate != null,
            toDate
        );

        Map<UUID, AttemptMetrics> attemptMetrics = new HashMap<>();
        List<UUID> attemptIds = attempts.stream()
            .map(AssessmentAttempt::getId)
            .filter(Objects::nonNull)
            .toList();
        if (!attemptIds.isEmpty()) {
            for (Object[] row : attemptAnswerRepository.summarizeByAssessmentAttemptIds(attemptIds)) {
                if (row == null || row.length < 5 || row[0] == null) {
                    continue;
                }
                UUID attemptId = (UUID) row[0];
                int answeredCount = ((Number) row[1]).intValue();
                int correctCount = ((Number) row[2]).intValue();
                double score = row[3] == null ? 0.0 : ((Number) row[3]).doubleValue();
                double maxScore = row[4] == null ? 0.0 : ((Number) row[4]).doubleValue();
                attemptMetrics.put(attemptId, new AttemptMetrics(answeredCount, correctCount, score, maxScore));
            }
        }

        Map<UUID, Integer> questionCountByAssessment = new HashMap<>();
        List<UUID> assessmentIds = attempts.stream()
            .map(AssessmentAttempt::getAssessmentEnrollment)
            .filter(Objects::nonNull)
            .map(AssessmentEnrollment::getAssessmentAssignment)
            .filter(Objects::nonNull)
            .map(AssessmentAssignment::getAssessment)
            .filter(Objects::nonNull)
            .map(assessment -> assessment.getId())
            .filter(Objects::nonNull)
            .distinct()
            .toList();
        if (!assessmentIds.isEmpty()) {
            for (Object[] row : assessmentQuestionRepository.countByAssessmentIds(assessmentIds)) {
                if (row == null || row.length < 2 || row[0] == null || row[1] == null) {
                    continue;
                }
                questionCountByAssessment.put((UUID) row[0], ((Number) row[1]).intValue());
            }
        }

        List<StudentActivityFeedItemDto> feedItems = new ArrayList<>();
        for (AssessmentAttempt attempt : attempts) {
            AssessmentEnrollment enrollment = attempt.getAssessmentEnrollment();
            if (enrollment == null || enrollment.getAssessmentAssignment() == null) {
                continue;
            }
            AssessmentAssignment assignment = enrollment.getAssessmentAssignment();
            if (assignment.getAssessment() == null || assignment.getAssessment().getSubject() == null) {
                continue;
            }

            AttemptMetrics metrics = attemptMetrics.getOrDefault(attempt.getId(), AttemptMetrics.ZERO);
            UUID assessmentId = assignment.getAssessment().getId();
            int totalQuestions = questionCountByAssessment.getOrDefault(
                assessmentId,
                metrics.answeredCount > 0 ? metrics.answeredCount : 0
            );
            Integer timeMinutes = null;
            if (attempt.getStartedAt() != null && attempt.getSubmittedAt() != null && !attempt.getSubmittedAt().isBefore(attempt.getStartedAt())) {
                long minutes = Duration.between(attempt.getStartedAt(), attempt.getSubmittedAt()).toMinutes();
                timeMinutes = (int) Math.max(0, minutes);
            }

            double score = attempt.getFinalScore() != null
                ? attempt.getFinalScore()
                : attempt.getTotalScore() != null ? attempt.getTotalScore() : metrics.score;
            double maxScore = attempt.getMaxScore() != null && attempt.getMaxScore() > 0
                ? attempt.getMaxScore()
                : metrics.maxScore;

            String title = assignment.getTitle() != null && !assignment.getTitle().isBlank()
                ? assignment.getTitle()
                : assignment.getAssessment().getName();

            feedItems.add(StudentActivityFeedItemDto.builder()
                .id("attempt-" + attempt.getId())
                .activityType("assessment_attempt")
                .sourceId(attempt.getId().toString())
                .title(title)
                .subjectId(assignment.getAssessment().getSubject().getId().toString())
                .subjectName(assignment.getAssessment().getSubject().getName())
                .occurredAt(attempt.getSubmittedAt())
                .level(resolveAttemptLevel(attempt))
                .correctCount(metrics.correctCount > 0 ? metrics.correctCount : null)
                .totalCount(totalQuestions > 0 ? totalQuestions : null)
                .score(maxScore > 0 ? roundOneDecimal(score) : null)
                .maxScore(maxScore > 0 ? roundOneDecimal(maxScore) : null)
                .timeMinutes(timeMinutes)
                .build());
        }

        List<StudentPlan> studentPlans = subjectId == null
            ? studentPlanRepository.findByStudent_IdAndDeletedAtIsNullOrderByCreatedAtDesc(studentId)
            : studentPlanRepository.findByStudent_IdAndSubject_IdAndDeletedAtIsNullOrderByCreatedAtDesc(studentId, subjectId);
        for (StudentPlan studentPlan : studentPlans) {
            Instant occurredAt = studentPlan.getUpdatedAt() != null ? studentPlan.getUpdatedAt() : studentPlan.getCreatedAt();
            if (occurredAt == null) {
                continue;
            }
            if (fromDate != null && occurredAt.isBefore(fromDate)) {
                continue;
            }
            if (toDate != null && occurredAt.isAfter(toDate)) {
                continue;
            }
            if (studentPlan.getPlan() == null || studentPlan.getSubject() == null) {
                continue;
            }

            feedItems.add(StudentActivityFeedItemDto.builder()
                .id("plan-" + studentPlan.getId())
                .activityType("plan_progress")
                .sourceId(studentPlan.getId().toString())
                .title(studentPlan.getPlan().getName())
                .subjectId(studentPlan.getSubject().getId().toString())
                .subjectName(studentPlan.getSubject().getName())
                .occurredAt(occurredAt)
                .level(studentPlan.getStatus())
                .progressPercent(roundOneDecimal(studentPlan.getCurrentProgress() == null ? 0.0 : studentPlan.getCurrentProgress()))
                .build());
        }

        feedItems.sort(Comparator
            .comparing(StudentActivityFeedItemDto::getOccurredAt, Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(StudentActivityFeedItemDto::getId, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
        if (feedItems.size() <= safeLimit) {
            return feedItems;
        }
        return feedItems.subList(0, safeLimit);
    }

    @Transactional
    public StudentPracticeSessionDto startPracticeSession(UUID studentId,
                                                          UUID subjectId,
                                                          StartStudentPracticeSessionRequest request) {
        User student = userRepository.findByIdAndDeletedAtIsNull(studentId)
            .orElseThrow(() -> new NotFoundException("Student not found: " + studentId));
        Subject subject = subjectRepository.findByIdAndDeletedAtIsNull(subjectId)
            .orElseThrow(() -> new NotFoundException("Subject not found: " + subjectId));

        String mode = normalizePracticeMode(request != null ? request.getMode() : null);
        int questionCount = clampQuestionCount(request != null ? request.getQuestionCount() : null);
        Topic topic = resolvePracticeTopic(subjectId, request != null ? request.getTopicId() : null);
        List<Question> selectedQuestions = resolvePracticeQuestions(subjectId, topic, questionCount);
        double totalMaxScore = selectedQuestions.stream()
            .map(Question::getMaxMark)
            .filter(Objects::nonNull)
            .mapToDouble(Double::doubleValue)
            .sum();
        if (totalMaxScore <= 0) {
            totalMaxScore = selectedQuestions.size();
        }

        School school = resolveStudentSchool(studentId);
        String title = resolvePracticeTitle(subject, topic, request != null ? request.getTitle() : null, mode);

        Assessment assessment = new Assessment();
        assessment.setSchool(school);
        assessment.setSubject(subject);
        assessment.setName(title);
        assessment.setDescription("Student practice session");
        assessment.setAssessmentType("practice");
        assessment.setVisibility("private");
        assessment.setAttemptsAllowed(1);
        assessment.setMaxScore(totalMaxScore);
        assessment.setWeightPct(0.0);
        assessment.setAiEnhanced(false);
        assessment.setStatus("published");
        assessment.setCreatedBy(student);
        assessment.setLastModifiedBy(student);
        Assessment savedAssessment = assessmentRepository.save(assessment);

        AssessmentAssignment assignment = new AssessmentAssignment();
        assignment.setAssessment(savedAssessment);
        assignment.setAssignedBy(student);
        assignment.setTitle(title);
        assignment.setInstructions("mode=" + mode);
        assignment.setStartTime(Instant.now());
        assignment.setDueTime(Instant.now().plus(Duration.ofDays(7)));
        assignment.setPublished(true);
        AssessmentAssignment savedAssignment = assessmentAssignmentRepository.save(assignment);

        AssessmentEnrollment enrollment = new AssessmentEnrollment();
        enrollment.setAssessmentAssignment(savedAssignment);
        enrollment.setStudent(student);
        enrollment.setStatusCode("assigned");
        AssessmentEnrollment savedEnrollment = assessmentEnrollmentRepository.save(enrollment);

        List<AssessmentQuestion> assessmentQuestions = new ArrayList<>(selectedQuestions.size());
        int sequence = 1;
        for (Question question : selectedQuestions) {
            AssessmentQuestion assessmentQuestion = new AssessmentQuestion();
            assessmentQuestion.setAssessment(savedAssessment);
            assessmentQuestion.setQuestion(question);
            assessmentQuestion.setSequenceIndex(sequence++);
            assessmentQuestion.setPoints(question.getMaxMark() == null ? 1.0 : question.getMaxMark());
            assessmentQuestions.add(assessmentQuestion);
        }
        List<AssessmentQuestion> savedAssessmentQuestions = assessmentQuestionRepository.saveAll(assessmentQuestions);

        AssessmentAttempt attempt = new AssessmentAttempt();
        attempt.setAssessmentEnrollment(savedEnrollment);
        attempt.setAttemptNumber(1);
        attempt.setStartedAt(Instant.now());
        attempt.setSubmissionType(mode);
        attempt.setGradingStatusCode("pending");
        attempt.setMaxScore(totalMaxScore);
        AssessmentAttempt savedAttempt = assessmentAttemptRepository.save(attempt);

        return buildPracticeSessionDto(
            savedAttempt,
            savedAssessmentQuestions,
            List.of(),
            mode,
            title,
            topic,
            true
        );
    }

    @Transactional
    public StudentPracticeAnswerResultDto submitPracticeAnswer(UUID studentId,
                                                               UUID sessionId,
                                                               StudentPracticeAnswerRequest request) {
        if (request == null || request.getAssessmentQuestionId() == null) {
            throw new BadRequestException("assessmentQuestionId is required");
        }

        AssessmentAttempt attempt = assessmentAttemptRepository.findById(sessionId)
            .orElseThrow(() -> new NotFoundException("Practice session not found: " + sessionId));
        validateSessionOwner(studentId, attempt);

        if (attempt.getSubmittedAt() != null) {
            throw new BadRequestException("Practice session is already completed");
        }

        Assessment assessment = attempt.getAssessmentEnrollment().getAssessmentAssignment().getAssessment();
        AssessmentQuestion assessmentQuestion = assessmentQuestionRepository
            .findByIdAndAssessment_IdAndDeletedAtIsNull(request.getAssessmentQuestionId(), assessment.getId())
            .orElseThrow(() -> new NotFoundException("Assessment question not found: " + request.getAssessmentQuestionId()));

        AttemptAnswer answer = attemptAnswerRepository
            .findByAssessmentAttempt_IdAndAssessmentQuestion_IdAndDeletedAtIsNull(sessionId, assessmentQuestion.getId())
            .orElseGet(AttemptAnswer::new);
        answer.setAssessmentAttempt(attempt);
        answer.setAssessmentQuestion(assessmentQuestion);
        answer.setStudentAnswerText(resolveStudentAnswerText(request));
        answer.setStudentAnswerBlob(buildStudentAnswerBlob(request));
        double questionMaxScore = resolveQuestionMaxScore(assessmentQuestion);
        answer.setMaxScore(questionMaxScore);

        PracticeEvaluation evaluation = evaluatePracticeAnswer(assessmentQuestion.getQuestion(), request, questionMaxScore);
        if (evaluation.autoGraded) {
            answer.setAiScore(evaluation.score);
            answer.setAiConfidence(1.0);
            answer.setRequiresReview(false);
            answer.setGradedAt(Instant.now());
        } else {
            answer.setAiScore(null);
            answer.setAiConfidence(null);
            answer.setRequiresReview(true);
            answer.setGradedAt(null);
        }
        answer.setFeedbackText(evaluation.feedback);

        AttemptAnswer savedAnswer = attemptAnswerRepository.save(answer);

        List<AssessmentQuestion> assessmentQuestions =
            assessmentQuestionRepository.findByAssessment_IdAndDeletedAtIsNullOrderBySequenceIndexAsc(assessment.getId());
        List<AttemptAnswer> answers =
            attemptAnswerRepository.findByAssessmentAttempt_IdAndDeletedAtIsNull(attempt.getId());
        PracticeSessionMetrics metrics = calculatePracticeSessionMetrics(assessmentQuestions, answers, attempt);

        return StudentPracticeAnswerResultDto.builder()
            .sessionId(attempt.getId().toString())
            .answerId(savedAnswer.getId().toString())
            .assessmentQuestionId(assessmentQuestion.getId().toString())
            .correct(evaluation.correct)
            .skipped(request.isSkipped())
            .score(evaluation.autoGraded ? roundOneDecimal(evaluation.score) : null)
            .maxScore(roundOneDecimal(questionMaxScore))
            .feedback(evaluation.feedback)
            .gradedAt(savedAnswer.getGradedAt())
            .answeredCount(metrics.answeredCount)
            .totalQuestions(metrics.totalQuestions)
            .correctCount(metrics.correctCount)
            .sessionScore(roundOneDecimal(metrics.score))
            .sessionMaxScore(roundOneDecimal(metrics.maxScore))
            .sessionPercentage(metrics.maxScore > 0 ? roundOneDecimal((metrics.score / metrics.maxScore) * 100.0) : null)
            .completed(metrics.answeredCount >= metrics.totalQuestions && metrics.totalQuestions > 0)
            .build();
    }

    @Transactional
    public StudentPracticeSessionDto completePracticeSession(UUID studentId, UUID sessionId) {
        AssessmentAttempt attempt = assessmentAttemptRepository.findById(sessionId)
            .orElseThrow(() -> new NotFoundException("Practice session not found: " + sessionId));
        validateSessionOwner(studentId, attempt);

        Assessment assessment = attempt.getAssessmentEnrollment().getAssessmentAssignment().getAssessment();
        List<AssessmentQuestion> assessmentQuestions =
            assessmentQuestionRepository.findByAssessment_IdAndDeletedAtIsNullOrderBySequenceIndexAsc(assessment.getId());
        List<AttemptAnswer> answers =
            attemptAnswerRepository.findByAssessmentAttempt_IdAndDeletedAtIsNull(attempt.getId());
        PracticeSessionMetrics metrics = calculatePracticeSessionMetrics(assessmentQuestions, answers, attempt);

        if (attempt.getSubmittedAt() == null) {
            attempt.setSubmittedAt(Instant.now());
        }
        attempt.setTotalScore(metrics.score);
        attempt.setMaxScore(metrics.maxScore > 0 ? metrics.maxScore : attempt.getMaxScore());
        attempt.setFinalScore(metrics.score);
        attempt.setFinalGrade(toGradeLabel(metrics.maxScore > 0 ? (metrics.score / metrics.maxScore) * 100.0 : 0.0));
        attempt.setGradingStatusCode("auto_graded");
        AssessmentAttempt savedAttempt = assessmentAttemptRepository.save(attempt);

        AssessmentEnrollment enrollment = savedAttempt.getAssessmentEnrollment();
        if (enrollment != null) {
            enrollment.setStatusCode("completed");
            assessmentEnrollmentRepository.save(enrollment);
            upsertPracticeResult(enrollment, savedAttempt, metrics);
        }

        String mode = normalizePracticeMode(savedAttempt.getSubmissionType());
        return buildPracticeSessionDto(
            savedAttempt,
            assessmentQuestions,
            answers,
            mode,
            savedAttempt.getAssessmentEnrollment().getAssessmentAssignment().getTitle(),
            resolveSingleTopic(assessmentQuestions),
            false
        );
    }

    @Transactional(readOnly = true)
    public List<StudentPracticeSessionDto> getPracticeSessionHistory(UUID studentId, UUID subjectId, Integer limit) {
        userRepository.findByIdAndDeletedAtIsNull(studentId)
            .orElseThrow(() -> new NotFoundException("Student not found: " + studentId));

        int safeLimit = limit == null ? 20 : Math.max(1, Math.min(100, limit));
        List<AssessmentAttempt> attempts = assessmentAttemptRepository.findStudentSessionHistory(
            studentId,
            subjectId,
            PRACTICE_SESSION_MODES
        );
        if (attempts.isEmpty()) {
            return List.of();
        }

        List<UUID> attemptIds = attempts.stream().map(AssessmentAttempt::getId).filter(Objects::nonNull).toList();
        Map<UUID, AttemptMetrics> attemptMetrics = new HashMap<>();
        if (!attemptIds.isEmpty()) {
            for (Object[] row : attemptAnswerRepository.summarizeByAssessmentAttemptIds(attemptIds)) {
                if (row == null || row.length < 5 || row[0] == null) {
                    continue;
                }
                UUID attemptId = (UUID) row[0];
                int answeredCount = ((Number) row[1]).intValue();
                int correctCount = ((Number) row[2]).intValue();
                double score = row[3] == null ? 0.0 : ((Number) row[3]).doubleValue();
                double maxScore = row[4] == null ? 0.0 : ((Number) row[4]).doubleValue();
                attemptMetrics.put(attemptId, new AttemptMetrics(answeredCount, correctCount, score, maxScore));
            }
        }

        List<UUID> assessmentIds = attempts.stream()
            .map(AssessmentAttempt::getAssessmentEnrollment)
            .filter(Objects::nonNull)
            .map(AssessmentEnrollment::getAssessmentAssignment)
            .filter(Objects::nonNull)
            .map(AssessmentAssignment::getAssessment)
            .filter(Objects::nonNull)
            .map(Assessment::getId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
        Map<UUID, Integer> questionCountByAssessment = new HashMap<>();
        if (!assessmentIds.isEmpty()) {
            for (Object[] row : assessmentQuestionRepository.countByAssessmentIds(assessmentIds)) {
                if (row == null || row.length < 2 || row[0] == null || row[1] == null) {
                    continue;
                }
                questionCountByAssessment.put((UUID) row[0], ((Number) row[1]).intValue());
            }
        }

        return attempts.stream()
            .limit(safeLimit)
            .map(attempt -> {
                AssessmentEnrollment enrollment = attempt.getAssessmentEnrollment();
                AssessmentAssignment assignment = enrollment.getAssessmentAssignment();
                Assessment assessment = assignment.getAssessment();
                AttemptMetrics metrics = attemptMetrics.getOrDefault(attempt.getId(), AttemptMetrics.ZERO);
                Integer questionCount = questionCountByAssessment.getOrDefault(
                    assessment.getId(),
                    metrics.answeredCount > 0 ? metrics.answeredCount : 0
                );
                Integer durationMinutes = resolveDurationMinutes(attempt.getStartedAt(), attempt.getSubmittedAt());
                double score = attempt.getFinalScore() != null
                    ? attempt.getFinalScore()
                    : attempt.getTotalScore() != null ? attempt.getTotalScore() : metrics.score;
                double maxScore = attempt.getMaxScore() != null && attempt.getMaxScore() > 0
                    ? attempt.getMaxScore()
                    : metrics.maxScore;
                String title = assignment.getTitle() != null && !assignment.getTitle().isBlank()
                    ? assignment.getTitle()
                    : assessment.getName();
                return StudentPracticeSessionDto.builder()
                    .sessionId(attempt.getId().toString())
                    .assessmentId(assessment.getId().toString())
                    .assignmentId(assignment.getId().toString())
                    .enrollmentId(enrollment.getId().toString())
                    .subjectId(assessment.getSubject().getId().toString())
                    .subjectName(assessment.getSubject().getName())
                    .topicId(null)
                    .topicName(null)
                    .mode(normalizePracticeMode(attempt.getSubmissionType()))
                    .title(title)
                    .status(attempt.getSubmittedAt() == null ? "in_progress" : "completed")
                    .startedAt(attempt.getStartedAt())
                    .submittedAt(attempt.getSubmittedAt())
                    .questionCount(questionCount)
                    .answeredCount(metrics.answeredCount)
                    .correctCount(metrics.correctCount)
                    .score(roundOneDecimal(score))
                    .maxScore(maxScore > 0 ? roundOneDecimal(maxScore) : null)
                    .percentage(maxScore > 0 ? roundOneDecimal((score / maxScore) * 100.0) : null)
                    .durationMinutes(durationMinutes)
                    .questions(List.of())
                    .build();
            })
            .toList();
    }

    @Transactional
    public StudentPlanRuntimeProgressDto updateStudentPlanRuntime(UUID studentId,
                                                                  UUID studentPlanId,
                                                                  StudentPlanRuntimeProgressRequest request) {
        StudentPlan studentPlan = studentPlanRepository.findByIdAndStudent_IdAndDeletedAtIsNull(studentPlanId, studentId)
            .orElseThrow(() -> new NotFoundException("Student plan not found: " + studentPlanId));
        if (request == null) {
            throw new BadRequestException("Request payload is required");
        }

        List<PlanStep> planSteps = planStepRepository.findByPlan_IdOrderByStepOrderAsc(studentPlan.getPlan().getId());
        Set<String> validStepIds = planSteps.stream()
            .map(PlanStep::getId)
            .filter(Objects::nonNull)
            .map(UUID::toString)
            .collect(Collectors.toCollection(LinkedHashSet::new));

        List<String> completedStepIds = (request.getCompletedStepIds() == null ? List.<String>of() : request.getCompletedStepIds()).stream()
            .filter(Objects::nonNull)
            .map(String::trim)
            .filter(id -> !id.isBlank() && validStepIds.contains(id))
            .distinct()
            .toList();

        String activeStepId = request.getActiveStepId();
        if (activeStepId != null) {
            activeStepId = activeStepId.trim();
            if (activeStepId.isBlank() || !validStepIds.contains(activeStepId)) {
                activeStepId = null;
            }
        }

        int totalSteps = planSteps.size();
        int completedSteps = Math.min(completedStepIds.size(), totalSteps);
        double progress = totalSteps == 0 ? 0.0 : roundOneDecimal((completedSteps * 100.0) / totalSteps);
        studentPlan.setCurrentProgress(progress);

        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            studentPlan.setStatus(normalizePlanRuntimeStatus(request.getStatus()));
        }
        if (progress >= 100.0) {
            studentPlan.setStatus("completed");
            studentPlan.setCompletionDate(studentPlan.getCompletionDate() == null ? Instant.now() : studentPlan.getCompletionDate());
        } else if ("completed".equalsIgnoreCase(studentPlan.getStatus())) {
            studentPlan.setStatus("active");
            studentPlan.setCompletionDate(null);
        }

        StudentPlan savedPlan = studentPlanRepository.save(studentPlan);
        return StudentPlanRuntimeProgressDto.builder()
            .studentPlanId(savedPlan.getId().toString())
            .studentId(savedPlan.getStudent().getId().toString())
            .activeStepId(activeStepId)
            .completedStepIds(completedStepIds)
            .totalSteps(totalSteps)
            .completedSteps(completedSteps)
            .currentProgress(roundOneDecimal(savedPlan.getCurrentProgress() == null ? 0.0 : savedPlan.getCurrentProgress()))
            .status(savedPlan.getStatus())
            .updatedAt(savedPlan.getUpdatedAt())
            .build();
    }

    @Transactional(readOnly = true)
    public StudentSubjectOverviewDto getSubjectOverview(UUID studentId, UUID subjectId) {
        userRepository.findByIdAndDeletedAtIsNull(studentId)
            .orElseThrow(() -> new NotFoundException("Student not found: " + studentId));
        Subject subject = subjectRepository.findByIdAndDeletedAtIsNull(subjectId)
            .orElseThrow(() -> new NotFoundException("Subject not found: " + subjectId));

        List<Topic> topics = topicRepository.findBySubject_IdAndDeletedAtIsNullOrderBySequenceIndexAsc(subjectId);
        if (topics.isEmpty()) {
            return StudentSubjectOverviewDto.builder()
                .studentId(studentId.toString())
                .subjectId(subjectId.toString())
                .subjectName(subject.getName())
                .topicCount(0)
                .unitCount(0)
                .totalQuestionCount(0)
                .challengeEligibility(StudentChallengeEligibilityDto.builder()
                    .eligible(false)
                    .reason("No topics are published for this subject yet.")
                    .minQuestionsRequired(MIN_QUESTIONS_FOR_SUBJECT_CHALLENGE)
                    .availableQuestions(0)
                    .build())
                .units(List.of())
                .build();
        }

        Map<UUID, Long> questionCountByTopic = questionRepository.countQuestionsByTopic(subjectId).stream()
            .filter(row -> row != null && row.length >= 2 && row[0] != null && row[1] != null)
            .collect(Collectors.toMap(
                row -> (UUID) row[0],
                row -> ((Number) row[1]).longValue(),
                (left, right) -> left
            ));

        Map<UUID, double[]> masteryTotalsByTopic = new HashMap<>();
        List<TopicAnswerStat> topicStats = attemptAnswerRepository.findTopicStatsBySubjectAndStudent(subjectId, studentId);
        for (TopicAnswerStat stat : topicStats) {
            if (stat.getTopicId() == null || stat.getMaxScore() == null || stat.getMaxScore() == 0) {
                continue;
            }
            double[] totals = masteryTotalsByTopic.computeIfAbsent(stat.getTopicId(), key -> new double[] {0, 0});
            double score = stat.getScore() != null ? stat.getScore() : 0;
            totals[0] += score;
            totals[1] += stat.getMaxScore();
        }

        Map<Integer, List<Topic>> topicsByUnit = new LinkedHashMap<>();
        for (int i = 0; i < topics.size(); i += 1) {
            Topic topic = topics.get(i);
            int unitNumber = deriveUnitNumber(topic, i);
            topicsByUnit.computeIfAbsent(unitNumber, key -> new ArrayList<>()).add(topic);
        }

        List<StudentSubjectOverviewUnitDto> units = new ArrayList<>();
        long totalQuestionCount = 0L;

        for (Map.Entry<Integer, List<Topic>> entry : topicsByUnit.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .toList()) {
            int unitNumber = entry.getKey();
            List<Topic> unitTopics = entry.getValue();

            List<StudentSubjectOverviewTopicDto> topicDtos = new ArrayList<>();
            long unitQuestionCount = 0L;
            double unitMasteryTotal = 0.0;

            for (Topic topic : unitTopics) {
                long questionCount = questionCountByTopic.getOrDefault(topic.getId(), 0L);
                unitQuestionCount += questionCount;

                double masteryPercent = 0.0;
                double[] masteryTotals = masteryTotalsByTopic.get(topic.getId());
                if (masteryTotals != null && masteryTotals[1] > 0) {
                    masteryPercent = (masteryTotals[0] / masteryTotals[1]) * 100.0;
                }
                unitMasteryTotal += masteryPercent;

                topicDtos.add(StudentSubjectOverviewTopicDto.builder()
                    .topicId(topic.getId().toString())
                    .code(topic.getCode())
                    .name(topic.getName())
                    .sequenceIndex(topic.getSequenceIndex())
                    .masteryPercent(roundOneDecimal(masteryPercent))
                    .questionCount(questionCount)
                    .build());
            }

            totalQuestionCount += unitQuestionCount;
            double unitMastery = unitTopics.isEmpty() ? 0.0 : unitMasteryTotal / unitTopics.size();
            Topic firstTopic = unitTopics.get(0);
            String title = firstTopic.getName() != null && !firstTopic.getName().isBlank()
                ? firstTopic.getName()
                : subject.getName() + " Unit " + unitNumber;

            units.add(StudentSubjectOverviewUnitDto.builder()
                .unitId(subjectId + "-unit-" + unitNumber)
                .unitNumber(unitNumber)
                .code("Unit " + unitNumber)
                .title(title)
                .topicCount(unitTopics.size())
                .questionCount(unitQuestionCount)
                .masteryPercent(roundOneDecimal(unitMastery))
                .topics(topicDtos)
                .build());
        }

        StudentChallengeEligibilityDto challengeEligibility;
        if (totalQuestionCount < MIN_QUESTIONS_FOR_SUBJECT_CHALLENGE) {
            challengeEligibility = StudentChallengeEligibilityDto.builder()
                .eligible(false)
                .reason("Need at least " + MIN_QUESTIONS_FOR_SUBJECT_CHALLENGE
                    + " published questions before challenge generation.")
                .minQuestionsRequired(MIN_QUESTIONS_FOR_SUBJECT_CHALLENGE)
                .availableQuestions(Math.toIntExact(totalQuestionCount))
                .build();
        } else {
            challengeEligibility = StudentChallengeEligibilityDto.builder()
                .eligible(true)
                .reason("Ready for challenge generation.")
                .minQuestionsRequired(MIN_QUESTIONS_FOR_SUBJECT_CHALLENGE)
                .availableQuestions(Math.toIntExact(totalQuestionCount))
                .build();
        }

        return StudentSubjectOverviewDto.builder()
            .studentId(studentId.toString())
            .subjectId(subjectId.toString())
            .subjectName(subject.getName())
            .topicCount(topics.size())
            .unitCount(units.size())
            .totalQuestionCount(totalQuestionCount)
            .challengeEligibility(challengeEligibility)
            .units(units)
            .build();
    }

    @Transactional(readOnly = true)
    public List<StudentTeacherDto> getTeachers(UUID studentId) {
        userRepository.findByIdAndDeletedAtIsNull(studentId)
            .orElseThrow(() -> new NotFoundException("Student not found: " + studentId));

        List<Enrolment> enrolments = enrolmentRepository.findByStudent_Id(studentId).stream()
            .filter(enrolment -> enrolment.getDeletedAt() == null)
            .filter(enrolment -> enrolment.getClassEntity() != null && enrolment.getClassEntity().getDeletedAt() == null)
            .toList();
        if (enrolments.isEmpty()) {
            return List.of();
        }

        Map<UUID, TeacherAccumulator> teacherMap = new LinkedHashMap<>();

        for (Enrolment enrolment : enrolments) {
            ClassEntity classEntity = enrolment.getClassEntity();
            if (classEntity == null) {
                continue;
            }
            User homeroomTeacher = classEntity.getHomeroomTeacher();
            if (homeroomTeacher == null || homeroomTeacher.getDeletedAt() != null) {
                continue;
            }
            TeacherAccumulator accumulator = teacherMap.computeIfAbsent(
                homeroomTeacher.getId(),
                key -> TeacherAccumulator.from(homeroomTeacher)
            );
            if (classEntity.getName() != null && !classEntity.getName().isBlank()) {
                String className = classEntity.getName().trim();
                accumulator.classNames.add(className);
                accumulator.homeroomClassNames.add(className);
            }
        }

        List<UUID> classIds = enrolments.stream()
            .map(Enrolment::getClassEntity)
            .filter(Objects::nonNull)
            .map(ClassEntity::getId)
            .distinct()
            .toList();
        if (!classIds.isEmpty()) {
            List<ClassSubject> classSubjects = classSubjectRepository.findByClassEntity_IdInAndDeletedAtIsNull(classIds);
            for (ClassSubject classSubject : classSubjects) {
                User teacher = classSubject.getTeacher();
                if (teacher == null || teacher.getDeletedAt() != null) {
                    continue;
                }
                TeacherAccumulator accumulator = teacherMap.computeIfAbsent(
                    teacher.getId(),
                    key -> TeacherAccumulator.from(teacher)
                );

                if (classSubject.getSubject() != null && classSubject.getSubject().getName() != null && !classSubject.getSubject().getName().isBlank()) {
                    accumulator.subjectNames.add(classSubject.getSubject().getName().trim());
                }
                if (classSubject.getClassEntity() != null
                    && classSubject.getClassEntity().getName() != null
                    && !classSubject.getClassEntity().getName().isBlank()) {
                    accumulator.classNames.add(classSubject.getClassEntity().getName().trim());
                }
            }
        }

        return teacherMap.values().stream()
            .map(TeacherAccumulator::toDto)
            .sorted(Comparator.comparing(StudentTeacherDto::getLastName, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(StudentTeacherDto::getFirstName, String.CASE_INSENSITIVE_ORDER))
            .toList();
    }

    private List<StudentAssessmentHistoryItemDto> mapStudentAssessmentHistory(UUID studentId,
                                                                              List<AssessmentEnrollment> enrollments,
                                                                              String statusFilter) {
        if (enrollments == null || enrollments.isEmpty()) {
            return List.of();
        }

        List<UUID> enrollmentIds = enrollments.stream()
            .map(AssessmentEnrollment::getId)
            .filter(Objects::nonNull)
            .toList();
        List<UUID> assignmentIds = enrollments.stream()
            .map(AssessmentEnrollment::getAssessmentAssignment)
            .filter(Objects::nonNull)
            .map(AssessmentAssignment::getId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();

        Map<UUID, AssessmentAttempt> latestAttemptByEnrollmentId = new HashMap<>();
        if (!enrollmentIds.isEmpty()) {
            for (AssessmentAttempt attempt : assessmentAttemptRepository.findForStudentHistory(enrollmentIds)) {
                UUID enrollmentId = attempt.getAssessmentEnrollment() != null
                    ? attempt.getAssessmentEnrollment().getId()
                    : null;
                if (enrollmentId != null) {
                    latestAttemptByEnrollmentId.putIfAbsent(enrollmentId, attempt);
                }
            }
        }

        Map<UUID, AssessmentResult> latestResultByAssignmentId = new HashMap<>();
        if (!assignmentIds.isEmpty()) {
            List<AssessmentResult> results = assessmentResultRepository.findForStudentHistory(studentId, assignmentIds);
            results.stream()
                .sorted(Comparator
                    .comparing(this::resultOrderingTime, Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(AssessmentResult::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .forEach(result -> {
                    UUID assignmentId = result.getAssessmentAssignment() != null
                        ? result.getAssessmentAssignment().getId()
                        : null;
                    if (assignmentId != null) {
                        latestResultByAssignmentId.putIfAbsent(assignmentId, result);
                    }
                });
        }

        List<StudentAssessmentHistoryItemDto> history = new ArrayList<>();
        for (AssessmentEnrollment enrollment : enrollments) {
            AssessmentAssignment assignment = enrollment.getAssessmentAssignment();
            if (assignment == null || assignment.getAssessment() == null || assignment.getAssessment().getSubject() == null) {
                continue;
            }

            AssessmentAttempt latestAttempt = latestAttemptByEnrollmentId.get(enrollment.getId());
            AssessmentResult latestResult = latestResultByAssignmentId.get(assignment.getId());
            String computedStatus = resolveAssessmentStatus(assignment.getDueTime(), latestAttempt, latestResult);
            if (!matchesStatusFilter(computedStatus, statusFilter)) {
                continue;
            }

            Double score = null;
            if (latestAttempt != null) {
                score = latestAttempt.getFinalScore() != null ? latestAttempt.getFinalScore() : latestAttempt.getTotalScore();
            }
            if (score == null && latestResult != null) {
                score = latestResult.getActualMark();
            }

            String grade = latestResult != null && latestResult.getGrade() != null
                ? latestResult.getGrade()
                : latestAttempt != null ? latestAttempt.getFinalGrade() : null;

            Instant submittedAt = latestAttempt != null ? latestAttempt.getSubmittedAt() : null;
            if (submittedAt == null && latestResult != null) {
                submittedAt = latestResult.getSubmittedAt();
            }

            history.add(StudentAssessmentHistoryItemDto.builder()
                .enrollmentId(enrollment.getId().toString())
                .assignmentId(assignment.getId().toString())
                .assessmentId(assignment.getAssessment().getId().toString())
                .assessmentName(assignment.getAssessment().getName())
                .assessmentType(assignment.getAssessment().getAssessmentType())
                .subjectId(assignment.getAssessment().getSubject().getId().toString())
                .subjectName(assignment.getAssessment().getSubject().getName())
                .startTime(assignment.getStartTime())
                .dueTime(assignment.getDueTime())
                .published(assignment.isPublished())
                .status(computedStatus)
                .submissionId(latestAttempt != null ? latestAttempt.getId().toString() : null)
                .attemptNumber(latestAttempt != null ? latestAttempt.getAttemptNumber() : null)
                .submittedAt(submittedAt)
                .gradedAt(latestResult != null ? latestResult.getGradedAt() : null)
                .score(score)
                .maxScore(assignment.getAssessment().getMaxScore())
                .expectedMark(latestResult != null ? latestResult.getExpectedMark() : null)
                .actualMark(latestResult != null ? latestResult.getActualMark() : null)
                .grade(grade)
                .feedback(latestResult != null ? latestResult.getFeedback() : null)
                .build());
        }

        history.sort(Comparator
            .comparing(StudentAssessmentHistoryItemDto::getDueTime, Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(StudentAssessmentHistoryItemDto::getSubmittedAt, Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(StudentAssessmentHistoryItemDto::getAssessmentName, String.CASE_INSENSITIVE_ORDER));
        return history;
    }

    private Instant resultOrderingTime(AssessmentResult result) {
        if (result == null) {
            return null;
        }
        if (result.getGradedAt() != null) {
            return result.getGradedAt();
        }
        if (result.getSubmittedAt() != null) {
            return result.getSubmittedAt();
        }
        return result.getCreatedAt();
    }

    private String resolveAssessmentStatus(Instant dueTime,
                                           AssessmentAttempt latestAttempt,
                                           AssessmentResult latestResult) {
        if (latestResult != null) {
            if (latestResult.getActualMark() != null || "published".equalsIgnoreCase(latestResult.getStatus())) {
                return "graded";
            }
        }
        if (latestAttempt != null && latestAttempt.getSubmittedAt() != null) {
            return "submitted";
        }
        if (dueTime != null && dueTime.isBefore(Instant.now())) {
            return "overdue";
        }
        return "pending";
    }

    private boolean matchesStatusFilter(String status, String filter) {
        if (filter == null || filter.isBlank() || "all".equalsIgnoreCase(filter)) {
            return true;
        }
        String normalizedFilter = filter.trim().toLowerCase();
        if ("reviewed".equals(normalizedFilter)) {
            normalizedFilter = "graded";
        }
        if ("assigned".equals(normalizedFilter)) {
            normalizedFilter = "pending";
        }
        return Objects.equals(status, normalizedFilter);
    }

    private Instant parseOptionalInstant(String value, boolean endOfDay) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();

        try {
            return Instant.parse(trimmed);
        } catch (DateTimeParseException ignored) {
        }

        try {
            return OffsetDateTime.parse(trimmed).toInstant();
        } catch (DateTimeParseException ignored) {
        }

        try {
            return LocalDateTime.parse(trimmed).toInstant(ZoneOffset.UTC);
        } catch (DateTimeParseException ignored) {
        }

        try {
            LocalDate parsedDate = LocalDate.parse(trimmed);
            if (endOfDay) {
                return parsedDate.atTime(23, 59, 59, 999_999_999).toInstant(ZoneOffset.UTC);
            }
            return parsedDate.atStartOfDay().toInstant(ZoneOffset.UTC);
        } catch (DateTimeParseException ex) {
            throw new BadRequestException("Invalid date value: " + value);
        }
    }

    private String normalizePracticeMode(String value) {
        if (value == null || value.isBlank()) {
            return "topic_practice";
        }
        String normalized = value.trim().toLowerCase().replace('-', '_').replace(' ', '_');
        return PRACTICE_SESSION_MODES.contains(normalized) ? normalized : "topic_practice";
    }

    private int clampQuestionCount(Integer value) {
        if (value == null) {
            return DEFAULT_PRACTICE_QUESTION_COUNT;
        }
        return Math.max(1, Math.min(MAX_PRACTICE_QUESTION_COUNT, value));
    }

    private Topic resolvePracticeTopic(UUID subjectId, UUID topicId) {
        if (topicId == null) {
            return null;
        }
        Topic topic = topicRepository.findById(topicId)
            .orElseThrow(() -> new NotFoundException("Topic not found: " + topicId));
        if (topic.getDeletedAt() != null || topic.getSubject() == null || !topic.getSubject().getId().equals(subjectId)) {
            throw new BadRequestException("Topic does not belong to selected subject");
        }
        return topic;
    }

    private List<Question> resolvePracticeQuestions(UUID subjectId, Topic topic, int questionCount) {
        List<Question> questionPool = topic == null
            ? questionRepository.findBySubject_IdAndActiveTrueAndDeletedAtIsNull(subjectId)
            : questionRepository.findBySubject_IdAndTopic_IdAndActiveTrueAndDeletedAtIsNull(subjectId, topic.getId());
        List<Question> activeQuestions = questionPool.stream().toList();
        if (activeQuestions.isEmpty()) {
            throw new BadRequestException("No published questions are available for this practice session.");
        }

        List<Question> shuffledQuestions = new ArrayList<>(activeQuestions);
        Collections.shuffle(shuffledQuestions);
        int count = Math.min(questionCount, shuffledQuestions.size());
        return shuffledQuestions.subList(0, count);
    }

    private School resolveStudentSchool(UUID studentId) {
        return enrolmentRepository.findActiveSchoolIdsByStudentId(studentId).stream()
            .findFirst()
            .flatMap(schoolRepository::findByIdAndDeletedAtIsNull)
            .or(() -> schoolRepository.findFirstByDeletedAtIsNullOrderByCreatedAtAsc())
            .orElseThrow(() -> new BadRequestException("No active school found for student " + studentId));
    }

    private String resolvePracticeTitle(Subject subject, Topic topic, String requestTitle, String mode) {
        if (requestTitle != null && !requestTitle.isBlank()) {
            return requestTitle.trim();
        }
        String subjectName = subject.getName() == null ? "Subject" : subject.getName();
        if ("subject_challenge".equals(mode)) {
            return "Subject challenge: " + subjectName;
        }
        if (topic != null && topic.getName() != null && !topic.getName().isBlank()) {
            return ("topic_challenge".equals(mode) ? "Topic challenge: " : "Practice: ") + topic.getName().trim();
        }
        return ("topic_challenge".equals(mode) ? "Topic challenge: " : "Practice: ") + subjectName;
    }

    private void validateSessionOwner(UUID studentId, AssessmentAttempt attempt) {
        if (attempt == null
            || attempt.getAssessmentEnrollment() == null
            || attempt.getAssessmentEnrollment().getStudent() == null
            || attempt.getAssessmentEnrollment().getStudent().getId() == null
            || !attempt.getAssessmentEnrollment().getStudent().getId().equals(studentId)) {
            throw new NotFoundException("Practice session not found");
        }
    }

    private String resolveStudentAnswerText(StudentPracticeAnswerRequest request) {
        if (request.getStudentAnswerText() != null && !request.getStudentAnswerText().isBlank()) {
            return request.getStudentAnswerText().trim();
        }
        if (request.getSelectedOptions() != null && !request.getSelectedOptions().isEmpty()) {
            return request.getSelectedOptions().stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .collect(Collectors.joining(" | "));
        }
        return null;
    }

    private JsonNode buildStudentAnswerBlob(StudentPracticeAnswerRequest request) {
        if (request.getSelectedOptions() == null || request.getSelectedOptions().isEmpty()) {
            return null;
        }
        ObjectNode node = objectMapper.createObjectNode();
        node.putPOJO("selectedOptions", request.getSelectedOptions());
        return node;
    }

    private double resolveQuestionMaxScore(AssessmentQuestion assessmentQuestion) {
        if (assessmentQuestion.getPoints() != null && assessmentQuestion.getPoints() > 0) {
            return assessmentQuestion.getPoints();
        }
        if (assessmentQuestion.getQuestion() != null
            && assessmentQuestion.getQuestion().getMaxMark() != null
            && assessmentQuestion.getQuestion().getMaxMark() > 0) {
            return assessmentQuestion.getQuestion().getMaxMark();
        }
        return 1.0;
    }

    private PracticeEvaluation evaluatePracticeAnswer(Question question,
                                                      StudentPracticeAnswerRequest request,
                                                      double questionMaxScore) {
        if (request.isSkipped()) {
            return new PracticeEvaluation(false, true, 0.0, true, "Question skipped.");
        }

        List<String> expectedAnswers = extractExpectedAnswers(question);
        if (expectedAnswers.isEmpty()) {
            return new PracticeEvaluation(false, false, 0.0, false, "Submitted for review.");
        }

        List<String> submittedAnswers = new ArrayList<>();
        if (request.getSelectedOptions() != null) {
            request.getSelectedOptions().stream()
                .filter(Objects::nonNull)
                .map(this::normalizeAnswer)
                .filter(value -> !value.isBlank())
                .forEach(submittedAnswers::add);
        }
        if ((submittedAnswers.isEmpty()) && request.getStudentAnswerText() != null) {
            String text = normalizeAnswer(request.getStudentAnswerText());
            if (!text.isBlank()) {
                submittedAnswers.add(text);
            }
        }

        boolean multipleAnswerCheck = expectedAnswers.size() > 1 || submittedAnswers.size() > 1;
        boolean correct;
        if (multipleAnswerCheck) {
            Set<String> submittedSet = new LinkedHashSet<>(submittedAnswers);
            Set<String> expectedSet = new LinkedHashSet<>(expectedAnswers);
            correct = !submittedSet.isEmpty() && submittedSet.equals(expectedSet);
        } else {
            correct = submittedAnswers.stream().anyMatch(expectedAnswers::contains);
        }

        double score = correct ? questionMaxScore : 0.0;
        return new PracticeEvaluation(correct, false, score, true, correct ? "Correct." : "Not correct. Try again.");
    }

    private List<String> extractExpectedAnswers(Question question) {
        if (question == null || question.getRubricJson() == null || question.getRubricJson().isNull()) {
            return List.of();
        }

        List<String> answers = new ArrayList<>();
        JsonNode rubricJson = question.getRubricJson();
        JsonNode direct = rubricJson.path("correctAnswer");
        appendAnswerValues(answers, direct);
        appendAnswerValues(answers, rubricJson.path("correct_answer"));
        appendAnswerValues(answers, rubricJson.path("answer"));
        appendAnswerValues(answers, rubricJson.path("answers"));

        JsonNode options = rubricJson.path("options");
        if (options.isArray()) {
            options.forEach(option -> {
                if (option != null && option.isObject() && option.path("isCorrect").asBoolean(false)) {
                    String text = option.path("text").asText("");
                    if (!text.isBlank()) {
                        answers.add(text);
                    }
                }
            });
        }

        return answers.stream()
            .map(this::normalizeAnswer)
            .filter(value -> !value.isBlank())
            .distinct()
            .toList();
    }

    private void appendAnswerValues(List<String> sink, JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return;
        }
        if (node.isArray()) {
            node.forEach(item -> appendAnswerValues(sink, item));
            return;
        }
        if (node.isTextual()) {
            String value = node.asText();
            if (value != null && !value.isBlank()) {
                sink.add(value);
            }
            return;
        }
        if (node.isNumber() || node.isBoolean()) {
            sink.add(node.asText());
        }
    }

    private String normalizeAnswer(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase().replaceAll("\\s+", " ");
    }

    private PracticeSessionMetrics calculatePracticeSessionMetrics(List<AssessmentQuestion> assessmentQuestions,
                                                                   List<AttemptAnswer> answers,
                                                                   AssessmentAttempt attempt) {
        int totalQuestions = assessmentQuestions == null ? 0 : assessmentQuestions.size();
        int answeredCount = answers == null ? 0 : answers.size();
        int correctCount = 0;
        double score = 0.0;
        double maxScore = 0.0;

        if (answers != null) {
            for (AttemptAnswer answer : answers) {
                double answerMax = answer.getMaxScore() == null ? 0.0 : answer.getMaxScore();
                double answerScore = answer.getHumanScore() != null
                    ? answer.getHumanScore()
                    : answer.getAiScore() != null ? answer.getAiScore() : 0.0;
                maxScore += answerMax;
                score += answerScore;
                if (answerMax > 0 && answerScore >= answerMax) {
                    correctCount += 1;
                }
            }
        }

        if (maxScore <= 0 && attempt.getMaxScore() != null) {
            maxScore = attempt.getMaxScore();
        }
        if (score <= 0 && attempt.getTotalScore() != null) {
            score = attempt.getTotalScore();
        }

        return new PracticeSessionMetrics(totalQuestions, answeredCount, correctCount, score, maxScore);
    }

    private Integer resolveDurationMinutes(Instant startedAt, Instant submittedAt) {
        if (startedAt == null || submittedAt == null || submittedAt.isBefore(startedAt)) {
            return null;
        }
        return (int) Math.max(0, Duration.between(startedAt, submittedAt).toMinutes());
    }

    private void upsertPracticeResult(AssessmentEnrollment enrollment,
                                      AssessmentAttempt finalizedAttempt,
                                      PracticeSessionMetrics metrics) {
        AssessmentResult result = assessmentResultRepository
            .findFirstByAssessmentAssignment_IdAndStudent_Id(
                enrollment.getAssessmentAssignment().getId(),
                enrollment.getStudent().getId()
            )
            .orElseGet(AssessmentResult::new);

        result.setAssessmentAssignment(enrollment.getAssessmentAssignment());
        result.setStudent(enrollment.getStudent());
        result.setFinalizedAttempt(finalizedAttempt);
        result.setExpectedMark(metrics.maxScore);
        result.setActualMark(metrics.score);
        result.setGrade(toGradeLabel(metrics.maxScore > 0 ? (metrics.score / metrics.maxScore) * 100.0 : 0.0));
        result.setSubmittedAt(finalizedAttempt.getSubmittedAt());
        result.setGradedAt(Instant.now());
        result.setStatus("published");
        assessmentResultRepository.save(result);
    }

    private String toGradeLabel(double percentage) {
        if (percentage >= 80) {
            return "A";
        }
        if (percentage >= 70) {
            return "B";
        }
        if (percentage >= 60) {
            return "C";
        }
        if (percentage >= 50) {
            return "D";
        }
        return "U";
    }

    private Topic resolveSingleTopic(List<AssessmentQuestion> questions) {
        if (questions == null || questions.isEmpty()) {
            return null;
        }
        Topic topic = null;
        for (AssessmentQuestion question : questions) {
            Topic candidate = question.getQuestion() != null ? question.getQuestion().getTopic() : null;
            if (candidate == null) {
                return null;
            }
            if (topic == null) {
                topic = candidate;
                continue;
            }
            if (!topic.getId().equals(candidate.getId())) {
                return null;
            }
        }
        return topic;
    }

    private StudentPracticeSessionDto buildPracticeSessionDto(AssessmentAttempt attempt,
                                                              List<AssessmentQuestion> assessmentQuestions,
                                                              List<AttemptAnswer> answers,
                                                              String mode,
                                                              String title,
                                                              Topic topic,
                                                              boolean includeQuestions) {
        AssessmentEnrollment enrollment = attempt.getAssessmentEnrollment();
        AssessmentAssignment assignment = enrollment.getAssessmentAssignment();
        Assessment assessment = assignment.getAssessment();
        PracticeSessionMetrics metrics = calculatePracticeSessionMetrics(assessmentQuestions, answers, attempt);
        Integer durationMinutes = resolveDurationMinutes(attempt.getStartedAt(), attempt.getSubmittedAt());
        double maxScore = metrics.maxScore > 0 ? metrics.maxScore : (attempt.getMaxScore() == null ? 0.0 : attempt.getMaxScore());
        double score = metrics.score > 0 ? metrics.score : (attempt.getTotalScore() == null ? 0.0 : attempt.getTotalScore());

        List<StudentPracticeQuestionDto> questionDtos = includeQuestions
            ? assessmentQuestions.stream().map(this::toPracticeQuestionDto).toList()
            : List.of();

        return StudentPracticeSessionDto.builder()
            .sessionId(attempt.getId().toString())
            .assessmentId(assessment.getId().toString())
            .assignmentId(assignment.getId().toString())
            .enrollmentId(enrollment.getId().toString())
            .subjectId(assessment.getSubject().getId().toString())
            .subjectName(assessment.getSubject().getName())
            .topicId(topic != null && topic.getId() != null ? topic.getId().toString() : null)
            .topicName(topic != null ? topic.getName() : null)
            .mode(normalizePracticeMode(mode))
            .title(title)
            .status(attempt.getSubmittedAt() == null ? "in_progress" : "completed")
            .startedAt(attempt.getStartedAt())
            .submittedAt(attempt.getSubmittedAt())
            .questionCount(metrics.totalQuestions)
            .answeredCount(metrics.answeredCount)
            .correctCount(metrics.correctCount)
            .score(roundOneDecimal(score))
            .maxScore(maxScore > 0 ? roundOneDecimal(maxScore) : null)
            .percentage(maxScore > 0 ? roundOneDecimal((score / maxScore) * 100.0) : null)
            .durationMinutes(durationMinutes)
            .questions(questionDtos)
            .build();
    }

    private StudentPracticeQuestionDto toPracticeQuestionDto(AssessmentQuestion assessmentQuestion) {
        Question question = assessmentQuestion.getQuestion();
        List<String> optionTexts = extractOptionTexts(question);
        List<String> expectedAnswers = extractExpectedAnswers(question);
        boolean multipleSelection = optionTexts.size() > 0 && expectedAnswers.size() > 1;
        String questionType = optionTexts.isEmpty() ? "input" : (multipleSelection ? "multiple" : "single");

        Topic topic = question.getTopic();
        return StudentPracticeQuestionDto.builder()
            .assessmentQuestionId(assessmentQuestion.getId().toString())
            .questionId(question.getId().toString())
            .topicId(topic != null && topic.getId() != null ? topic.getId().toString() : null)
            .topicName(topic != null ? topic.getName() : null)
            .prompt(question.getStem())
            .questionType(questionType)
            .maxScore(roundOneDecimal(resolveQuestionMaxScore(assessmentQuestion)))
            .options(optionTexts)
            .multipleSelection(multipleSelection)
            .build();
    }

    private List<String> extractOptionTexts(Question question) {
        if (question == null || question.getRubricJson() == null || question.getRubricJson().isNull()) {
            return List.of();
        }
        JsonNode options = question.getRubricJson().path("options");
        if (!options.isArray()) {
            if ("true_false".equalsIgnoreCase(question.getQuestionTypeCode())) {
                return List.of("True", "False");
            }
            return List.of();
        }

        List<String> result = new ArrayList<>();
        options.forEach(option -> {
            if (option == null || option.isNull()) {
                return;
            }
            if (option.isTextual()) {
                String value = option.asText();
                if (value != null && !value.isBlank()) {
                    result.add(value);
                }
                return;
            }
            if (option.isObject()) {
                String text = option.path("text").asText("");
                if (text.isBlank()) {
                    text = option.path("label").asText("");
                }
                if (text.isBlank()) {
                    text = option.path("value").asText("");
                }
                if (!text.isBlank()) {
                    result.add(text);
                }
            }
        });
        return result;
    }

    private String normalizePlanRuntimeStatus(String status) {
        String normalized = status == null ? "" : status.trim().toLowerCase();
        return switch (normalized) {
            case "active", "in_progress", "in-progress" -> "active";
            case "completed", "done" -> "completed";
            case "cancelled", "canceled" -> "cancelled";
            case "on_hold", "on hold", "paused" -> "on_hold";
            default -> "active";
        };
    }

    private List<StudentDto> listByClassSubject(UUID classSubjectId) {
        List<User> students = studentSubjectEnrolmentRepository.findDistinctStudentsByClassSubjectId(classSubjectId);
        if (students.isEmpty()) {
            return List.of();
        }

        String resolvedSubjectId = classSubjectRepository.findById(classSubjectId)
            .map(ClassSubject::getSubject)
            .filter(Objects::nonNull)
            .map(Subject::getId)
            .map(UUID::toString)
            .orElse(null);

        Map<UUID, Set<String>> subjectIdsByStudent = new HashMap<>();
        if (resolvedSubjectId != null) {
            for (User student : students) {
                subjectIdsByStudent.put(student.getId(), Set.of(resolvedSubjectId));
            }
        }
        return toStudentDtos(
            students,
            subjectIdsByStudent,
            resolvedSubjectId == null ? null : List.of(resolvedSubjectId)
        );
    }

    private List<StudentDto> listByClass(UUID classId) {
        List<Enrolment> enrolments = enrolmentRepository.findByClassEntity_Id(classId);
        if (enrolments.isEmpty()) {
            return List.of();
        }
        List<User> students = enrolments.stream()
            .filter(enrolment -> enrolment.getDeletedAt() == null)
            .map(Enrolment::getStudent)
            .filter(student -> student != null && student.getDeletedAt() == null)
            .collect(Collectors.toMap(User::getId, user -> user, (left, right) -> left, HashMap::new))
            .values()
            .stream()
            .toList();
        return toStudentDtos(students);
    }

    private List<StudentDto> listBySubject(UUID subjectId) {
        String subjectIdValue = subjectId.toString();
        List<User> directStudents = studentSubjectEnrolmentRepository.findDistinctStudentsBySubjectId(subjectId);
        if (!directStudents.isEmpty()) {
            Map<UUID, Set<String>> subjectIdsByStudent = new HashMap<>();
            for (User student : directStudents) {
                subjectIdsByStudent.put(student.getId(), Set.of(subjectIdValue));
            }
            return toStudentDtos(directStudents, subjectIdsByStudent, List.of(subjectIdValue));
        }

        List<ClassSubject> classSubjects = classSubjectRepository.findBySubject_IdAndDeletedAtIsNull(subjectId);
        if (classSubjects.isEmpty()) {
            return List.of();
        }
        Set<UUID> classIds = classSubjects.stream()
            .map(ClassSubject::getClassEntity)
            .filter(classEntity -> classEntity != null)
            .map(ClassEntity::getId)
            .collect(Collectors.toSet());
        if (classIds.isEmpty()) {
            return List.of();
        }
        List<Enrolment> classEnrolments = enrolmentRepository.findByClassEntity_IdIn(classIds.stream().toList());
        List<User> students = classEnrolments.stream()
            .filter(enrolment -> enrolment.getDeletedAt() == null)
            .map(Enrolment::getStudent)
            .filter(student -> student != null && student.getDeletedAt() == null)
            .collect(Collectors.toMap(User::getId, user -> user, (left, right) -> left, HashMap::new))
            .values()
            .stream()
            .toList();
        Map<UUID, Set<String>> subjectIdsByStudent = new HashMap<>();
        for (User student : students) {
            subjectIdsByStudent.put(student.getId(), Set.of(subjectIdValue));
        }
        return toStudentDtos(students, subjectIdsByStudent, List.of(subjectIdValue));
    }

    private List<StudentDto> toStudentDtos(List<User> students) {
        return toStudentDtos(students, null, null);
    }

    private List<StudentDto> toStudentDtos(List<User> students,
                                           Map<UUID, Set<String>> subjectIdsByStudentOverride,
                                           List<String> fallbackSubjectsOverride) {
        if (students == null || students.isEmpty()) {
            return List.of();
        }

        List<User> uniqueStudents = students.stream()
            .filter(Objects::nonNull)
            .filter(user -> user.getDeletedAt() == null)
            .collect(Collectors.toMap(User::getId, user -> user, (left, right) -> left, LinkedHashMap::new))
            .values()
            .stream()
            .toList();
        if (uniqueStudents.isEmpty()) {
            return List.of();
        }

        List<UUID> studentIds = uniqueStudents.stream().map(User::getId).toList();
        Map<UUID, StudentProfile> profilesByStudent = studentProfileRepository.findByUserIdInAndDeletedAtIsNull(studentIds)
            .stream()
            .collect(Collectors.toMap(StudentProfile::getUserId, profile -> profile, (left, right) -> left));
        Map<UUID, Set<String>> subjectIdsByStudent = subjectIdsByStudentOverride != null
            ? subjectIdsByStudentOverride
            : resolveSubjectIdsForStudents(studentIds);
        List<String> fallbackSubjects = fallbackSubjectsOverride != null
            ? fallbackSubjectsOverride
            : fallbackSubjects();

        List<UUID> studentsNeedingAttributeFallback = uniqueStudents.stream()
            .map(User::getId)
            .filter(studentId -> {
                StudentProfile profile = profilesByStudent.get(studentId);
                return profile == null
                    || profile.getOverall() == null
                    || profile.getStrength() == null || profile.getStrength().isBlank()
                    || profile.getPerformance() == null || profile.getPerformance().isBlank()
                    || profile.getEngagement() == null || profile.getEngagement().isBlank();
            })
            .toList();

        Map<UUID, List<StudentAttribute>> attributesByStudent = Map.of();
        if (!studentsNeedingAttributeFallback.isEmpty()) {
            Set<UUID> scopedSubjectIds = new LinkedHashSet<>();
            if (subjectIdsByStudentOverride != null && !subjectIdsByStudentOverride.isEmpty()) {
                subjectIdsByStudentOverride.values().stream()
                    .filter(Objects::nonNull)
                    .flatMap(Set::stream)
                    .filter(Objects::nonNull)
                    .forEach(subjectValue -> {
                        try {
                            scopedSubjectIds.add(UUID.fromString(subjectValue));
                        } catch (IllegalArgumentException ignored) {
                            // ignore malformed values
                        }
                    });
            } else if (fallbackSubjectsOverride != null && !fallbackSubjectsOverride.isEmpty()) {
                fallbackSubjectsOverride.stream()
                    .filter(Objects::nonNull)
                    .forEach(subjectValue -> {
                        try {
                            scopedSubjectIds.add(UUID.fromString(subjectValue));
                        } catch (IllegalArgumentException ignored) {
                            // ignore malformed values
                        }
                    });
            }

            List<StudentAttribute> attributes = scopedSubjectIds.isEmpty()
                ? studentAttributeRepository.findByStudent_IdIn(studentsNeedingAttributeFallback)
                : studentAttributeRepository.findByStudent_IdInAndSkill_Subject_IdIn(
                    studentsNeedingAttributeFallback,
                    scopedSubjectIds.stream().toList()
                );
            attributesByStudent = attributes.stream()
                .collect(Collectors.groupingBy(attribute -> attribute.getStudent().getId()));
        }

        List<StudentDto> dtos = new ArrayList<>(uniqueStudents.size());
        for (User user : uniqueStudents) {
            StudentProfile profile = profilesByStudent.get(user.getId());
            List<StudentAttribute> attributes = attributesByStudent.getOrDefault(user.getId(), List.of());

            double overall = profile != null && profile.getOverall() != null
                ? profile.getOverall()
                : computeOverall(attributes);
            String strength = profile != null && profile.getStrength() != null && !profile.getStrength().isBlank()
                ? profile.getStrength()
                : computeStrength(attributes);
            String performance = profile != null && profile.getPerformance() != null && !profile.getPerformance().isBlank()
                ? profile.getPerformance()
                : performanceFromOverall(overall);
            String engagement = profile != null && profile.getEngagement() != null && !profile.getEngagement().isBlank()
                ? profile.getEngagement()
                : engagementFromAttributes(attributes);

            Set<String> resolvedSubjectIds = subjectIdsByStudent.get(user.getId());
            List<String> finalSubjects = (resolvedSubjectIds == null || resolvedSubjectIds.isEmpty())
                ? fallbackSubjects
                : resolvedSubjectIds.stream().toList();

            dtos.add(StudentDto.builder()
                .id(user.getId().toString())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .overall(overall)
                .strength(strength)
                .performance(performance)
                .engagement(engagement)
                .subjects(finalSubjects)
                .build());
        }
        return dtos;
    }

    private Map<UUID, Set<String>> resolveSubjectIdsForStudents(List<UUID> studentIds) {
        if (studentIds == null || studentIds.isEmpty()) {
            return Map.of();
        }

        Map<UUID, Set<String>> subjectsByStudent = new HashMap<>();

        List<StudentSubjectEnrolment> directEnrolments =
            studentSubjectEnrolmentRepository.findByStudent_IdInAndDeletedAtIsNull(studentIds);
        for (StudentSubjectEnrolment enrolment : directEnrolments) {
            if (enrolment.getStudent() == null || enrolment.getClassSubject() == null || enrolment.getClassSubject().getSubject() == null) {
                continue;
            }
            subjectsByStudent
                .computeIfAbsent(enrolment.getStudent().getId(), key -> new HashSet<>())
                .add(enrolment.getClassSubject().getSubject().getId().toString());
        }

        List<UUID> unresolvedStudentIds = studentIds.stream()
            .filter(id -> !subjectsByStudent.containsKey(id) || subjectsByStudent.get(id).isEmpty())
            .toList();
        if (unresolvedStudentIds.isEmpty()) {
            return subjectsByStudent;
        }

        List<Enrolment> enrolments = enrolmentRepository.findByStudent_IdIn(unresolvedStudentIds).stream()
            .filter(enrolment -> enrolment.getDeletedAt() == null)
            .toList();
        if (enrolments.isEmpty()) {
            return subjectsByStudent;
        }

        Set<UUID> classIds = enrolments.stream()
            .map(Enrolment::getClassEntity)
            .filter(Objects::nonNull)
            .map(ClassEntity::getId)
            .collect(Collectors.toSet());
        if (classIds.isEmpty()) {
            return subjectsByStudent;
        }

        Map<UUID, Set<String>> subjectsByClass = new HashMap<>();
        classSubjectRepository.findByClassEntity_IdInAndDeletedAtIsNull(classIds.stream().toList())
            .forEach(classSubject -> {
                if (classSubject.getClassEntity() == null || classSubject.getSubject() == null) {
                    return;
                }
                subjectsByClass
                    .computeIfAbsent(classSubject.getClassEntity().getId(), key -> new HashSet<>())
                    .add(classSubject.getSubject().getId().toString());
            });

        for (Enrolment enrolment : enrolments) {
            if (enrolment.getStudent() == null || enrolment.getClassEntity() == null) {
                continue;
            }
            Set<String> classSubjectIds = subjectsByClass.get(enrolment.getClassEntity().getId());
            if (classSubjectIds == null || classSubjectIds.isEmpty()) {
                continue;
            }
            subjectsByStudent
                .computeIfAbsent(enrolment.getStudent().getId(), key -> new HashSet<>())
                .addAll(classSubjectIds);
        }

        return subjectsByStudent;
    }

    private List<String> fallbackSubjects() {
        return subjectRepository.findAllByDeletedAtIsNull().stream()
            .map(subject -> subject.getId().toString())
            .toList();
    }

    private double computeOverall(List<StudentAttribute> attributes) {
        if (attributes == null || attributes.isEmpty()) {
            return 0.0;
        }
        double average = attributes.stream()
            .map(StudentAttribute::getCurrentScore)
            .filter(score -> score != null)
            .mapToDouble(Double::doubleValue)
            .average()
            .orElse(0.0);
        return Math.round(average * 10.0) / 10.0;
    }

    private String computeStrength(List<StudentAttribute> attributes) {
        if (attributes == null || attributes.isEmpty()) {
            return "General";
        }
        return attributes.stream()
            .filter(attribute -> attribute.getSkill() != null)
            .max(Comparator.comparingDouble(attribute -> attribute.getCurrentScore() == null ? 0.0 : attribute.getCurrentScore()))
            .map(attribute -> attribute.getSkill().getName())
            .filter(name -> name != null && !name.isBlank())
            .orElse("General");
    }

    private String performanceFromOverall(double overall) {
        if (overall >= 85.0) {
            return "Excellent";
        }
        if (overall >= 70.0) {
            return "Good";
        }
        if (overall >= 55.0) {
            return "Average";
        }
        if (overall >= 40.0) {
            return "Developing";
        }
        return "Needs Support";
    }

    private String engagementFromAttributes(List<StudentAttribute> attributes) {
        if (attributes == null || attributes.isEmpty()) {
            return "Unknown";
        }
        Instant latest = attributes.stream()
            .map(StudentAttribute::getLastAssessed)
            .filter(value -> value != null)
            .max(Comparator.naturalOrder())
            .orElse(null);
        if (latest == null) {
            return "Unknown";
        }
        long days = Duration.between(latest, Instant.now()).toDays();
        if (days <= 7) {
            return "High";
        }
        if (days <= 30) {
            return "Medium";
        }
        return "Low";
    }

    private int deriveUnitNumber(Topic topic, int fallbackTopicPosition) {
        String source = ((topic.getCode() != null ? topic.getCode() : "") + " " + (topic.getName() != null ? topic.getName() : ""))
            .toLowerCase();

        Matcher explicitMatcher = UNIT_PATTERN.matcher(source);
        if (explicitMatcher.find()) {
            int value = parsePositiveInt(explicitMatcher.group(1));
            if (value > 0) {
                return value;
            }
        }

        Matcher prefixMatcher = UNIT_PREFIX_PATTERN.matcher(source);
        if (prefixMatcher.find()) {
            int value = parsePositiveInt(prefixMatcher.group(1));
            if (value > 0) {
                return value;
            }
        }

        int sequence = topic.getSequenceIndex() != null && topic.getSequenceIndex() > 0
            ? topic.getSequenceIndex()
            : fallbackTopicPosition + 1;
        return Math.max(1, ((sequence - 1) / TOPICS_PER_UNIT_FALLBACK) + 1);
    }

    private int parsePositiveInt(String value) {
        if (value == null || value.isBlank()) {
            return -1;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return -1;
        }
    }

    private double roundOneDecimal(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private String resolveAttemptLevel(AssessmentAttempt attempt) {
        if (attempt == null) {
            return null;
        }

        if (attempt.getFinalGrade() != null && !attempt.getFinalGrade().isBlank()) {
            return attempt.getFinalGrade().trim();
        }

        if (attempt.getGradingStatusCode() != null && !attempt.getGradingStatusCode().isBlank()) {
            return attempt.getGradingStatusCode().trim().toLowerCase().replace('_', ' ');
        }

        Double score = attempt.getFinalScore() != null ? attempt.getFinalScore() : attempt.getTotalScore();
        Double maxScore = attempt.getMaxScore();
        if (score == null || maxScore == null || maxScore <= 0) {
            return null;
        }
        double percent = (score / maxScore) * 100.0;
        if (percent >= 80) {
            return "excellent";
        }
        if (percent >= 65) {
            return "good";
        }
        if (percent >= 50) {
            return "average";
        }
        return "needs support";
    }

    private static final class AttemptMetrics {
        private static final AttemptMetrics ZERO = new AttemptMetrics(0, 0, 0.0, 0.0);

        private final int answeredCount;
        private final int correctCount;
        private final double score;
        private final double maxScore;

        private AttemptMetrics(int answeredCount, int correctCount, double score, double maxScore) {
            this.answeredCount = answeredCount;
            this.correctCount = correctCount;
            this.score = score;
            this.maxScore = maxScore;
        }
    }

    private static final class PracticeEvaluation {
        private final boolean correct;
        private final boolean skipped;
        private final double score;
        private final boolean autoGraded;
        private final String feedback;

        private PracticeEvaluation(boolean correct,
                                   boolean skipped,
                                   double score,
                                   boolean autoGraded,
                                   String feedback) {
            this.correct = correct;
            this.skipped = skipped;
            this.score = score;
            this.autoGraded = autoGraded;
            this.feedback = feedback;
        }
    }

    private static final class PracticeSessionMetrics {
        private final int totalQuestions;
        private final int answeredCount;
        private final int correctCount;
        private final double score;
        private final double maxScore;

        private PracticeSessionMetrics(int totalQuestions,
                                       int answeredCount,
                                       int correctCount,
                                       double score,
                                       double maxScore) {
            this.totalQuestions = totalQuestions;
            this.answeredCount = answeredCount;
            this.correctCount = correctCount;
            this.score = score;
            this.maxScore = maxScore;
        }
    }

    private static final class TeacherAccumulator {
        private final UUID id;
        private final String firstName;
        private final String lastName;
        private final String email;
        private final Set<String> subjectNames = new LinkedHashSet<>();
        private final Set<String> classNames = new LinkedHashSet<>();
        private final Set<String> homeroomClassNames = new LinkedHashSet<>();

        private TeacherAccumulator(UUID id, String firstName, String lastName, String email) {
            this.id = id;
            this.firstName = firstName;
            this.lastName = lastName;
            this.email = email;
        }

        private static TeacherAccumulator from(User teacher) {
            return new TeacherAccumulator(teacher.getId(), teacher.getFirstName(), teacher.getLastName(), teacher.getEmail());
        }

        private StudentTeacherDto toDto() {
            return StudentTeacherDto.builder()
                .id(id.toString())
                .firstName(firstName)
                .lastName(lastName)
                .email(email)
                .subjectNames(subjectNames.stream().toList())
                .classNames(classNames.stream().toList())
                .homeroomClassNames(homeroomClassNames.stream().toList())
                .build();
        }
    }
}
