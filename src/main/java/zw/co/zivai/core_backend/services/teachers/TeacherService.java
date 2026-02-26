package zw.co.zivai.core_backend.services.teachers;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import zw.co.zivai.core_backend.dtos.common.PageResponse;
import zw.co.zivai.core_backend.dtos.teachers.TeacherAssessmentOverviewDto;
import zw.co.zivai.core_backend.dtos.teachers.TeacherBasicDto;
import zw.co.zivai.core_backend.dtos.teachers.TeacherClassDto;
import zw.co.zivai.core_backend.dtos.teachers.TeacherDashboardDto;
import zw.co.zivai.core_backend.dtos.teachers.TeacherStudentProfileSummaryDto;
import zw.co.zivai.core_backend.dtos.teachers.TeacherStudentSummaryDto;
import zw.co.zivai.core_backend.dtos.teachers.TeacherSubjectDto;
import zw.co.zivai.core_backend.exceptions.BadRequestException;
import zw.co.zivai.core_backend.exceptions.NotFoundException;
import zw.co.zivai.core_backend.models.lms.Assessment;
import zw.co.zivai.core_backend.models.lms.AssessmentAssignment;
import zw.co.zivai.core_backend.models.lms.AssessmentAttempt;
import zw.co.zivai.core_backend.models.lms.AssessmentEnrollment;
import zw.co.zivai.core_backend.models.lms.AssessmentResult;
import zw.co.zivai.core_backend.models.lms.ClassEntity;
import zw.co.zivai.core_backend.models.lms.ClassSubject;
import zw.co.zivai.core_backend.models.lms.Enrolment;
import zw.co.zivai.core_backend.models.lms.StudentPlan;
import zw.co.zivai.core_backend.models.lms.StudentProfile;
import zw.co.zivai.core_backend.models.lms.StudentSubjectEnrolment;
import zw.co.zivai.core_backend.models.lms.Subject;
import zw.co.zivai.core_backend.models.lms.User;
import zw.co.zivai.core_backend.models.lookups.Role;
import zw.co.zivai.core_backend.repositories.assessments.AssessmentAssignmentRepository;
import zw.co.zivai.core_backend.repositories.assessments.AssessmentAttemptRepository;
import zw.co.zivai.core_backend.repositories.assessments.AssessmentEnrollmentRepository;
import zw.co.zivai.core_backend.repositories.assessments.AssessmentResultRepository;
import zw.co.zivai.core_backend.repositories.classroom.ClassSubjectRepository;
import zw.co.zivai.core_backend.repositories.classroom.EnrolmentRepository;
import zw.co.zivai.core_backend.repositories.classroom.StudentSubjectEnrolmentRepository;
import zw.co.zivai.core_backend.repositories.development.StudentPlanRepository;
import zw.co.zivai.core_backend.repositories.notification.NotificationRepository;
import zw.co.zivai.core_backend.repositories.students.StudentProfileRepository;
import zw.co.zivai.core_backend.repositories.user.UserRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class TeacherService {
    private static final UUID EMPTY_UUID = new UUID(0L, 0L);

    private final UserRepository userRepository;
    private final ClassSubjectRepository classSubjectRepository;
    private final EnrolmentRepository enrolmentRepository;
    private final StudentSubjectEnrolmentRepository studentSubjectEnrolmentRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final StudentPlanRepository studentPlanRepository;
    private final AssessmentAssignmentRepository assessmentAssignmentRepository;
    private final AssessmentEnrollmentRepository assessmentEnrollmentRepository;
    private final AssessmentResultRepository assessmentResultRepository;
    private final AssessmentAttemptRepository assessmentAttemptRepository;
    private final NotificationRepository notificationRepository;

    public TeacherBasicDto getTeacher(UUID teacherId) {
        User teacher = requireTeacher(teacherId);
        return toTeacherBasicDto(teacher);
    }

    public List<TeacherSubjectDto> getTeacherSubjects(UUID teacherId) {
        requireTeacher(teacherId);
        List<ClassSubject> teachingLinks = classSubjectRepository.findByTeacher_IdAndDeletedAtIsNull(teacherId);
        if (teachingLinks.isEmpty()) {
            return List.of();
        }

        List<StudentSubjectEnrolment> subjectEnrolments = studentSubjectEnrolmentRepository
            .findByClassSubject_IdInAndDeletedAtIsNull(extractClassSubjectIds(teachingLinks));

        Map<UUID, Set<UUID>> studentsBySubject = new HashMap<>();
        for (StudentSubjectEnrolment enrolment : subjectEnrolments) {
            if (enrolment.getDeletedAt() != null || enrolment.getStudent() == null || enrolment.getStudent().getDeletedAt() != null) {
                continue;
            }
            Subject subject = enrolment.getClassSubject() != null ? enrolment.getClassSubject().getSubject() : null;
            if (subject == null || subject.getDeletedAt() != null) {
                continue;
            }
            studentsBySubject.computeIfAbsent(subject.getId(), key -> new LinkedHashSet<>())
                .add(enrolment.getStudent().getId());
        }

        Map<UUID, List<ClassSubject>> linksBySubject = teachingLinks.stream()
            .filter(link -> link.getDeletedAt() == null)
            .filter(link -> link.getSubject() != null && link.getSubject().getDeletedAt() == null)
            .collect(Collectors.groupingBy(link -> link.getSubject().getId()));

        return linksBySubject.entrySet().stream()
            .map(entry -> {
                Subject subject = entry.getValue().get(0).getSubject();
                Set<UUID> classIds = entry.getValue().stream()
                    .map(ClassSubject::getClassEntity)
                    .filter(Objects::nonNull)
                    .filter(classEntity -> classEntity.getDeletedAt() == null)
                    .map(ClassEntity::getId)
                    .collect(Collectors.toCollection(LinkedHashSet::new));

                return TeacherSubjectDto.builder()
                    .subjectId(subject.getId().toString())
                    .subjectCode(subject.getCode())
                    .subjectName(subject.getName())
                    .classCount(classIds.size())
                    .studentCount(studentsBySubject.getOrDefault(subject.getId(), Set.of()).size())
                    .build();
            })
            .sorted(Comparator.comparing(TeacherSubjectDto::getSubjectName, String.CASE_INSENSITIVE_ORDER))
            .toList();
    }

    public List<TeacherClassDto> getTeacherClasses(UUID teacherId, UUID subjectId) {
        requireTeacher(teacherId);
        TeacherScope scope = resolveScope(teacherId, subjectId, null);
        if (scope.classSubjects().isEmpty()) {
            return List.of();
        }

        List<Enrolment> enrolments = enrolmentRepository.findByClassEntity_IdInAndDeletedAtIsNull(new ArrayList<>(scope.classIds()));
        Map<UUID, Set<UUID>> studentsByClass = new HashMap<>();
        for (Enrolment enrolment : enrolments) {
            if (enrolment.getStudent() == null || enrolment.getStudent().getDeletedAt() != null) {
                continue;
            }
            studentsByClass.computeIfAbsent(enrolment.getClassEntity().getId(), key -> new LinkedHashSet<>())
                .add(enrolment.getStudent().getId());
        }

        Map<UUID, List<ClassSubject>> linksByClass = scope.classSubjects().stream()
            .collect(Collectors.groupingBy(link -> link.getClassEntity().getId()));

        return linksByClass.entrySet().stream()
            .map(entry -> {
                ClassEntity classEntity = entry.getValue().get(0).getClassEntity();
                long subjectCount = entry.getValue().stream()
                    .map(ClassSubject::getSubject)
                    .filter(Objects::nonNull)
                    .map(Subject::getId)
                    .distinct()
                    .count();

                return TeacherClassDto.builder()
                    .classId(classEntity.getId().toString())
                    .code(classEntity.getCode())
                    .name(classEntity.getName())
                    .gradeLevel(classEntity.getGradeLevel())
                    .academicYear(classEntity.getAcademicYear())
                    .subjectCount(subjectCount)
                    .studentCount(studentsByClass.getOrDefault(classEntity.getId(), Set.of()).size())
                    .build();
            })
            .sorted(Comparator.comparing(TeacherClassDto::getName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
            .toList();
    }

    public List<TeacherAssessmentOverviewDto> getAssessmentOverview(UUID teacherId,
                                                                    UUID subjectId,
                                                                    String status,
                                                                    UUID studentId,
                                                                    String search,
                                                                    String from,
                                                                    String to) {
        requireTeacher(teacherId);

        Instant fromDate = parseOptionalInstant(from, false);
        Instant toDate = parseOptionalInstant(to, true);
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new BadRequestException("from must be before or equal to to");
        }

        boolean filterMarked = status != null && status.equalsIgnoreCase("marked");
        String statusFilter = filterMarked ? null : normalizeNullable(status);

        TeacherScope scope = resolveScope(teacherId, subjectId, null);
        List<AssessmentAssignment> assignments = assessmentAssignmentRepository.findTeacherAssignments(
            teacherId,
            scope.classIdsOrSentinel(),
            !scope.classIds().isEmpty(),
            subjectId,
            statusFilter,
            normalizeSearch(search),
            fromDate,
            toDate
        );
        if (assignments.isEmpty()) {
            return List.of();
        }

        List<AssessmentAssignment> safeAssignments = assignments.stream()
            .filter(Objects::nonNull)
            .filter(assignment -> assignment.getId() != null)
            .filter(assignment -> assignment.getAssessment() != null)
            .filter(assignment -> assignment.getAssessment().getSubject() != null)
            .filter(assignment -> assignment.getAssessment().getSubject().getId() != null)
            .toList();
        if (safeAssignments.isEmpty()) {
            return List.of();
        }

        List<UUID> assignmentIds = safeAssignments.stream().map(AssessmentAssignment::getId).toList();

        List<AssessmentResult> results = studentId != null
            ? assessmentResultRepository.findByAssessmentAssignment_IdInAndStudent_IdAndDeletedAtIsNull(assignmentIds, studentId)
            : assessmentResultRepository.findByAssessmentAssignment_IdInAndDeletedAtIsNull(assignmentIds);
        Map<UUID, List<AssessmentResult>> resultsByAssignment = results.stream()
            .filter(Objects::nonNull)
            .filter(result -> result.getAssessmentAssignment() != null && result.getAssessmentAssignment().getId() != null)
            .collect(Collectors.groupingBy(result -> result.getAssessmentAssignment().getId()));

        Map<UUID, String> enrollmentStatusByAssignment = new HashMap<>();
        if (studentId != null) {
            List<AssessmentEnrollment> enrollments = assessmentEnrollmentRepository
                .findByAssessmentAssignment_IdInAndStudent_IdAndDeletedAtIsNull(assignmentIds, studentId);
            enrollmentStatusByAssignment = enrollments.stream()
                .collect(Collectors.toMap(
                    enrollment -> enrollment.getAssessmentAssignment().getId(),
                    AssessmentEnrollment::getStatusCode,
                    (left, right) -> left
                ));
        }

        List<TeacherAssessmentOverviewDto> overview = new ArrayList<>();
        for (AssessmentAssignment assignment : safeAssignments) {
            Assessment assessment = assignment.getAssessment();
            if (assessment == null) {
                log.warn("Skipping malformed assessment assignment {} in overview for teacher {}", assignment.getId(), teacherId);
                continue;
            }
            Subject subject = assessment.getSubject();
            if (subject == null || subject.getId() == null) {
                log.warn("Skipping malformed assessment assignment {} in overview for teacher {}", assignment.getId(), teacherId);
                continue;
            }

            List<AssessmentResult> assignmentResults = resultsByAssignment.getOrDefault(assignment.getId(), List.of());
            if (studentId != null
                && assignmentResults.isEmpty()
                && !enrollmentStatusByAssignment.containsKey(assignment.getId())) {
                continue;
            }

            long attempted = assignmentResults.size();
            long passed = assignmentResults.stream().filter(this::isPass).count();
            long failed = Math.max(0, attempted - passed);
            double avgScore = assignmentResults.stream()
                .map(AssessmentResult::getActualMark)
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);
            double passRate = attempted > 0 ? (passed * 100.0) / attempted : 0.0;

            AssessmentResult latestResult = assignmentResults.stream()
                .sorted(Comparator
                    .comparing(AssessmentResult::getSubmittedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(AssessmentResult::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .findFirst()
                .orElse(null);

            String studentStatus = null;
            if (studentId != null) {
                studentStatus = resolveStudentStatus(enrollmentStatusByAssignment.get(assignment.getId()), latestResult);
            }

            TeacherAssessmentOverviewDto dto = TeacherAssessmentOverviewDto.builder()
                .assignmentId(assignment.getId().toString())
                .assessmentId(assessment.getId() != null ? assessment.getId().toString() : null)
                .assessmentName(assessment.getName())
                .assessmentType(assessment.getAssessmentType())
                .assessmentStatus(assessment.getStatus())
                .subjectId(subject.getId().toString())
                .subjectName(subject.getName())
                .classId(assignment.getClassEntity() != null ? assignment.getClassEntity().getId().toString() : null)
                .className(assignment.getClassEntity() != null ? assignment.getClassEntity().getName() : null)
                .dueTime(assignment.getDueTime())
                .published(assignment.isPublished())
                .aiEnhanced(assessment.isAiEnhanced())
                .attempted(attempted)
                .submitted(attempted)
                .passed(passed)
                .failed(failed)
                .averageScore(roundTwo(avgScore))
                .passRate(roundTwo(passRate))
                .studentActualMark(latestResult != null ? latestResult.getActualMark() : null)
                .studentExpectedMark(latestResult != null ? latestResult.getExpectedMark() : null)
                .studentGrade(latestResult != null ? latestResult.getGrade() : null)
                .studentStatus(studentStatus)
                .build();

            if (!filterMarked || dto.getAttempted() > 0) {
                overview.add(dto);
            }
        }

        return overview.stream()
            .sorted(Comparator
                .comparing(TeacherAssessmentOverviewDto::getDueTime, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(TeacherAssessmentOverviewDto::getAssessmentName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
            .toList();
    }

    public PageResponse<TeacherStudentSummaryDto> getStudentsSummary(UUID teacherId,
                                                                     UUID subjectId,
                                                                     UUID classId,
                                                                     String performance,
                                                                     String planStatus,
                                                                     String query,
                                                                     int page,
                                                                     int size) {
        requireTeacher(teacherId);
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(size, 200));

        TeacherScope scope = resolveScope(teacherId, subjectId, classId);
        if (scope.classIds().isEmpty()) {
            return emptyPage(safePage, safeSize);
        }

        List<Enrolment> classEnrolments = enrolmentRepository.findByClassEntity_IdInAndDeletedAtIsNull(new ArrayList<>(scope.classIds()));
        Map<UUID, Set<UUID>> classIdsByStudent = new HashMap<>();
        Set<UUID> scopedStudentIds = new LinkedHashSet<>();
        for (Enrolment enrolment : classEnrolments) {
            if (enrolment.getStudent() == null || enrolment.getStudent().getDeletedAt() != null) {
                continue;
            }
            UUID studentId = enrolment.getStudent().getId();
            scopedStudentIds.add(studentId);
            classIdsByStudent.computeIfAbsent(studentId, key -> new LinkedHashSet<>())
                .add(enrolment.getClassEntity().getId());
        }

        if (scopedStudentIds.isEmpty()) {
            return emptyPage(safePage, safeSize);
        }

        List<StudentSubjectEnrolment> subjectEnrolments = scope.classSubjectIds().isEmpty()
            ? List.of()
            : studentSubjectEnrolmentRepository.findByClassSubject_IdInAndDeletedAtIsNull(new ArrayList<>(scope.classSubjectIds()));
        Map<UUID, Set<UUID>> subjectIdsByStudent = new HashMap<>();
        for (StudentSubjectEnrolment enrolment : subjectEnrolments) {
            if (enrolment.getDeletedAt() != null || enrolment.getStudent() == null || enrolment.getStudent().getDeletedAt() != null) {
                continue;
            }
            Subject subject = enrolment.getClassSubject() != null ? enrolment.getClassSubject().getSubject() : null;
            if (subject == null || subject.getDeletedAt() != null) {
                continue;
            }
            subjectIdsByStudent.computeIfAbsent(enrolment.getStudent().getId(), key -> new LinkedHashSet<>())
                .add(subject.getId());
        }

        List<User> students = userRepository.findByIdInAndDeletedAtIsNull(new ArrayList<>(scopedStudentIds)).stream()
            .filter(this::isStudent)
            .toList();
        if (students.isEmpty()) {
            return emptyPage(safePage, safeSize);
        }

        List<UUID> studentIds = students.stream().map(User::getId).toList();
        Map<UUID, StudentProfile> profileByStudent = studentProfileRepository.findByUserIdInAndDeletedAtIsNull(studentIds).stream()
            .collect(Collectors.toMap(StudentProfile::getUserId, profile -> profile));

        List<StudentPlan> studentPlans = subjectId != null
            ? studentPlanRepository.findByStudent_IdInAndSubject_IdAndDeletedAtIsNull(studentIds, subjectId)
            : studentPlanRepository.findByStudent_IdInAndDeletedAtIsNull(studentIds);
        Map<UUID, List<StudentPlan>> plansByStudent = studentPlans.stream()
            .filter(plan -> plan.getStudent() != null)
            .collect(Collectors.groupingBy(plan -> plan.getStudent().getId()));

        String normalizedPerformance = normalizeFilter(performance);
        String normalizedPlanStatus = normalizeFilter(planStatus);
        String normalizedQuery = normalizeSearch(query);

        List<TeacherStudentSummaryDto> summaries = new ArrayList<>();
        for (User student : students) {
            StudentProfile profile = profileByStudent.get(student.getId());
            List<StudentPlan> plans = plansByStudent.getOrDefault(student.getId(), List.of());
            StudentPlan primaryPlan = selectPrimaryPlan(plans);

            String performanceValue = resolvePerformance(profile);
            String performanceBucket = normalizePerformanceBucket(profile, performanceValue);
            if (normalizedPerformance != null
                && !normalizedPerformance.equals("all")
                && !normalizedPerformance.equals(performanceBucket)
                && !normalizeFilter(performanceValue).equals(normalizedPerformance)) {
                continue;
            }

            String planStatusValue = primaryPlan != null ? primaryPlan.getStatus() : null;
            if (normalizedPlanStatus != null && !normalizedPlanStatus.equals("all")) {
                if (planStatusValue == null || !normalizeFilter(planStatusValue).equals(normalizedPlanStatus)) {
                    continue;
                }
            }

            if (normalizedQuery != null) {
                String fullName = (student.getFirstName() + " " + student.getLastName()).toLowerCase(Locale.ROOT);
                String email = student.getEmail() != null ? student.getEmail().toLowerCase(Locale.ROOT) : "";
                if (!fullName.contains(normalizedQuery) && !email.contains(normalizedQuery)) {
                    continue;
                }
            }

            summaries.add(TeacherStudentSummaryDto.builder()
                .studentId(student.getId().toString())
                .firstName(student.getFirstName())
                .lastName(student.getLastName())
                .email(student.getEmail())
                .overall(profile != null ? profile.getOverall() : null)
                .performance(performanceValue)
                .engagement(profile != null ? profile.getEngagement() : null)
                .strength(profile != null ? profile.getStrength() : null)
                .subjectCount(subjectIdsByStudent.getOrDefault(student.getId(), Set.of()).size())
                .classCount(classIdsByStudent.getOrDefault(student.getId(), Set.of()).size())
                .planStatus(primaryPlan != null ? primaryPlan.getStatus() : null)
                .planProgress(primaryPlan != null ? primaryPlan.getCurrentProgress() : null)
                .activePlanName(primaryPlan != null && primaryPlan.getPlan() != null ? primaryPlan.getPlan().getName() : null)
                .build());
        }

        summaries.sort(Comparator
            .comparing(TeacherStudentSummaryDto::getFirstName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
            .thenComparing(TeacherStudentSummaryDto::getLastName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));

        long totalItems = summaries.size();
        int fromIndex = Math.min(safePage * safeSize, summaries.size());
        int toIndex = Math.min(fromIndex + safeSize, summaries.size());
        List<TeacherStudentSummaryDto> pageItems = summaries.subList(fromIndex, toIndex);
        int totalPages = safeSize > 0 ? (int) Math.ceil(totalItems / (double) safeSize) : 0;

        return PageResponse.<TeacherStudentSummaryDto>builder()
            .items(pageItems)
            .page(safePage)
            .size(safeSize)
            .totalItems(totalItems)
            .totalPages(totalPages)
            .build();
    }

    public TeacherStudentProfileSummaryDto getStudentProfileSummary(UUID teacherId, UUID studentId, UUID subjectId) {
        requireTeacher(teacherId);
        User student = userRepository.findByIdAndDeletedAtIsNull(studentId)
            .orElseThrow(() -> new NotFoundException("Student not found: " + studentId));
        if (!isStudent(student)) {
            throw new NotFoundException("Student not found: " + studentId);
        }

        TeacherScope scope = resolveScope(teacherId, subjectId, null);
        if (scope.classIds().isEmpty()) {
            throw new NotFoundException("Student is not linked to this teacher: " + studentId);
        }

        boolean linkedToTeacher = enrolmentRepository.findByClassEntity_IdInAndDeletedAtIsNull(new ArrayList<>(scope.classIds())).stream()
            .anyMatch(enrolment -> enrolment.getStudent() != null && studentId.equals(enrolment.getStudent().getId()));
        if (!linkedToTeacher) {
            throw new NotFoundException("Student is not linked to this teacher: " + studentId);
        }

        StudentProfile profile = studentProfileRepository.findByUserIdAndDeletedAtIsNull(studentId).orElse(null);

        List<StudentPlan> plans = subjectId != null
            ? studentPlanRepository.findByStudent_IdAndSubject_Id(studentId, subjectId)
            : studentPlanRepository.findByStudent_Id(studentId);
        List<StudentPlan> activePlans = plans.stream()
            .filter(plan -> plan.getDeletedAt() == null)
            .toList();
        StudentPlan latestPlan = selectPrimaryPlan(activePlans);

        List<AssessmentAssignment> assignments = assessmentAssignmentRepository.findTeacherAssignmentsForProfile(
            teacherId,
            scope.classIdsOrSentinel(),
            !scope.classIds().isEmpty(),
            subjectId
        );
        List<UUID> assignmentIds = assignments.stream().map(AssessmentAssignment::getId).toList();

        List<AssessmentEnrollment> enrollments = assignmentIds.isEmpty()
            ? List.of()
            : assessmentEnrollmentRepository.findByAssessmentAssignment_IdInAndStudent_IdAndDeletedAtIsNull(assignmentIds, studentId);
        Set<UUID> assignedIds = enrollments.stream()
            .map(enrollment -> enrollment.getAssessmentAssignment().getId())
            .collect(Collectors.toCollection(LinkedHashSet::new));

        List<AssessmentResult> results = assignedIds.isEmpty()
            ? List.of()
            : assessmentResultRepository.findByAssessmentAssignment_IdInAndStudent_IdAndDeletedAtIsNull(assignedIds, studentId);

        long reviewed = results.stream()
            .filter(result -> result.getGradedAt() != null
                || (result.getStatus() != null && result.getStatus().equalsIgnoreCase("published")))
            .count();
        Double averageScore = results.stream()
            .map(AssessmentResult::getActualMark)
            .filter(Objects::nonNull)
            .mapToDouble(Double::doubleValue)
            .average()
            .orElse(0.0);

        Map<UUID, AssessmentAssignment> assignmentsById = assignments.stream()
            .collect(Collectors.toMap(AssessmentAssignment::getId, assignment -> assignment, (left, right) -> left));

        AssessmentResult latestResult = results.stream()
            .sorted(Comparator
                .comparing(AssessmentResult::getSubmittedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(AssessmentResult::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
            .findFirst()
            .orElse(null);
        AssessmentAssignment latestAssignment = latestResult != null
            ? assignmentsById.get(latestResult.getAssessmentAssignment().getId())
            : enrollments.stream()
                .map(AssessmentEnrollment::getAssessmentAssignment)
                .sorted(Comparator
                    .comparing(AssessmentAssignment::getDueTime, Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(AssessmentAssignment::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .findFirst()
                .orElse(null);

        long activePlanCount = activePlans.stream()
            .filter(plan -> plan.getStatus() != null && plan.getStatus().equalsIgnoreCase("active"))
            .count();
        long completedPlanCount = activePlans.stream()
            .filter(plan -> plan.getStatus() != null && plan.getStatus().equalsIgnoreCase("completed"))
            .count();

        double avgPlanProgress = activePlans.stream()
            .map(StudentPlan::getCurrentProgress)
            .filter(Objects::nonNull)
            .mapToDouble(Double::doubleValue)
            .average()
            .orElse(0.0);

        return TeacherStudentProfileSummaryDto.builder()
            .teacherId(teacherId.toString())
            .studentId(studentId.toString())
            .student(TeacherStudentProfileSummaryDto.StudentProfile.builder()
                .id(student.getId().toString())
                .firstName(student.getFirstName())
                .lastName(student.getLastName())
                .email(student.getEmail())
                .overall(profile != null ? profile.getOverall() : null)
                .performance(profile != null ? profile.getPerformance() : null)
                .engagement(profile != null ? profile.getEngagement() : null)
                .strength(profile != null ? profile.getStrength() : null)
                .gradeLevel(profile != null ? profile.getGradeLevel() : null)
                .build())
            .planSummary(TeacherStudentProfileSummaryDto.PlanSummary.builder()
                .totalPlans(activePlans.size())
                .activePlans(activePlanCount)
                .completedPlans(completedPlanCount)
                .averageProgress(roundTwo(avgPlanProgress))
                .latestStatus(latestPlan != null ? latestPlan.getStatus() : null)
                .latestPlanName(latestPlan != null && latestPlan.getPlan() != null ? latestPlan.getPlan().getName() : null)
                .build())
            .assessmentSummary(TeacherStudentProfileSummaryDto.AssessmentSummary.builder()
                .totalAssigned(assignedIds.size())
                .attempted(results.size())
                .reviewed(reviewed)
                .averageScore(roundTwo(averageScore))
                .latestAssessmentName(latestAssignment != null ? latestAssignment.getAssessment().getName() : null)
                .latestDueTime(latestAssignment != null ? latestAssignment.getDueTime() : null)
                .latestScore(latestResult != null ? latestResult.getActualMark() : null)
                .latestGrade(latestResult != null ? latestResult.getGrade() : null)
                .build())
            .build();
    }

    public TeacherDashboardDto getDashboard(UUID teacherId, UUID subjectId) {
        requireTeacher(teacherId);
        TeacherScope scope = resolveScope(teacherId, subjectId, null);

        Set<UUID> studentIds = scope.classIds().isEmpty()
            ? Set.of()
            : enrolmentRepository.findByClassEntity_IdInAndDeletedAtIsNull(new ArrayList<>(scope.classIds())).stream()
                .map(Enrolment::getStudent)
                .filter(Objects::nonNull)
                .filter(student -> student.getDeletedAt() == null)
                .map(User::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        List<AssessmentAttempt> attempts = assessmentAttemptRepository.findTeacherSubmittedAttempts(
            teacherId,
            scope.classIdsOrSentinel(),
            !scope.classIds().isEmpty(),
            subjectId
        );

        long pending = attempts.stream()
            .filter(attempt -> attempt.getGradingStatusCode() == null
                || !attempt.getGradingStatusCode().equalsIgnoreCase("reviewed"))
            .count();
        long reviewed = attempts.stream()
            .filter(attempt -> attempt.getGradingStatusCode() != null
                && attempt.getGradingStatusCode().equalsIgnoreCase("reviewed"))
            .count();
        long autoGraded = attempts.stream()
            .filter(attempt -> attempt.getGradingStatusCode() != null
                && attempt.getGradingStatusCode().equalsIgnoreCase("auto_graded"))
            .count();

        double averageScore = attempts.stream()
            .map(AssessmentAttempt::getTotalScore)
            .filter(Objects::nonNull)
            .mapToDouble(Double::doubleValue)
            .average()
            .orElse(0.0);

        double averageConfidence = attempts.stream()
            .map(AssessmentAttempt::getAiConfidence)
            .filter(Objects::nonNull)
            .mapToDouble(Double::doubleValue)
            .average()
            .orElse(0.0);

        long unread = notificationRepository.countByRecipient_IdAndDeletedAtIsNullAndReadFalse(teacherId);
        long critical = notificationRepository.countByRecipient_IdAndDeletedAtIsNullAndReadFalseAndPriorityIgnoreCase(teacherId, "high");

        List<UUID> studentIdList = new ArrayList<>(studentIds);
        Map<UUID, StudentProfile> profilesByStudent = studentIdList.isEmpty()
            ? Map.of()
            : studentProfileRepository.findByUserIdInAndDeletedAtIsNull(studentIdList).stream()
                .collect(Collectors.toMap(StudentProfile::getUserId, profile -> profile));

        long masteryRiskCount = profilesByStudent.values().stream()
            .filter(this::isMasteryRisk)
            .count();

        List<StudentPlan> plans = studentIdList.isEmpty()
            ? List.of()
            : (subjectId != null
                ? studentPlanRepository.findByStudent_IdInAndSubject_IdAndDeletedAtIsNull(studentIdList, subjectId)
                : studentPlanRepository.findByStudent_IdInAndDeletedAtIsNull(studentIdList));
        long activePlans = plans.stream()
            .filter(plan -> plan.getStatus() != null && plan.getStatus().equalsIgnoreCase("active"))
            .count();

        return TeacherDashboardDto.builder()
            .teacherId(teacherId.toString())
            .subjectId(subjectId != null ? subjectId.toString() : null)
            .totalStudents(studentIds.size())
            .pendingSubmissions(pending)
            .reviewedSubmissions(reviewed)
            .autoGradedSubmissions(autoGraded)
            .averageScore(roundTwo(averageScore))
            .averageAiConfidence(roundTwo(averageConfidence))
            .unreadNotifications(unread)
            .criticalAlerts(critical)
            .masteryRiskCount(masteryRiskCount)
            .activePlans(activePlans)
            .build();
    }

    public PageResponse<AssessmentAttempt> getTeacherPendingSubmissionsRaw(UUID teacherId,
                                                                            UUID subjectId,
                                                                            UUID classId,
                                                                            UUID assessmentId,
                                                                            UUID studentId,
                                                                            String status,
                                                                            int page,
                                                                            int size) {
        requireTeacher(teacherId);
        TeacherScope scope = resolveScope(teacherId, subjectId, classId);

        Pageable pageable = PageRequest.of(
            Math.max(0, page),
            Math.max(1, Math.min(size, 200)),
            Sort.by(Sort.Order.desc("submittedAt"), Sort.Order.desc("createdAt"))
        );

        Page<AssessmentAttempt> attemptsPage = assessmentAttemptRepository.findTeacherPending(
            teacherId,
            scope.classIdsOrSentinel(),
            !scope.classIds().isEmpty(),
            subjectId,
            classId,
            assessmentId,
            studentId,
            normalizeNullable(status),
            pageable
        );

        return PageResponse.<AssessmentAttempt>builder()
            .items(attemptsPage.getContent())
            .page(attemptsPage.getNumber())
            .size(attemptsPage.getSize())
            .totalItems(attemptsPage.getTotalElements())
            .totalPages(attemptsPage.getTotalPages())
            .build();
    }

    private TeacherBasicDto toTeacherBasicDto(User teacher) {
        List<String> roles = teacher.getRoles().stream()
            .map(Role::getCode)
            .sorted(String.CASE_INSENSITIVE_ORDER)
            .toList();

        return TeacherBasicDto.builder()
            .id(teacher.getId().toString())
            .firstName(teacher.getFirstName())
            .lastName(teacher.getLastName())
            .email(teacher.getEmail())
            .phoneNumber(teacher.getPhoneNumber())
            .roles(roles)
            .build();
    }

    private TeacherScope resolveScope(UUID teacherId, UUID subjectId, UUID classId) {
        List<ClassSubject> classSubjects;
        if (subjectId != null && classId != null) {
            classSubjects = classSubjectRepository.findByTeacher_IdAndClassEntity_IdAndSubject_IdAndDeletedAtIsNull(
                teacherId,
                classId,
                subjectId
            );
        } else if (subjectId != null) {
            classSubjects = classSubjectRepository.findBySubject_IdAndTeacher_IdAndDeletedAtIsNull(subjectId, teacherId);
        } else if (classId != null) {
            classSubjects = classSubjectRepository.findByTeacher_IdAndClassEntity_IdAndDeletedAtIsNull(teacherId, classId);
        } else {
            classSubjects = classSubjectRepository.findByTeacher_IdAndDeletedAtIsNull(teacherId);
        }

        List<ClassSubject> filtered = classSubjects.stream()
            .filter(link -> link.getDeletedAt() == null)
            .filter(link -> link.getClassEntity() != null && link.getClassEntity().getDeletedAt() == null)
            .filter(link -> link.getSubject() != null && link.getSubject().getDeletedAt() == null)
            .toList();

        Set<UUID> classIds = filtered.stream()
            .map(ClassSubject::getClassEntity)
            .filter(Objects::nonNull)
            .map(ClassEntity::getId)
            .collect(Collectors.toCollection(LinkedHashSet::new));

        Set<UUID> classSubjectIds = filtered.stream()
            .map(ClassSubject::getId)
            .collect(Collectors.toCollection(LinkedHashSet::new));

        return new TeacherScope(filtered, classIds, classSubjectIds);
    }

    private User requireTeacher(UUID teacherId) {
        if (teacherId == null) {
            throw new BadRequestException("teacherId is required");
        }
        User teacher = userRepository.findByIdAndDeletedAtIsNull(teacherId)
            .orElseThrow(() -> new NotFoundException("Teacher not found: " + teacherId));
        if (teacher.getRoles().stream().noneMatch(role -> "teacher".equalsIgnoreCase(role.getCode()))) {
            throw new NotFoundException("Teacher not found: " + teacherId);
        }
        return teacher;
    }

    private boolean isStudent(User user) {
        return user != null
            && user.getRoles().stream().anyMatch(role -> "student".equalsIgnoreCase(role.getCode()));
    }

    private List<UUID> extractClassSubjectIds(List<ClassSubject> classSubjects) {
        return classSubjects.stream().map(ClassSubject::getId).toList();
    }

    private boolean isPass(AssessmentResult result) {
        if (result == null) {
            return false;
        }
        Double actual = result.getActualMark();
        Double expected = result.getExpectedMark();
        if (actual != null && expected != null && expected > 0) {
            return actual >= expected * 0.5;
        }
        String grade = result.getGrade();
        if (grade == null || grade.isBlank()) {
            return false;
        }
        String normalized = grade.toLowerCase(Locale.ROOT);
        return !(normalized.contains("fail") || normalized.equals("f"));
    }

    private String resolveStudentStatus(String enrollmentStatus, AssessmentResult latestResult) {
        if (latestResult != null) {
            if (latestResult.getStatus() != null && !latestResult.getStatus().isBlank()) {
                return latestResult.getStatus();
            }
            if (latestResult.getActualMark() != null || latestResult.getGradedAt() != null) {
                return "reviewed";
            }
            return "submitted";
        }
        if (enrollmentStatus != null && !enrollmentStatus.isBlank()) {
            return enrollmentStatus;
        }
        return "not_assigned";
    }

    private String resolvePerformance(StudentProfile profile) {
        if (profile != null && profile.getPerformance() != null && !profile.getPerformance().isBlank()) {
            return profile.getPerformance();
        }
        if (profile == null || profile.getOverall() == null) {
            return "Unrated";
        }
        double overall = profile.getOverall();
        if (overall >= 75) {
            return "Excellent";
        }
        if (overall >= 65) {
            return "Good";
        }
        if (overall >= 55) {
            return "Average";
        }
        if (overall > 0) {
            return "Needs improvement";
        }
        return "Unrated";
    }

    private String normalizePerformanceBucket(StudentProfile profile, String performanceValue) {
        if (profile != null && profile.getOverall() != null) {
            double overall = profile.getOverall();
            if (overall >= 75) {
                return "excellent";
            }
            if (overall >= 65) {
                return "good";
            }
            if (overall >= 55) {
                return "average";
            }
            if (overall > 0) {
                return "needs-improvement";
            }
        }

        String normalized = normalizeFilter(performanceValue);
        if (normalized.contains("excellent")) {
            return "excellent";
        }
        if (normalized.contains("good")) {
            return "good";
        }
        if (normalized.contains("average")) {
            return "average";
        }
        if (normalized.contains("need")) {
            return "needs-improvement";
        }
        return "unknown";
    }

    private StudentPlan selectPrimaryPlan(Collection<StudentPlan> plans) {
        if (plans == null || plans.isEmpty()) {
            return null;
        }
        return plans.stream()
            .filter(plan -> plan.getDeletedAt() == null)
            .sorted(Comparator
                .comparing(StudentPlan::isCurrent, Comparator.reverseOrder())
                .thenComparing(StudentPlan::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(StudentPlan::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
            .findFirst()
            .orElse(null);
    }

    private boolean isMasteryRisk(StudentProfile profile) {
        if (profile == null) {
            return false;
        }
        if (profile.getOverall() != null && profile.getOverall() < 50) {
            return true;
        }
        String performance = profile.getPerformance();
        return performance != null && performance.toLowerCase(Locale.ROOT).contains("need");
    }

    private String normalizeSearch(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return normalized.isBlank() ? null : normalized;
    }

    private String normalizeFilter(String value) {
        if (value == null) {
            return "";
        }
        return value.trim()
            .toLowerCase(Locale.ROOT)
            .replace('_', '-')
            .replace(' ', '-');
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isBlank() ? null : normalized;
    }

    private Instant parseOptionalInstant(String value, boolean endOfDay) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();

        try {
            return Instant.parse(trimmed);
        } catch (DateTimeParseException ignored) {
            // Try date-only input next.
        }

        try {
            LocalDate localDate = LocalDate.parse(trimmed);
            if (endOfDay) {
                return localDate.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC).minusNanos(1);
            }
            return localDate.atStartOfDay().toInstant(ZoneOffset.UTC);
        } catch (DateTimeParseException ex) {
            throw new BadRequestException("Invalid date format: " + value + ". Use ISO-8601 date or datetime.");
        }
    }

    private double roundTwo(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private PageResponse<TeacherStudentSummaryDto> emptyPage(int page, int size) {
        return PageResponse.<TeacherStudentSummaryDto>builder()
            .items(List.of())
            .page(page)
            .size(size)
            .totalItems(0)
            .totalPages(0)
            .build();
    }

    private record TeacherScope(List<ClassSubject> classSubjects, Set<UUID> classIds, Set<UUID> classSubjectIds) {
        private List<UUID> classIdsOrSentinel() {
            if (classIds == null || classIds.isEmpty()) {
                return List.of(EMPTY_UUID);
            }
            return new ArrayList<>(classIds);
        }
    }
}
