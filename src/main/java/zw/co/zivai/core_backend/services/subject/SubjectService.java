package zw.co.zivai.core_backend.services.subject;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.RequiredArgsConstructor;
import zw.co.zivai.core_backend.dtos.subjects.CreateSubjectRequest;
import zw.co.zivai.core_backend.dtos.subjects.UpdateSubjectRequest;
import zw.co.zivai.core_backend.exceptions.BadRequestException;
import zw.co.zivai.core_backend.exceptions.NotFoundException;
import zw.co.zivai.core_backend.models.lms.Subject;
import zw.co.zivai.core_backend.repositories.subject.SubjectRepository;

@Service
@RequiredArgsConstructor
public class SubjectService {
    private final SubjectRepository subjectRepository;
    private final ObjectMapper objectMapper;

    public Subject create(CreateSubjectRequest request) {
        if (request.getCode() == null || request.getCode().isBlank()) {
            throw new BadRequestException("Subject code is required");
        }
        if (request.getName() == null || request.getName().isBlank()) {
            throw new BadRequestException("Subject name is required");
        }

        Subject subject = new Subject();
        subject.setCode(request.getCode().trim().toUpperCase());
        subject.setName(request.getName().trim());
        subject.setExamBoardCode(normalizeExamBoardCode(request.getExamBoardCode()));
        subject.setDescription(trimToNull(request.getDescription()));
        subject.setSubjectAttributes(buildSubjectAttributes(request.getSubjectAttributes(), request.getGrades(), null));
        subject.setActive(request.isActive());
        return saveSubject(subject);
    }

    public List<Subject> list() {
        return subjectRepository.findAllByDeletedAtIsNull();
    }

    public Subject get(UUID id) {
        return subjectRepository.findByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> new NotFoundException("Subject not found: " + id));
    }

    public Subject update(UUID id, UpdateSubjectRequest request) {
        Subject subject = get(id);

        if (request.getCode() != null && !request.getCode().isBlank()) {
            subject.setCode(request.getCode().trim().toUpperCase());
        }
        if (request.getName() != null && !request.getName().isBlank()) {
            subject.setName(request.getName().trim());
        }
        if (request.getExamBoardCode() != null) {
            subject.setExamBoardCode(normalizeExamBoardCode(request.getExamBoardCode()));
        }
        if (request.getDescription() != null) {
            subject.setDescription(trimToNull(request.getDescription()));
        }
        if (request.getSubjectAttributes() != null || request.getGrades() != null) {
            subject.setSubjectAttributes(
                buildSubjectAttributes(request.getSubjectAttributes(), request.getGrades(), subject.getSubjectAttributes())
            );
        }
        if (request.getActive() != null) {
            subject.setActive(request.getActive());
        }

        return saveSubject(subject);
    }

    public void delete(UUID id) {
        Subject subject = get(id);
        subject.setActive(false);
        subject.setDeletedAt(Instant.now());
        subjectRepository.save(subject);
    }

    private String normalizeExamBoardCode(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return normalized.isBlank() ? null : normalized;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }

    private JsonNode buildSubjectAttributes(Object subjectAttributes, List<String> grades, JsonNode existing) {
        ObjectNode node = objectMapper.createObjectNode();

        if (existing != null && existing.isObject()) {
            node.setAll((ObjectNode) existing);
        }

        if (subjectAttributes != null) {
            JsonNode incoming = objectMapper.valueToTree(subjectAttributes);
            if (incoming != null && incoming.isObject()) {
                node.setAll((ObjectNode) incoming);
            } else {
                throw new BadRequestException("subjectAttributes must be a JSON object");
            }
        }

        if (grades != null) {
            List<String> sanitized = sanitizeGrades(grades);
            if (sanitized.isEmpty()) {
                node.remove("grades");
            } else {
                ArrayNode array = objectMapper.createArrayNode();
                sanitized.forEach(array::add);
                node.set("grades", array);
            }
        }

        return node.isEmpty() ? null : node;
    }

    private List<String> sanitizeGrades(List<String> grades) {
        if (grades == null) {
            return List.of();
        }
        return new ArrayList<>(grades.stream()
            .filter(value -> value != null && !value.isBlank())
            .map(String::trim)
            .collect(LinkedHashSet::new, LinkedHashSet::add, LinkedHashSet::addAll));
    }

    private Subject saveSubject(Subject subject) {
        try {
            return subjectRepository.save(subject);
        } catch (DataIntegrityViolationException ex) {
            String details = ex.getMostSpecificCause() != null
                ? ex.getMostSpecificCause().getMessage()
                : ex.getMessage();
            if (details != null && details.contains("subjects_exam_board_code_fkey")) {
                throw new BadRequestException("Invalid exam board code. Use ZIMSEC or CAMBRIDGE.");
            }
            throw ex;
        }
    }
}
