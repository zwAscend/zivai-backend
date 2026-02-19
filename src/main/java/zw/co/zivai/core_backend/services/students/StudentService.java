package zw.co.zivai.core_backend.services.students;

import java.time.Duration;
import java.time.Instant;
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
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import zw.co.zivai.core_backend.dtos.students.StudentDto;
import zw.co.zivai.core_backend.dtos.students.StudentTeacherDto;
import zw.co.zivai.core_backend.exceptions.NotFoundException;
import zw.co.zivai.core_backend.models.lms.ClassSubject;
import zw.co.zivai.core_backend.models.lms.ClassEntity;
import zw.co.zivai.core_backend.models.lms.Enrolment;
import zw.co.zivai.core_backend.models.lms.StudentAttribute;
import zw.co.zivai.core_backend.models.lms.StudentProfile;
import zw.co.zivai.core_backend.models.lms.StudentSubjectEnrolment;
import zw.co.zivai.core_backend.models.lms.User;
import zw.co.zivai.core_backend.repositories.classroom.ClassSubjectRepository;
import zw.co.zivai.core_backend.repositories.classroom.EnrolmentRepository;
import zw.co.zivai.core_backend.repositories.subject.SubjectRepository;
import zw.co.zivai.core_backend.repositories.development.StudentAttributeRepository;
import zw.co.zivai.core_backend.repositories.students.StudentProfileRepository;
import zw.co.zivai.core_backend.repositories.classroom.StudentSubjectEnrolmentRepository;
import zw.co.zivai.core_backend.repositories.user.UserRepository;

@Service
@RequiredArgsConstructor
public class StudentService {
    private final UserRepository userRepository;
    private final SubjectRepository subjectRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final StudentAttributeRepository studentAttributeRepository;
    private final StudentSubjectEnrolmentRepository studentSubjectEnrolmentRepository;
    private final EnrolmentRepository enrolmentRepository;
    private final ClassSubjectRepository classSubjectRepository;

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
