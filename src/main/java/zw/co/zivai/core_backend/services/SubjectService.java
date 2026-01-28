package zw.co.zivai.core_backend.services;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import zw.co.zivai.core_backend.dtos.CreateSubjectRequest;
import zw.co.zivai.core_backend.exceptions.NotFoundException;
import zw.co.zivai.core_backend.models.lms.Subject;
import zw.co.zivai.core_backend.repositories.SubjectRepository;

@Service
@RequiredArgsConstructor
public class SubjectService {
    private final SubjectRepository subjectRepository;

    public Subject create(CreateSubjectRequest request) {
        Subject subject = new Subject();
        subject.setCode(request.getCode());
        subject.setName(request.getName());
        subject.setExamBoardCode(request.getExamBoardCode());
        subject.setDescription(request.getDescription());
        subject.setActive(request.isActive());
        return subjectRepository.save(subject);
    }

    public List<Subject> list() {
        return subjectRepository.findAll();
    }

    public Subject get(UUID id) {
        return subjectRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Subject not found: " + id));
    }
}
