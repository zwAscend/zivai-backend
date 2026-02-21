package zw.co.zivai.core_backend.services.students;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
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

import lombok.RequiredArgsConstructor;
import zw.co.zivai.core_backend.dtos.students.StudentAssessmentDetailDto;
import zw.co.zivai.core_backend.dtos.students.StudentAssessmentHistoryItemDto;
import zw.co.zivai.core_backend.dtos.students.StudentDto;
import zw.co.zivai.core_backend.dtos.students.StudentChallengeEligibilityDto;
import zw.co.zivai.core_backend.dtos.students.StudentSubjectOverviewDto;
import zw.co.zivai.core_backend.dtos.students.StudentSubjectOverviewTopicDto;
import zw.co.zivai.core_backend.dtos.students.StudentSubjectOverviewUnitDto;
import zw.co.zivai.core_backend.dtos.students.StudentTeacherDto;
import zw.co.zivai.core_backend.dtos.assessments.TopicAnswerStat;
import zw.co.zivai.core_backend.exceptions.BadRequestException;
import zw.co.zivai.core_backend.exceptions.NotFoundException;
import zw.co.zivai.core_backend.models.lms.ClassSubject;
import zw.co.zivai.core_backend.models.lms.ClassEntity;
import zw.co.zivai.core_backend.models.lms.AssessmentAssignment;
import zw.co.zivai.core_backend.models.lms.AssessmentAttempt;
import zw.co.zivai.core_backend.models.lms.AssessmentEnrollment;
import zw.co.zivai.core_backend.models.lms.AssessmentResult;
import zw.co.zivai.core_backend.models.lms.Enrolment;
import zw.co.zivai.core_backend.models.lms.StudentAttribute;
import zw.co.zivai.core_backend.models.lms.StudentProfile;
import zw.co.zivai.core_backend.models.lms.StudentSubjectEnrolment;
import zw.co.zivai.core_backend.models.lms.Subject;
import zw.co.zivai.core_backend.models.lms.Topic;
import zw.co.zivai.core_backend.models.lms.User;
import zw.co.zivai.core_backend.repositories.assessments.AttemptAnswerRepository;
import zw.co.zivai.core_backend.repositories.assessments.AssessmentAttemptRepository;
import zw.co.zivai.core_backend.repositories.assessments.AssessmentEnrollmentRepository;
import zw.co.zivai.core_backend.repositories.assessments.AssessmentResultRepository;
import zw.co.zivai.core_backend.repositories.classroom.ClassSubjectRepository;
import zw.co.zivai.core_backend.repositories.classroom.EnrolmentRepository;
import zw.co.zivai.core_backend.repositories.assessments.QuestionRepository;
import zw.co.zivai.core_backend.repositories.subject.SubjectRepository;
import zw.co.zivai.core_backend.repositories.subject.TopicRepository;
import zw.co.zivai.core_backend.repositories.development.StudentAttributeRepository;
import zw.co.zivai.core_backend.repositories.students.StudentProfileRepository;
import zw.co.zivai.core_backend.repositories.classroom.StudentSubjectEnrolmentRepository;
import zw.co.zivai.core_backend.repositories.user.UserRepository;

@Service
@RequiredArgsConstructor
public class StudentService {
    private static final int TOPICS_PER_UNIT_FALLBACK = 4;
    private static final int MIN_QUESTIONS_FOR_SUBJECT_CHALLENGE = 6;
    private static final Pattern UNIT_PATTERN = Pattern.compile("unit[\\s\\-_]*(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern UNIT_PREFIX_PATTERN = Pattern.compile("\\bu[\\s\\-_]*(\\d+)", Pattern.CASE_INSENSITIVE);

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
    private final AssessmentEnrollmentRepository assessmentEnrollmentRepository;
    private final AssessmentAttemptRepository assessmentAttemptRepository;
    private final AssessmentResultRepository assessmentResultRepository;

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

        List<AssessmentEnrollment> enrollments = assessmentEnrollmentRepository
            .findStudentHistory(studentId, subjectId, fromDate, toDate);
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

    private List<StudentDto> listByClassSubject(UUID classSubjectId) {
        List<StudentSubjectEnrolment> enrolments =
            studentSubjectEnrolmentRepository.findByClassSubject_IdAndDeletedAtIsNull(classSubjectId);
        if (enrolments.isEmpty()) {
            return List.of();
        }
        List<User> students = enrolments.stream()
            .map(StudentSubjectEnrolment::getStudent)
            .filter(student -> student != null && student.getDeletedAt() == null)
            .collect(Collectors.toMap(User::getId, user -> user, (left, right) -> left, HashMap::new))
            .values()
            .stream()
            .toList();
        return toStudentDtos(students);
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
        List<StudentSubjectEnrolment> enrolments =
            studentSubjectEnrolmentRepository.findByClassSubject_Subject_IdAndDeletedAtIsNull(subjectId);
        if (!enrolments.isEmpty()) {
            List<User> students = enrolments.stream()
                .map(StudentSubjectEnrolment::getStudent)
                .filter(student -> student != null && student.getDeletedAt() == null)
                .collect(Collectors.toMap(User::getId, user -> user, (left, right) -> left, HashMap::new))
                .values()
                .stream()
                .toList();
            return toStudentDtos(students);
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
        return toStudentDtos(students);
    }

    private List<StudentDto> toStudentDtos(List<User> students) {
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
        Map<UUID, List<StudentAttribute>> attributesByStudent = studentAttributeRepository.findByStudent_IdIn(studentIds)
            .stream()
            .collect(Collectors.groupingBy(attribute -> attribute.getStudent().getId()));

        Map<UUID, Set<String>> subjectIdsByStudent = resolveSubjectIdsForStudents(studentIds);
        List<String> fallbackSubjects = fallbackSubjects();

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
