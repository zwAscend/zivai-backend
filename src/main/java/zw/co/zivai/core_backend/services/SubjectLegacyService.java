package zw.co.zivai.core_backend.services;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import zw.co.zivai.core_backend.dtos.SubjectLegacyDto;
import zw.co.zivai.core_backend.exceptions.NotFoundException;
import zw.co.zivai.core_backend.models.lms.Subject;
import zw.co.zivai.core_backend.repositories.SubjectRepository;

@Service
@RequiredArgsConstructor
public class SubjectLegacyService {
    private final SubjectRepository subjectRepository;

    public List<SubjectLegacyDto> list() {
        return subjectRepository.findAll().stream()
            .map(this::toSubjectLegacyDto)
            .collect(Collectors.toList());
    }

    public SubjectLegacyDto get(UUID id) {
        Subject subject = subjectRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Subject not found: " + id));
        return toSubjectLegacyDto(subject);
    }

    private SubjectLegacyDto toSubjectLegacyDto(Subject subject) {
        String id = subject.getId().toString();
        return SubjectLegacyDto.builder()
            ._id(id)
            .code(subject.getCode())
            .name(subject.getName())
            .description(subject.getDescription())
            .teacher(null)
            .build();
    }
}
