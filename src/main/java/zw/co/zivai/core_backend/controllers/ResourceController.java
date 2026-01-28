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
import zw.co.zivai.core_backend.dtos.CreateResourceRequest;
import zw.co.zivai.core_backend.models.lms.Resource;
import zw.co.zivai.core_backend.services.ResourceService;

@RestController
@RequestMapping("/api/resources")
@RequiredArgsConstructor
public class ResourceController {
    private final ResourceService resourceService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Resource create(@RequestBody CreateResourceRequest request) {
        return resourceService.create(request);
    }

    @GetMapping
    public List<Resource> list() {
        return resourceService.list();
    }

    @GetMapping("/{id}")
    public Resource get(@PathVariable UUID id) {
        return resourceService.get(id);
    }
}
