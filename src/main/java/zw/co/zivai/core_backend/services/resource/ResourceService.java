package zw.co.zivai.core_backend.services.resource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import zw.co.zivai.core_backend.dtos.resources.CreateResourceRequest;
import zw.co.zivai.core_backend.dtos.resources.ResourceCountsDto;
import zw.co.zivai.core_backend.dtos.resources.ResourceDto;
import zw.co.zivai.core_backend.dtos.resources.ResourceRecentDto;
import zw.co.zivai.core_backend.dtos.resources.UpdateResourceRequest;
import zw.co.zivai.core_backend.exceptions.BadRequestException;
import zw.co.zivai.core_backend.exceptions.NotFoundException;
import zw.co.zivai.core_backend.models.lms.Resource;
import zw.co.zivai.core_backend.models.lms.School;
import zw.co.zivai.core_backend.models.lms.Subject;
import zw.co.zivai.core_backend.models.lms.User;
import zw.co.zivai.core_backend.repositories.resource.ResourceRepository;
import zw.co.zivai.core_backend.repositories.school.SchoolRepository;
import zw.co.zivai.core_backend.repositories.subject.SubjectRepository;
import zw.co.zivai.core_backend.repositories.user.UserRepository;

@Service
@RequiredArgsConstructor
public class ResourceService {
    private final ResourceRepository resourceRepository;
    private final SchoolRepository schoolRepository;
    private final SubjectRepository subjectRepository;
    private final UserRepository userRepository;

    private static final Path UPLOAD_ROOT = Path.of("core-backend", "uploads");

    public Resource create(CreateResourceRequest request) {
        School school = schoolRepository.findById(request.getSchoolId())
            .orElseThrow(() -> new NotFoundException("School not found: " + request.getSchoolId()));
        User uploader = userRepository.findById(request.getUploadedBy())
            .orElseThrow(() -> new NotFoundException("Uploader not found: " + request.getUploadedBy()));

        Subject subject = null;
        if (request.getSubjectId() != null) {
            subject = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> new NotFoundException("Subject not found: " + request.getSubjectId()));
        }

        boolean isContent = request.getContentBody() != null && !request.getContentBody().isBlank();

        if (request.getName() == null || request.getName().isBlank()) {
            throw new BadRequestException("Resource name is required");
        }

        Resource resource = new Resource();
        resource.setSchool(school);
        resource.setSubject(subject);
        resource.setUploadedBy(uploader);
        resource.setName(request.getName());
        resource.setOriginalName(request.getOriginalName() != null ? request.getOriginalName() : request.getName());
        resource.setMimeType(request.getMimeType() != null ? request.getMimeType() : (isContent ? "text/markdown" : "application/octet-stream"));
        resource.setResType(request.getResType() != null ? request.getResType() : "document");
        resource.setSizeBytes(request.getSizeBytes() != null
            ? request.getSizeBytes()
            : (isContent ? (long) request.getContentBody().getBytes(StandardCharsets.UTF_8).length : 0L));
        if (request.getUrl() == null || request.getUrl().isBlank()) {
            if (isContent) {
                resource.setUrl("content://pending");
            } else {
                throw new BadRequestException("Resource url is required");
            }
        } else {
            resource.setUrl(request.getUrl());
        }
        resource.setStorageKey(request.getStorageKey());
        resource.setStoragePath(request.getStoragePath());
        if (request.getTags() != null) {
            resource.setTags(request.getTags().toArray(new String[0]));
        }
        resource.setContentType(request.getContentType());
        resource.setContentBody(request.getContentBody());
        resource.setPublishAt(request.getPublishAt());
        resource.setDisplayOrder(request.getDisplayOrder());
        resource.setStatus(request.getStatus());

        Resource saved = resourceRepository.save(resource);
        if (isContent && "content://pending".equals(saved.getUrl())) {
            saved.setUrl("/api/resources/content/" + saved.getId());
            saved = resourceRepository.save(saved);
        }
        return saved;
    }

    public List<ResourceDto> list(UUID subjectId, String status) {
        List<Resource> resources;
        if (subjectId != null && status != null && !status.isBlank()) {
            resources = resourceRepository.findBySubject_IdAndStatus(subjectId, status);
        } else if (subjectId != null) {
            resources = resourceRepository.findBySubject_Id(subjectId);
        } else if (status != null && !status.isBlank()) {
            resources = resourceRepository.findByStatus(status);
        } else {
            resources = resourceRepository.findAll();
        }

        return resources.stream()
            .map(resource -> toDto(resource, false))
            .toList();
    }

    public Resource get(UUID id) {
        return resourceRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Resource not found: " + id));
    }

    public List<ResourceDto> listBySubject(UUID subjectId) {
        return resourceRepository.findBySubject_Id(subjectId).stream()
            .map(resource -> toDto(resource, false))
            .toList();
    }

    public ResourceDto getContent(UUID id) {
        Resource resource = get(id);
        return toDto(resource, true);
    }

    public ResourceDto update(UUID id, UpdateResourceRequest request) {
        Resource resource = get(id);

        if (request.getSubjectId() != null) {
            Subject subject = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> new NotFoundException("Subject not found: " + request.getSubjectId()));
            resource.setSubject(subject);
        }
        if (request.getName() != null) {
            resource.setName(request.getName());
        }
        if (request.getOriginalName() != null) {
            resource.setOriginalName(request.getOriginalName());
        }
        if (request.getMimeType() != null) {
            resource.setMimeType(request.getMimeType());
        }
        if (request.getResType() != null) {
            resource.setResType(request.getResType());
        }
        if (request.getSizeBytes() != null) {
            resource.setSizeBytes(request.getSizeBytes());
        }
        if (request.getUrl() != null) {
            resource.setUrl(request.getUrl());
        }
        if (request.getStorageKey() != null) {
            resource.setStorageKey(request.getStorageKey());
        }
        if (request.getStoragePath() != null) {
            resource.setStoragePath(request.getStoragePath());
        }
        if (request.getTags() != null) {
            resource.setTags(request.getTags().toArray(new String[0]));
        }
        if (request.getContentType() != null) {
            resource.setContentType(request.getContentType());
        }
        if (request.getContentBody() != null) {
            resource.setContentBody(request.getContentBody());
            resource.setSizeBytes((long) request.getContentBody().getBytes(StandardCharsets.UTF_8).length);
        }
        if (request.getPublishAt() != null) {
            resource.setPublishAt(request.getPublishAt());
        }
        if (request.getDisplayOrder() != null) {
            resource.setDisplayOrder(request.getDisplayOrder());
        }
        if (request.getStatus() != null) {
            resource.setStatus(request.getStatus());
        }

        Resource saved = resourceRepository.save(resource);
        return toDto(saved, true);
    }

    public Map<String, ResourceCountsDto> getCounts() {
        List<Resource> resources = resourceRepository.findAll();
        Map<String, ResourceCountsDto.ResourceCountsDtoBuilder> builders = new HashMap<>();

        for (Resource resource : resources) {
            if (resource.getSubject() == null) {
                continue;
            }
            String subjectId = resource.getSubject().getId().toString();
            ResourceCountsDto.ResourceCountsDtoBuilder builder = builders.computeIfAbsent(
                subjectId,
                key -> ResourceCountsDto.builder()
                    .count(0)
                    .documents(0)
                    .images(0)
                    .videos(0)
                    .others(0)
            );

            ResourceCountsDto snapshot = builder.build();
            builder.count(snapshot.getCount() + 1);
            String type = normalizeType(resource);
            switch (type) {
                case "document" -> builder.documents(snapshot.getDocuments() + 1);
                case "image" -> builder.images(snapshot.getImages() + 1);
                case "video" -> builder.videos(snapshot.getVideos() + 1);
                default -> builder.others(snapshot.getOthers() + 1);
            }

            Instant lastUpdated = resource.getUpdatedAt() != null ? resource.getUpdatedAt() : resource.getCreatedAt();
            if (snapshot.getLastUpdated() == null || lastUpdated.isAfter(snapshot.getLastUpdated())) {
                builder.lastUpdated(lastUpdated);
            }
        }

        Map<String, ResourceCountsDto> result = new HashMap<>();
        builders.forEach((key, builder) -> result.put(key, builder.build()));
        return result;
    }

    public List<ResourceRecentDto> recent(int limit) {
        int safeLimit = limit <= 0 ? 5 : limit;
        return resourceRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, safeLimit)).stream()
            .map(this::toRecentDto)
            .toList();
    }

    public ResourceDto upload(MultipartFile file, String subjectId) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File is required");
        }

        Subject subject = null;
        if (subjectId != null && !subjectId.isBlank()) {
            subject = subjectRepository.findById(UUID.fromString(subjectId))
                .orElseThrow(() -> new NotFoundException("Subject not found: " + subjectId));
        }

        Resource resource = new Resource();
        resource.setSchool(resolveSchool());
        resource.setSubject(subject);
        resource.setUploadedBy(resolveUploader());
        resource.setName(file.getOriginalFilename() != null ? file.getOriginalFilename() : "resource");
        resource.setOriginalName(file.getOriginalFilename() != null ? file.getOriginalFilename() : "resource");
        resource.setMimeType(file.getContentType() != null ? file.getContentType() : "application/octet-stream");
        resource.setResType(normalizeType(resource));
        resource.setSizeBytes(file.getSize());
        resource.setStatus("active");

        Resource saved = resourceRepository.save(resource);
        try {
            Files.createDirectories(UPLOAD_ROOT);
            String filename = saved.getId() + "_" + resource.getOriginalName().replaceAll("\\s+", "_");
            Path storagePath = UPLOAD_ROOT.resolve(filename);
            file.transferTo(storagePath);

            saved.setStoragePath(storagePath.toString());
            saved.setStorageKey(filename);
            saved.setUrl("/api/resources/file/" + saved.getId());
            saved = resourceRepository.save(saved);
        } catch (IOException ex) {
            throw new BadRequestException("Failed to store file: " + ex.getMessage());
        }

        return toDto(saved, false);
    }

    public Map<String, String> getDownloadLink(UUID id) {
        Resource resource = get(id);
        resource.setDownloads(resource.getDownloads() == null ? 1 : resource.getDownloads() + 1);
        resourceRepository.save(resource);
        return Map.of("url", resource.getUrl());
    }

    public Path getFilePath(UUID id) {
        Resource resource = get(id);
        if (resource.getStoragePath() == null || resource.getStoragePath().isBlank()) {
            throw new NotFoundException("Resource file not found: " + id);
        }
        return Path.of(resource.getStoragePath());
    }

    private ResourceDto toDto(Resource resource, boolean includeContent) {
        User uploader = resource.getUploadedBy();
        return ResourceDto.builder()
            .id(resource.getId().toString())
            .name(resource.getName())
            .originalName(resource.getOriginalName())
            .mimeType(resource.getMimeType())
            .type(normalizeType(resource))
            .size(resource.getSizeBytes() == null ? 0L : resource.getSizeBytes())
            .url(resource.getUrl())
            .key(resource.getStorageKey())
            .path(resource.getStoragePath())
            .downloads(resource.getDownloads() == null ? 0 : resource.getDownloads())
            .status(resource.getStatus())
            .tags(resource.getTags() == null ? List.of() : Arrays.asList(resource.getTags()))
            .contentType(resource.getContentType())
            .contentBody(includeContent ? resource.getContentBody() : null)
            .publishAt(resource.getPublishAt())
            .subject(resource.getSubject() != null ? resource.getSubject().getId().toString() : null)
            .createdAt(resource.getCreatedAt())
            .updatedAt(resource.getUpdatedAt())
            .uploadedBy(ResourceDto.UploadedBy.builder()
                .id(uploader != null ? uploader.getId().toString() : null)
                .firstName(uploader != null ? uploader.getFirstName() : null)
                .lastName(uploader != null ? uploader.getLastName() : null)
                .build())
            .build();
    }

    private ResourceRecentDto toRecentDto(Resource resource) {
        User uploader = resource.getUploadedBy();
        Subject subject = resource.getSubject();
        return ResourceRecentDto.builder()
            .id(resource.getId().toString())
            .name(resource.getName())
            .type(normalizeType(resource))
            .createdAt(resource.getCreatedAt())
            .subject(subject == null ? null : ResourceRecentDto.SubjectRef.builder()
                .id(subject.getId().toString())
                .name(subject.getName())
                .code(subject.getCode())
                .build())
            .uploadedBy(uploader == null ? null : ResourceRecentDto.UserRef.builder()
                .id(uploader.getId().toString())
                .firstName(uploader.getFirstName())
                .lastName(uploader.getLastName())
                .build())
            .build();
    }

    private String normalizeType(Resource resource) {
        String resType = resource.getResType();
        if (resType != null && !resType.isBlank()) {
            String normalized = resType.toLowerCase(Locale.ROOT);
            return "content".equals(normalized) ? "document" : normalized;
        }
        String mime = resource.getMimeType() != null ? resource.getMimeType().toLowerCase(Locale.ROOT) : "";
        if (mime.startsWith("image/")) {
            return "image";
        }
        if (mime.startsWith("video/")) {
            return "video";
        }
        return "document";
    }

    private School resolveSchool() {
        return schoolRepository.findByCode("ZVHS")
            .orElseGet(() -> schoolRepository.findAll().stream()
                .findFirst()
                .orElseThrow(() -> new NotFoundException("No school found")));
    }

    private User resolveUploader() {
        return userRepository.findByEmail("teacher@zivai.local")
            .orElseGet(() -> userRepository.findAll().stream()
                .findFirst()
                .orElseThrow(() -> new NotFoundException("No user found")));
    }
}
