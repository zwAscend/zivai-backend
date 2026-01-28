package zw.co.zivai.core_backend.controllers;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import zw.co.zivai.core_backend.dtos.SubjectLegacyDto;
import zw.co.zivai.core_backend.services.SubjectLegacyService;

@RestController
@RequestMapping("/api/subjects-legacy")
@RequiredArgsConstructor
public class SubjectLegacyController {
    private final SubjectLegacyService subjectLegacyService;

    @GetMapping
    public List<SubjectLegacyDto> list() {
        return subjectLegacyService.list();
    }

    @GetMapping("/teaching")
    public List<SubjectLegacyDto> teaching() {
        return subjectLegacyService.list();
    }

    @GetMapping("/{id}")
    public SubjectLegacyDto get(@PathVariable UUID id) {
        return subjectLegacyService.get(id);
    }
}
