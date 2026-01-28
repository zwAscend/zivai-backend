package zw.co.zivai.core_backend.services;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import zw.co.zivai.core_backend.dtos.CreateClassRequest;
import zw.co.zivai.core_backend.exceptions.NotFoundException;
import zw.co.zivai.core_backend.models.lms.ClassEntity;
import zw.co.zivai.core_backend.models.lms.School;
import zw.co.zivai.core_backend.models.lms.User;
import zw.co.zivai.core_backend.repositories.ClassRepository;
import zw.co.zivai.core_backend.repositories.SchoolRepository;
import zw.co.zivai.core_backend.repositories.UserRepository;

@Service
@RequiredArgsConstructor
public class ClassService {
    private final ClassRepository classRepository;
    private final SchoolRepository schoolRepository;
    private final UserRepository userRepository;

    public ClassEntity create(CreateClassRequest request) {
        School school = schoolRepository.findById(request.getSchoolId())
            .orElseThrow(() -> new NotFoundException("School not found: " + request.getSchoolId()));

        ClassEntity classEntity = new ClassEntity();
        classEntity.setSchool(school);
        classEntity.setCode(request.getCode());
        classEntity.setName(request.getName());
        classEntity.setGradeLevel(request.getGradeLevel());
        classEntity.setAcademicYear(request.getAcademicYear());

        if (request.getHomeroomTeacherId() != null) {
            User teacher = userRepository.findById(request.getHomeroomTeacherId())
                .orElseThrow(() -> new NotFoundException("Teacher not found: " + request.getHomeroomTeacherId()));
            classEntity.setHomeroomTeacher(teacher);
        }

        return classRepository.save(classEntity);
    }

    public List<ClassEntity> list() {
        return classRepository.findAll();
    }

    public ClassEntity get(UUID id) {
        return classRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Class not found: " + id));
    }
}
