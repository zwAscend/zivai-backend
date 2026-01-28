package zw.co.zivai.core_backend.controllers;

import java.util.List;
import java.util.stream.Collectors;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import zw.co.zivai.core_backend.dtos.CreateSubjectRequest;
import zw.co.zivai.core_backend.dtos.SubjectDto;
import zw.co.zivai.core_backend.models.lms.Subject;
import zw.co.zivai.core_backend.services.SubjectService;

@RestController
@RequestMapping("/api/subjects")
@RequiredArgsConstructor
public class SubjectController {
    private final SubjectService subjectService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Subject create(@RequestBody CreateSubjectRequest request) {
        return subjectService.create(request);
    }

    @GetMapping("/teaching")
    public List<SubjectDto> teaching() {
        return subjectService.list().stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }

    @GetMapping
    public List<SubjectDto> list() {
        return subjectService.list().stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public SubjectDto get(@PathVariable UUID id) {
        return toDto(subjectService.get(id));
    }


    private SubjectDto toDto(Subject subject) {
        String id = subject.getId().toString();
        return SubjectDto.builder()
            ._id(id)
            .code(subject.getCode())
            .name(subject.getName())
            .description(subject.getDescription())
            .teacher(null)
            .build();
    }
}
