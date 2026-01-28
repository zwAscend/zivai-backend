package zw.co.zivai.core_backend.controllers;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/development")
@RequiredArgsConstructor
public class DevelopmentController {

    @GetMapping("/attributes/subject/{subjectId}")
    public List<Map<String, Object>> getSubjectAttributes(@PathVariable UUID subjectId) {
        return Collections.emptyList();
    }

    @GetMapping("/attributes/student/{studentId}/subject/{subjectId}")
    public Map<String, Object> getStudentAttributes(@PathVariable UUID studentId, @PathVariable UUID subjectId) {
        return Collections.emptyMap();
    }

    @GetMapping("/plans/subject/{subjectId}")
    public List<Map<String, Object>> getSubjectPlans(@PathVariable UUID subjectId) {
        return Collections.emptyList();
    }

    @GetMapping("/plans/student/{studentId}")
    public List<Map<String, Object>> getStudentPlans(@PathVariable UUID studentId) {
        return Collections.emptyList();
    }

    @GetMapping("/plans/student/{studentId}/subject/{subjectId}")
    public Map<String, Object> getStudentPlan(@PathVariable UUID studentId, @PathVariable UUID subjectId) {
        return Collections.emptyMap();
    }
}
