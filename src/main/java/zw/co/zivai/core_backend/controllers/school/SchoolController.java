package zw.co.zivai.core_backend.controllers.school;

import java.util.List;
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
import zw.co.zivai.core_backend.dtos.schools.CreateSchoolRequest;
import zw.co.zivai.core_backend.models.lms.School;
import zw.co.zivai.core_backend.services.school.SchoolService;

@RestController
@RequestMapping("/api/schools")
@RequiredArgsConstructor
public class SchoolController {
    private final SchoolService schoolService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public School create(@RequestBody CreateSchoolRequest request) {
        return schoolService.create(request);
    }

    @GetMapping
    public List<School> list() {
        return schoolService.list();
    }

    @GetMapping("/{id}")
    public School get(@PathVariable UUID id) {
        return schoolService.get(id);
    }
}
