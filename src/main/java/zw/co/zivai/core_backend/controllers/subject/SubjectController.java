package zw.co.zivai.core_backend.controllers.subject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.stream.Collectors;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import com.fasterxml.jackson.databind.JsonNode;
import zw.co.zivai.core_backend.dtos.subjects.CreateSubjectRequest;
import zw.co.zivai.core_backend.dtos.subjects.SubjectDto;
import zw.co.zivai.core_backend.dtos.subjects.UpdateSubjectRequest;
import zw.co.zivai.core_backend.models.lms.ClassSubject;
import zw.co.zivai.core_backend.models.lms.Subject;
import zw.co.zivai.core_backend.repositories.classroom.ClassSubjectRepository;
import zw.co.zivai.core_backend.services.subject.SubjectService;

@RestController
@RequestMapping("/api/subjects")
@RequiredArgsConstructor
public class SubjectController {
    private final SubjectService subjectService;
    private final ClassSubjectRepository classSubjectRepository;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Subject create(@RequestBody CreateSubjectRequest request) {
        return subjectService.create(request);
    }

    @GetMapping("/teaching")
    public List<SubjectDto> teaching() {
        Map<UUID, List<ClassSubject>> bySubject = classSubjectRepository.findAllByDeletedAtIsNull().stream()
            .collect(Collectors.groupingBy(cs -> cs.getSubject().getId()));
        return subjectService.list().stream()
            .map(subject -> toDto(subject, bySubject.getOrDefault(subject.getId(), Collections.emptyList())))
            .collect(Collectors.toList());
    }

    @GetMapping
    public List<SubjectDto> list() {
        Map<UUID, List<ClassSubject>> bySubject = classSubjectRepository.findAllByDeletedAtIsNull().stream()
            .collect(Collectors.groupingBy(cs -> cs.getSubject().getId()));
        return subjectService.list().stream()
            .map(subject -> toDto(subject, bySubject.getOrDefault(subject.getId(), Collections.emptyList())))
            .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public SubjectDto get(@PathVariable UUID id) {
        Subject subject = subjectService.get(id);
        List<ClassSubject> links = classSubjectRepository.findBySubject_IdAndDeletedAtIsNull(id);
        return toDto(subject, links);
    }

    @PutMapping("/{id}")
    public SubjectDto update(@PathVariable UUID id, @RequestBody UpdateSubjectRequest request) {
        Subject updated = subjectService.update(id, request);
        List<ClassSubject> links = classSubjectRepository.findBySubject_IdAndDeletedAtIsNull(id);
        return toDto(updated, links);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        subjectService.delete(id);
    }

    private SubjectDto toDto(Subject subject, List<ClassSubject> links) {
        String id = subject.getId().toString();
        LinkedHashSet<String> gradeSet = new LinkedHashSet<>();
        links.stream()
            .map(link -> link.getClassEntity() != null ? link.getClassEntity().getGradeLevel() : null)
            .filter(value -> value != null && !value.isBlank())
            .forEach(gradeSet::add);

        JsonNode subjectAttributes = subject.getSubjectAttributes();
        if (subjectAttributes != null && subjectAttributes.has("grades") && subjectAttributes.get("grades").isArray()) {
            subjectAttributes.get("grades").forEach(node -> {
                String value = node.asText(null);
                if (value != null && !value.isBlank()) {
                    gradeSet.add(value.trim());
                }
            });
        }

        List<String> grades = new ArrayList<>(gradeSet);
        List<String> teachers = links.stream()
            .map(link -> link.getTeacher())
            .filter(teacher -> teacher != null)
            .map(teacher -> {
                String fullName = (teacher.getFirstName() + " " + teacher.getLastName()).trim();
                return fullName.isBlank() ? teacher.getEmail() : fullName;
            })
            .filter(value -> value != null && !value.isBlank())
            .collect(Collectors.collectingAndThen(Collectors.toCollection(LinkedHashSet::new), ArrayList::new));

        return SubjectDto.builder()
            .id(id)
            .code(subject.getCode())
            .name(subject.getName())
            .examBoardCode(subject.getExamBoardCode())
            .description(subject.getDescription())
            .active(subject.isActive())
            .grades(grades)
            .teachers(teachers)
            .build();
    }
}
