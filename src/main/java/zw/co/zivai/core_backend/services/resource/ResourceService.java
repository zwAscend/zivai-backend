package zw.co.zivai.core_backend.services.resource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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
import zw.co.zivai.core_backend.models.lms.resources.Resource;
import zw.co.zivai.core_backend.models.lms.school.School;
import zw.co.zivai.core_backend.models.lms.subjects.Subject;
import zw.co.zivai.core_backend.models.lms.resources.Topic;
import zw.co.zivai.core_backend.models.lms.resources.TopicResource;
import zw.co.zivai.core_backend.models.lms.users.User;
import zw.co.zivai.core_backend.repositories.classroom.StudentSubjectEnrolmentRepository;
import zw.co.zivai.core_backend.repositories.resource.ResourceRepository;
import zw.co.zivai.core_backend.repositories.resource.TopicResourceRepository;
import zw.co.zivai.core_backend.repositories.school.SchoolRepository;
import zw.co.zivai.core_backend.repositories.subject.SubjectRepository;
import zw.co.zivai.core_backend.repositories.subject.TopicRepository;
import zw.co.zivai.core_backend.repositories.user.UserRepository;
import zw.co.zivai.core_backend.services.notification.NotificationService;

@Service
@RequiredArgsConstructor
public class ResourceService {
    private final ResourceRepository resourceRepository;
    private final SchoolRepository schoolRepository;
    private final SubjectRepository subjectRepository;
    private final TopicRepository topicRepository;
    private final UserRepository userRepository;
    private final TopicResourceRepository topicResourceRepository;
    private final StudentSubjectEnrolmentRepository studentSubjectEnrolmentRepository;
    private final NotificationService notificationService;

    private static final Path UPLOAD_ROOT = Path.of("core-backend", "uploads");
    private static final Set<String> ALLOWED_RESOURCE_TYPES = Set.of("document", "image", "video", "other");

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
        resource.setResType(resolveResourceType(request.getResType(), request.getMimeType(), isContent));
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
        if (request.getTopicIds() != null) {
            syncResourceTopics(saved, request.getTopicIds());
        }
        notifyIfVisibleToStudents(saved, false);
        return saved;
    }

    public List<ResourceDto> list(UUID subjectId, String status) {
        List<Resource> resources;
        if (subjectId != null && status != null && !status.isBlank()) {
            resources = resourceRepository.findBySubject_IdAndStatusAndDeletedAtIsNull(subjectId, status);
        } else if (subjectId != null) {
            resources = resourceRepository.findBySubject_IdAndDeletedAtIsNull(subjectId);
        } else if (status != null && !status.isBlank()) {
            resources = resourceRepository.findByStatusAndDeletedAtIsNull(status);
        } else {
            resources = resourceRepository.findByDeletedAtIsNull();
        }

        Map<UUID, List<String>> topicIdsByResourceId = resolveTopicIdsMap(resources);

        return resources.stream()
            .map(resource -> toDto(resource, false, topicIdsByResourceId.getOrDefault(resource.getId(), List.of())))
            .toList();
    }

    public Resource get(UUID id) {
        return resourceRepository.findByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> new NotFoundException("Resource not found: " + id));
    }

    public List<ResourceDto> listBySubject(UUID subjectId) {
        List<Resource> resources = resourceRepository.findBySubject_IdAndDeletedAtIsNull(subjectId);
        Map<UUID, List<String>> topicIdsByResourceId = resolveTopicIdsMap(resources);
        return resources.stream()
            .map(resource -> toDto(resource, false, topicIdsByResourceId.getOrDefault(resource.getId(), List.of())))
            .toList();
    }

    public List<ResourceDto> listByTopic(UUID topicId) {
        if (!topicRepository.existsById(topicId)) {
            throw new NotFoundException("Topic not found: " + topicId);
        }
        List<Resource> resources = topicResourceRepository.findByTopic_IdAndDeletedAtIsNullOrderByDisplayOrderAscCreatedAtAsc(topicId).stream()
            .map(TopicResource::getResource)
            .filter(Objects::nonNull)
            .filter(resource -> resource.getDeletedAt() == null)
            .toList();
        Map<UUID, List<String>> topicIdsByResourceId = resolveTopicIdsMap(resources);
        return resources.stream()
            .map(resource -> toDto(resource, false, topicIdsByResourceId.getOrDefault(resource.getId(), List.of())))
            .toList();
    }

    public ResourceDto getContent(UUID id) {
        Resource resource = get(id);
        return toDto(resource, true, resolveTopicIds(resource.getId()));
    }

    public ResourceDto update(UUID id, UpdateResourceRequest request) {
        Resource resource = get(id);
        boolean wasVisibleToStudents = isVisibleToStudents(resource);

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
            resource.setResType(resolveResourceType(request.getResType(), resource.getMimeType(), resource.getContentBody() != null && !resource.getContentBody().isBlank()));
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
        if (request.getTopicIds() != null) {
            syncResourceTopics(saved, request.getTopicIds());
        }
        notifyIfVisibleToStudents(saved, wasVisibleToStudents);
        return toDto(saved, true, resolveTopicIds(saved.getId()));
    }

    public void delete(UUID id) {
        Resource resource = get(id);
        resource.setDeletedAt(Instant.now());
        resource.setStatus("archived");
        resourceRepository.save(resource);
    }

    public ResourceDto addTopics(UUID resourceId, List<UUID> topicIds) {
        Resource resource = get(resourceId);
        List<UUID> existingTopicIds = resolveTopicIds(resourceId).stream()
            .map(UUID::fromString)
            .collect(Collectors.toCollection(ArrayList::new));

        if (topicIds != null) {
            for (UUID topicId : topicIds) {
                if (topicId != null && !existingTopicIds.contains(topicId)) {
                    existingTopicIds.add(topicId);
                }
            }
        }

        syncResourceTopics(resource, existingTopicIds);
        Resource saved = resourceRepository.save(resource);
        return toDto(saved, true, resolveTopicIds(saved.getId()));
    }

    public ResourceDto removeTopic(UUID resourceId, UUID topicId) {
        Resource resource = get(resourceId);
        TopicResource link = topicResourceRepository.findByResource_IdAndTopic_IdAndDeletedAtIsNull(resourceId, topicId)
            .orElseThrow(() -> new NotFoundException("Topic link not found for resource: " + topicId));
        link.setDeletedAt(Instant.now());
        topicResourceRepository.save(link);
        normalizeTopicDisplayOrder(resourceId);
        return toDto(resource, true, resolveTopicIds(resourceId));
    }

    public Map<String, ResourceCountsDto> getCounts() {
        List<Resource> resources = resourceRepository.findByDeletedAtIsNull();
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

        return toDto(saved, false, resolveTopicIds(saved.getId()));
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

    private ResourceDto toDto(Resource resource, boolean includeContent, List<String> topicIds) {
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
            .topicIds(topicIds == null ? List.of() : topicIds)
            .createdAt(resource.getCreatedAt())
            .updatedAt(resource.getUpdatedAt())
            .uploadedBy(ResourceDto.UploadedBy.builder()
                .id(uploader != null ? uploader.getId().toString() : null)
                .firstName(uploader != null ? uploader.getFirstName() : null)
                .lastName(uploader != null ? uploader.getLastName() : null)
                .build())
            .build();
    }

    private List<String> resolveTopicIds(UUID resourceId) {
        return topicResourceRepository.findByResource_IdAndDeletedAtIsNullOrderByDisplayOrderAscCreatedAtAsc(resourceId).stream()
            .map(TopicResource::getTopic)
            .filter(Objects::nonNull)
            .map(Topic::getId)
            .map(UUID::toString)
            .toList();
    }

    private Map<UUID, List<String>> resolveTopicIdsMap(List<Resource> resources) {
        if (resources == null || resources.isEmpty()) {
            return Map.of();
        }
        Set<UUID> resourceIds = resources.stream()
            .map(Resource::getId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        if (resourceIds.isEmpty()) {
            return Map.of();
        }

        Map<UUID, List<String>> topicIdsByResourceId = new HashMap<>();
        for (TopicResource link : topicResourceRepository.findByResource_IdInAndDeletedAtIsNullOrderByDisplayOrderAscCreatedAtAsc(List.copyOf(resourceIds))) {
            if (link.getResource() == null || link.getResource().getId() == null || link.getTopic() == null || link.getTopic().getId() == null) {
                continue;
            }
            topicIdsByResourceId
                .computeIfAbsent(link.getResource().getId(), key -> new java.util.ArrayList<>())
                .add(link.getTopic().getId().toString());
        }
        return topicIdsByResourceId;
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

    private String resolveResourceType(String requestedType, String mimeType, boolean isContent) {
        if (requestedType != null && !requestedType.isBlank()) {
            String normalized = requestedType.trim().toLowerCase(Locale.ROOT);
            if ("content".equals(normalized)) {
                return "document";
            }
            if (ALLOWED_RESOURCE_TYPES.contains(normalized)) {
                return normalized;
            }
        }
        if (mimeType != null && !mimeType.isBlank()) {
            String normalizedMime = mimeType.toLowerCase(Locale.ROOT);
            if (normalizedMime.startsWith("image/")) {
                return "image";
            }
            if (normalizedMime.startsWith("video/")) {
                return "video";
            }
        }
        return isContent ? "document" : "other";
    }

    private void syncResourceTopics(Resource resource, List<UUID> requestedTopicIds) {
        List<UUID> orderedTopicIds = requestedTopicIds == null
            ? List.of()
            : requestedTopicIds.stream()
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.collectingAndThen(
                    java.util.stream.Collectors.toCollection(LinkedHashSet::new),
                    List::copyOf
                ));

        List<TopicResource> existingLinks = topicResourceRepository.findByResource_Id(resource.getId());
        Map<UUID, TopicResource> existingByTopicId = existingLinks.stream()
            .filter(link -> link.getTopic() != null)
            .collect(java.util.stream.Collectors.toMap(
                link -> link.getTopic().getId(),
                link -> link,
                (left, right) -> left
            ));

        Map<UUID, Topic> topicsById = topicRepository.findAllById(orderedTopicIds).stream()
            .filter(topic -> topic.getDeletedAt() == null)
            .collect(Collectors.toMap(Topic::getId, topic -> topic));

        Instant now = Instant.now();
        int order = 0;
        List<TopicResource> linksToPersist = new ArrayList<>();

        for (UUID topicId : orderedTopicIds) {
            Topic topic = topicsById.get(topicId);
            if (topic == null) {
                throw new NotFoundException("Topic not found: " + topicId);
            }
            if (resource.getSubject() != null
                && topic.getSubject() != null
                && !resource.getSubject().getId().equals(topic.getSubject().getId())) {
                throw new BadRequestException("Topic " + topicId + " does not belong to the resource subject");
            }

            TopicResource link = existingByTopicId.get(topicId);
            if (link == null) {
                link = new TopicResource();
                link.setResource(resource);
                link.setTopic(topic);
            }
            link.setDeletedAt(null);
            link.setDisplayOrder(order++);
            linksToPersist.add(link);
        }

        for (TopicResource link : existingLinks) {
            UUID topicId = link.getTopic() != null ? link.getTopic().getId() : null;
            if (topicId != null && !orderedTopicIds.contains(topicId) && link.getDeletedAt() == null) {
                link.setDeletedAt(now);
                linksToPersist.add(link);
            }
        }

        if (!linksToPersist.isEmpty()) {
            topicResourceRepository.saveAll(linksToPersist);
        }
    }

    private void normalizeTopicDisplayOrder(UUID resourceId) {
        List<TopicResource> activeLinks = topicResourceRepository
            .findByResource_IdAndDeletedAtIsNullOrderByDisplayOrderAscCreatedAtAsc(resourceId);
        for (int index = 0; index < activeLinks.size(); index += 1) {
            activeLinks.get(index).setDisplayOrder(index);
        }
        if (!activeLinks.isEmpty()) {
            topicResourceRepository.saveAll(activeLinks);
        }
    }

    private School resolveSchool() {
        return schoolRepository.findByCode("ZVHS")
            .orElseGet(() -> schoolRepository.findFirstByDeletedAtIsNullOrderByCreatedAtAsc()
                .orElseThrow(() -> new NotFoundException("No school found")));
    }

    private User resolveUploader() {
        return userRepository.findByEmailAndDeletedAtIsNull("teacher@zivai.local")
            .orElseGet(() -> userRepository.findFirstByDeletedAtIsNullOrderByCreatedAtAsc()
                .orElseThrow(() -> new NotFoundException("No user found")));
    }

    private boolean isVisibleToStudents(Resource resource) {
        if (resource == null || resource.getDeletedAt() != null || resource.getSubject() == null) {
            return false;
        }
        String status = resource.getStatus() == null ? "" : resource.getStatus().trim().toLowerCase(Locale.ROOT);
        boolean activeStatus = status.isBlank()
            || "active".equals(status)
            || "published".equals(status);
        if (!activeStatus) {
            return false;
        }
        Instant publishAt = resource.getPublishAt();
        return publishAt == null || !publishAt.isAfter(Instant.now());
    }

    private void notifyIfVisibleToStudents(Resource resource, boolean wasVisibleToStudents) {
        if (!isVisibleToStudents(resource) || wasVisibleToStudents) {
            return;
        }
        Subject subject = resource.getSubject();
        School school = resource.getSchool();
        if (subject == null || subject.getId() == null || school == null || school.getId() == null) {
            return;
        }

        List<String> topicIds = resolveTopicIds(resource.getId());
        if (topicIds.isEmpty()) {
            return;
        }

        List<UUID> recipientIds = studentSubjectEnrolmentRepository.findDistinctStudentsBySubjectId(subject.getId()).stream()
            .map(User::getId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
        if (recipientIds.isEmpty()) {
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("resourceId", resource.getId().toString());
        data.put("resourceName", resource.getName());
        data.put("subjectId", subject.getId().toString());
        data.put("subjectName", safeSubjectName(subject));
        data.put("topicIds", new ArrayList<>(topicIds));
        data.put("event", "resource_published");

        notificationService.createBulk(
            school.getId(),
            recipientIds,
            "resource_published",
            "New resource published",
            resource.getName() + " is now available in " + safeSubjectName(subject) + ".",
            data,
            "medium"
        );
    }

    private String safeSubjectName(Subject subject) {
        if (subject == null) {
            return "your subject";
        }
        if (subject.getName() != null && !subject.getName().isBlank()) {
            return subject.getName();
        }
        if (subject.getCode() != null && !subject.getCode().isBlank()) {
            return subject.getCode();
        }
        return "your subject";
    }
}
