package zw.co.zivai.core_backend.services.classroom;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import zw.co.zivai.core_backend.dtos.classroom.CreateEnrolmentRequest;
import zw.co.zivai.core_backend.exceptions.NotFoundException;
import zw.co.zivai.core_backend.models.lms.ClassEntity;
import zw.co.zivai.core_backend.models.lms.Enrolment;
import zw.co.zivai.core_backend.models.lms.User;
import zw.co.zivai.core_backend.repositories.classroom.ClassRepository;
import zw.co.zivai.core_backend.repositories.classroom.EnrolmentRepository;
import zw.co.zivai.core_backend.repositories.user.UserRepository;

@Service
@RequiredArgsConstructor
public class EnrolmentService {
    private final EnrolmentRepository enrolmentRepository;
    private final ClassRepository classRepository;
    private final UserRepository userRepository;

    public Enrolment create(CreateEnrolmentRequest request) {
        ClassEntity classEntity = classRepository.findById(request.getClassId())
            .orElseThrow(() -> new NotFoundException("Class not found: " + request.getClassId()));
        User student = userRepository.findById(request.getStudentId())
            .orElseThrow(() -> new NotFoundException("Student not found: " + request.getStudentId()));

        Enrolment enrolment = new Enrolment();
        enrolment.setClassEntity(classEntity);
        enrolment.setStudent(student);
        enrolment.setEnrolmentStatusCode(request.getEnrolmentStatusCode());
        return enrolmentRepository.save(enrolment);
    }

    public List<Enrolment> list() {
        return enrolmentRepository.findAll();
    }

    public Enrolment get(UUID id) {
        return enrolmentRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Enrolment not found: " + id));
    }
}
