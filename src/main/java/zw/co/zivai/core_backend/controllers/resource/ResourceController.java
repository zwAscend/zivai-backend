package zw.co.zivai.core_backend.controllers.resource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import zw.co.zivai.core_backend.dtos.resources.CreateResourceRequest;
import zw.co.zivai.core_backend.dtos.resources.ResourceCountsDto;
import zw.co.zivai.core_backend.dtos.resources.ResourceDto;
import zw.co.zivai.core_backend.dtos.resources.ResourceRecentDto;
import zw.co.zivai.core_backend.dtos.resources.UpdateResourceRequest;
import zw.co.zivai.core_backend.services.resource.ResourceService;

@RestController
@RequestMapping("/api/resources")
@RequiredArgsConstructor
public class ResourceController {
    private final ResourceService resourceService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public zw.co.zivai.core_backend.models.lms.Resource create(@RequestBody CreateResourceRequest request) {
        return resourceService.create(request);
    }

    @PostMapping("/upload")
    public Map<String, Object> upload(@RequestParam("file") MultipartFile file,
                                      @RequestParam(value = "subjectId", required = false) String subjectId) {
        ResourceDto resource = resourceService.upload(file, subjectId);
        return Map.of("resource", resource);
    }

    @GetMapping
    public List<ResourceDto> list(@RequestParam(value = "subjectId", required = false) UUID subjectId,
                                  @RequestParam(value = "status", required = false) String status) {
        return resourceService.list(subjectId, status);
    }

    @GetMapping("/counts")
    public Map<String, ResourceCountsDto> counts() {
        return resourceService.getCounts();
    }

    @GetMapping("/recent")
    public List<ResourceRecentDto> recent(@RequestParam(value = "limit", required = false, defaultValue = "5") int limit) {
        return resourceService.recent(limit);
    }

    @GetMapping("/subject/{subjectId}")
    public List<ResourceDto> bySubject(@PathVariable UUID subjectId) {
        return resourceService.listBySubject(subjectId);
    }

    @GetMapping("/topic/{topicId}")
    public List<ResourceDto> byTopic(@PathVariable UUID topicId) {
        return resourceService.listByTopic(topicId);
    }

    @GetMapping("/{id}")
    public ResourceDto get(@PathVariable UUID id) {
        return resourceService.getContent(id);
    }

    @PutMapping("/{id}")
    public ResourceDto update(@PathVariable UUID id, @RequestBody UpdateResourceRequest request) {
        return resourceService.update(id, request);
    }

    @GetMapping("/content/{id}")
    public ResourceDto content(@PathVariable UUID id) {
        return resourceService.getContent(id);
    }

    @GetMapping("/download/{id}")
    public Map<String, String> download(@PathVariable UUID id) {
        return resourceService.getDownloadLink(id);
    }

    @GetMapping("/file/{id}")
    public ResponseEntity<Resource> file(@PathVariable UUID id) throws java.io.IOException {
        Path path = resourceService.getFilePath(id);
        String contentType = Files.probeContentType(path);
        FileSystemResource resource = new FileSystemResource(path);
        return ResponseEntity.ok()
            .contentType(contentType != null ? MediaType.parseMediaType(contentType) : MediaType.APPLICATION_OCTET_STREAM)
            .body(resource);
    }
}
