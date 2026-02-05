package zw.co.zivai.core_backend.services;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import zw.co.zivai.core_backend.dtos.CreateClassRequest;
import zw.co.zivai.core_backend.dtos.UpdateClassRequest;
import zw.co.zivai.core_backend.exceptions.BadRequestException;
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
        if (request.getSchoolId() == null) {
            throw new BadRequestException("School id is required");
        }
        if (request.getCode() == null || request.getCode().isBlank()) {
            throw new BadRequestException("Class code is required");
        }
        if (request.getName() == null || request.getName().isBlank()) {
            throw new BadRequestException("Class name is required");
        }

        School school = schoolRepository.findByIdAndDeletedAtIsNull(request.getSchoolId())
            .orElseThrow(() -> new NotFoundException("School not found: " + request.getSchoolId()));

        ClassEntity classEntity = new ClassEntity();
        classEntity.setSchool(school);
        classEntity.setCode(request.getCode().trim().toUpperCase());
        classEntity.setName(request.getName().trim());
        classEntity.setGradeLevel(request.getGradeLevel() == null ? null : request.getGradeLevel().trim());
        classEntity.setAcademicYear(request.getAcademicYear() == null ? null : request.getAcademicYear().trim());

        if (request.getHomeroomTeacherId() != null) {
            User teacher = userRepository.findByIdAndDeletedAtIsNull(request.getHomeroomTeacherId())
                .orElseThrow(() -> new NotFoundException("Teacher not found: " + request.getHomeroomTeacherId()));
            classEntity.setHomeroomTeacher(teacher);
        }

        return classRepository.save(classEntity);
    }

    public List<ClassEntity> list() {
        return classRepository.findAllByDeletedAtIsNull();
    }

    public ClassEntity get(UUID id) {
        return classRepository.findByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> new NotFoundException("Class not found: " + id));
    }

    public ClassEntity update(UUID id, UpdateClassRequest request) {
        ClassEntity classEntity = get(id);

        if (request.getSchoolId() != null) {
            School school = schoolRepository.findByIdAndDeletedAtIsNull(request.getSchoolId())
                .orElseThrow(() -> new NotFoundException("School not found: " + request.getSchoolId()));
            classEntity.setSchool(school);
        }
        if (request.getCode() != null && !request.getCode().isBlank()) {
            classEntity.setCode(request.getCode().trim().toUpperCase());
        }
        if (request.getName() != null && !request.getName().isBlank()) {
            classEntity.setName(request.getName().trim());
        }
        if (request.getGradeLevel() != null) {
            String gradeLevel = request.getGradeLevel().trim();
            classEntity.setGradeLevel(gradeLevel.isEmpty() ? null : gradeLevel);
        }
        if (request.getAcademicYear() != null) {
            String academicYear = request.getAcademicYear().trim();
            classEntity.setAcademicYear(academicYear.isEmpty() ? null : academicYear);
        }

        if (Boolean.TRUE.equals(request.getClearHomeroomTeacher())) {
            classEntity.setHomeroomTeacher(null);
        } else if (request.getHomeroomTeacherId() != null) {
            User teacher = userRepository.findByIdAndDeletedAtIsNull(request.getHomeroomTeacherId())
                .orElseThrow(() -> new NotFoundException("Teacher not found: " + request.getHomeroomTeacherId()));
            classEntity.setHomeroomTeacher(teacher);
        }

        return classRepository.save(classEntity);
    }

    public void delete(UUID id) {
        ClassEntity classEntity = get(id);
        classEntity.setDeletedAt(Instant.now());
        classRepository.save(classEntity);
    }
}
