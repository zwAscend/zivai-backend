package zw.co.zivai.core_backend.services.students;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import zw.co.zivai.core_backend.dtos.students.StudentDto;
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
}
