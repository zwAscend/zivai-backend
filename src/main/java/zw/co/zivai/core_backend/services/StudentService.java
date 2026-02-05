package zw.co.zivai.core_backend.services;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import zw.co.zivai.core_backend.dtos.StudentDto;
import zw.co.zivai.core_backend.exceptions.NotFoundException;
import zw.co.zivai.core_backend.models.lms.Subject;
import zw.co.zivai.core_backend.models.lms.User;
import zw.co.zivai.core_backend.repositories.SubjectRepository;
import zw.co.zivai.core_backend.repositories.UserRepository;

@Service
@RequiredArgsConstructor
public class StudentService {
    private final UserRepository userRepository;
    private final SubjectRepository subjectRepository;

    public List<StudentDto> list() {
        List<String> subjectIds = subjectRepository.findAllByDeletedAtIsNull().stream()
            .map(Subject::getId)
            .map(UUID::toString)
            .toList();

        return userRepository.findByRoles_CodeAndDeletedAtIsNull("student").stream()
            .map(user -> toStudentDto(user, subjectIds))
            .collect(Collectors.toList());
    }

    public StudentDto get(UUID id) {
        User user = userRepository.findByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> new NotFoundException("Student not found: " + id));

        List<String> subjectIds = subjectRepository.findAllByDeletedAtIsNull().stream()
            .map(Subject::getId)
            .map(UUID::toString)
            .toList();

        return toStudentDto(user, subjectIds);
    }

    private StudentDto toStudentDto(User user, List<String> subjectIds) {
        String id = user.getId().toString();
        return StudentDto.builder()
            .id(id)
            .firstName(user.getFirstName())
            .lastName(user.getLastName())
            .email(user.getEmail())
            .overall(72.5)
            .strength("Problem Solving")
            .performance("Good")
            .engagement("High")
            .subjects(subjectIds)
            .build();
    }
}
