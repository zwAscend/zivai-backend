package zw.co.zivai.core_backend.controllers;

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
import zw.co.zivai.core_backend.dtos.CreateEnrolmentRequest;
import zw.co.zivai.core_backend.models.lms.Enrolment;
import zw.co.zivai.core_backend.services.EnrolmentService;

@RestController
@RequestMapping("/api/enrolments")
@RequiredArgsConstructor
public class EnrolmentController {
    private final EnrolmentService enrolmentService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Enrolment create(@RequestBody CreateEnrolmentRequest request) {
        return enrolmentService.create(request);
    }

    @GetMapping
    public List<Enrolment> list() {
        return enrolmentService.list();
    }

    @GetMapping("/{id}")
    public Enrolment get(@PathVariable UUID id) {
        return enrolmentService.get(id);
    }
}
