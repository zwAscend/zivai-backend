package zw.co.zivai.core_backend.services;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import zw.co.zivai.core_backend.dtos.CreateResourceRequest;
import zw.co.zivai.core_backend.exceptions.NotFoundException;
import zw.co.zivai.core_backend.models.lms.Resource;
import zw.co.zivai.core_backend.models.lms.School;
import zw.co.zivai.core_backend.models.lms.Subject;
import zw.co.zivai.core_backend.models.lms.User;
import zw.co.zivai.core_backend.repositories.ResourceRepository;
import zw.co.zivai.core_backend.repositories.SchoolRepository;
import zw.co.zivai.core_backend.repositories.SubjectRepository;
import zw.co.zivai.core_backend.repositories.UserRepository;

@Service
@RequiredArgsConstructor
public class ResourceService {
    private final ResourceRepository resourceRepository;
    private final SchoolRepository schoolRepository;
    private final SubjectRepository subjectRepository;
    private final UserRepository userRepository;

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

        Resource resource = new Resource();
        resource.setSchool(school);
        resource.setSubject(subject);
        resource.setUploadedBy(uploader);
        resource.setName(request.getName());
        resource.setOriginalName(request.getOriginalName());
        resource.setMimeType(request.getMimeType());
        resource.setResType(request.getResType());
        resource.setSizeBytes(request.getSizeBytes());
        resource.setUrl(request.getUrl());
        resource.setStorageKey(request.getStorageKey());
        resource.setStoragePath(request.getStoragePath());
        resource.setDisplayOrder(request.getDisplayOrder());
        resource.setStatus(request.getStatus());

        return resourceRepository.save(resource);
    }

    public List<Resource> list() {
        return resourceRepository.findAll();
    }

    public Resource get(UUID id) {
        return resourceRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Resource not found: " + id));
    }
}
