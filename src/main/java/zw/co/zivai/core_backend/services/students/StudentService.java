package zw.co.zivai.core_backend.services.students;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
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
        return userRepository.findByRoles_CodeAndDeletedAtIsNull("student").stream()
            .map(this::toStudentDto)
            .collect(Collectors.toList());
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
        return toStudentDto(user);
    }

    private StudentDto toStudentDto(User user) {
        List<String> subjectIds = resolveSubjectIds(user.getId());
        StudentProfile profile = studentProfileRepository.findByUserIdAndDeletedAtIsNull(user.getId()).orElse(null);
        List<StudentAttribute> attributes = studentAttributeRepository.findByStudent_Id(user.getId());

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

        return StudentDto.builder()
            .id(user.getId().toString())
            .firstName(user.getFirstName())
            .lastName(user.getLastName())
            .email(user.getEmail())
            .overall(overall)
            .strength(strength)
            .performance(performance)
            .engagement(engagement)
            .subjects(subjectIds)
            .build();
    }

    private List<StudentDto> listByClassSubject(UUID classSubjectId) {
        List<StudentSubjectEnrolment> enrolments =
            studentSubjectEnrolmentRepository.findByClassSubject_IdAndDeletedAtIsNull(classSubjectId);
        if (enrolments.isEmpty()) {
            return List.of();
        }
        return enrolments.stream()
            .map(StudentSubjectEnrolment::getStudent)
            .filter(student -> student != null && student.getDeletedAt() == null)
            .distinct()
            .map(this::toStudentDto)
            .toList();
    }

    private List<StudentDto> listByClass(UUID classId) {
        List<Enrolment> enrolments = enrolmentRepository.findByClassEntity_Id(classId);
        if (enrolments.isEmpty()) {
            return List.of();
        }
        return enrolments.stream()
            .map(Enrolment::getStudent)
            .filter(student -> student != null && student.getDeletedAt() == null)
            .distinct()
            .map(this::toStudentDto)
            .toList();
    }

    private List<StudentDto> listBySubject(UUID subjectId) {
        List<StudentSubjectEnrolment> enrolments =
            studentSubjectEnrolmentRepository.findByClassSubject_Subject_IdAndDeletedAtIsNull(subjectId);
        if (!enrolments.isEmpty()) {
            return enrolments.stream()
                .map(StudentSubjectEnrolment::getStudent)
                .filter(student -> student != null && student.getDeletedAt() == null)
                .distinct()
                .map(this::toStudentDto)
                .toList();
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
        return classIds.stream()
            .flatMap(id -> enrolmentRepository.findByClassEntity_Id(id).stream())
            .map(Enrolment::getStudent)
            .filter(student -> student != null && student.getDeletedAt() == null)
            .distinct()
            .map(this::toStudentDto)
            .toList();
    }

    private List<String> resolveSubjectIds(UUID studentId) {
        List<StudentSubjectEnrolment> directEnrolments =
            studentSubjectEnrolmentRepository.findByStudent_IdAndDeletedAtIsNull(studentId);
        if (!directEnrolments.isEmpty()) {
            return directEnrolments.stream()
                .map(StudentSubjectEnrolment::getClassSubject)
                .filter(classSubject -> classSubject != null && classSubject.getSubject() != null)
                .map(classSubject -> classSubject.getSubject().getId().toString())
                .distinct()
                .toList();
        }

        List<Enrolment> enrolments = enrolmentRepository.findByStudent_Id(studentId);
        if (enrolments.isEmpty()) {
            return fallbackSubjects();
        }

        Set<String> subjectIds = new HashSet<>();
        for (Enrolment enrolment : enrolments) {
            if (enrolment.getClassEntity() == null) {
                continue;
            }
            List<ClassSubject> classSubjects =
                classSubjectRepository.findByClassEntity_IdAndDeletedAtIsNull(enrolment.getClassEntity().getId());
            for (ClassSubject classSubject : classSubjects) {
                if (classSubject.getSubject() != null) {
                    subjectIds.add(classSubject.getSubject().getId().toString());
                }
            }
        }
        List<String> resolved = subjectIds.stream().toList();
        return resolved.isEmpty() ? fallbackSubjects() : resolved;
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
