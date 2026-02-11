package zw.co.zivai.core_backend.services.development;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import zw.co.zivai.core_backend.dtos.development.CreateReteachCardRequest;
import zw.co.zivai.core_backend.dtos.development.ReteachCardDetailDto;
import zw.co.zivai.core_backend.dtos.development.ReteachCardDto;
import zw.co.zivai.core_backend.dtos.students.StudentRefDto;
import zw.co.zivai.core_backend.dtos.development.UpdateReteachCardRequest;
import zw.co.zivai.core_backend.exceptions.BadRequestException;
import zw.co.zivai.core_backend.exceptions.NotFoundException;
import zw.co.zivai.core_backend.models.lms.ReteachCard;
import zw.co.zivai.core_backend.models.lms.Subject;
import zw.co.zivai.core_backend.models.lms.Topic;
import zw.co.zivai.core_backend.models.lms.User;
import zw.co.zivai.core_backend.repositories.development.ReteachCardRepository;
import zw.co.zivai.core_backend.repositories.subject.SubjectRepository;
import zw.co.zivai.core_backend.repositories.subject.TopicRepository;
import zw.co.zivai.core_backend.repositories.user.UserRepository;

@Service
@RequiredArgsConstructor
public class ReteachCardService {
    private final ReteachCardRepository reteachCardRepository;
    private final SubjectRepository subjectRepository;
    private final TopicRepository topicRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public ReteachCardDto create(CreateReteachCardRequest request) {
        if (request == null || request.getSubjectId() == null || request.getSubjectId().isBlank()) {
            throw new BadRequestException("subjectId is required");
        }
        Subject subject = subjectRepository.findById(UUID.fromString(request.getSubjectId()))
            .orElseThrow(() -> new NotFoundException("Subject not found: " + request.getSubjectId()));

        Topic topic = null;
        if (request.getTopicId() != null && !request.getTopicId().isBlank()) {
            topic = topicRepository.findById(UUID.fromString(request.getTopicId()))
                .orElseThrow(() -> new NotFoundException("Topic not found: " + request.getTopicId()));
        }

        String title = resolveTitle(request.getTitle(), topic);
        if (title == null || title.isBlank()) {
            throw new BadRequestException("title is required when topic is not provided");
        }

        ReteachCard card = new ReteachCard();
        card.setSubject(subject);
        card.setTopic(topic);
        card.setTitle(title);
        card.setIssueSummary(request.getIssueSummary());
        card.setRecommendedActions(request.getRecommendedActions());
        card.setPriorityCode(normalizePriority(request.getPriority()));
        card.setStatusCode(normalizeStatus(request.getStatus()));
        if (request.getAffectedStudentIds() != null) {
            card.setAffectedStudentIds(toJson(request.getAffectedStudentIds()));
        }
        if (request.getCreatedBy() != null && !request.getCreatedBy().isBlank()) {
            User createdBy = userRepository.findById(UUID.fromString(request.getCreatedBy()))
                .orElseThrow(() -> new NotFoundException("User not found: " + request.getCreatedBy()));
            card.setCreatedBy(createdBy);
        }
        return toDto(reteachCardRepository.save(card));
    }

    public ReteachCardDto update(UUID id, UpdateReteachCardRequest request) {
        ReteachCard card = reteachCardRepository.findByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> new NotFoundException("Re-teach card not found: " + id));

        if (request != null) {
            if (request.getSubjectId() != null && !request.getSubjectId().isBlank()) {
                Subject subject = subjectRepository.findById(UUID.fromString(request.getSubjectId()))
                    .orElseThrow(() -> new NotFoundException("Subject not found: " + request.getSubjectId()));
                card.setSubject(subject);
            }
            if (request.getTopicId() != null) {
                if (request.getTopicId().isBlank()) {
                    card.setTopic(null);
                } else {
                    Topic topic = topicRepository.findById(UUID.fromString(request.getTopicId()))
                        .orElseThrow(() -> new NotFoundException("Topic not found: " + request.getTopicId()));
                    card.setTopic(topic);
                }
            }
            if (request.getTitle() != null) {
                String title = resolveTitle(request.getTitle(), card.getTopic());
                if (title == null || title.isBlank()) {
                    throw new BadRequestException("title cannot be empty");
                }
                card.setTitle(title);
            }
            if (request.getIssueSummary() != null) {
                card.setIssueSummary(request.getIssueSummary());
            }
            if (request.getRecommendedActions() != null) {
                card.setRecommendedActions(request.getRecommendedActions());
            }
            if (request.getPriority() != null) {
                card.setPriorityCode(normalizePriority(request.getPriority()));
            }
            if (request.getStatus() != null) {
                card.setStatusCode(normalizeStatus(request.getStatus()));
            }
            if (request.getAffectedStudentIds() != null) {
                card.setAffectedStudentIds(toJson(request.getAffectedStudentIds()));
            }
            if (request.getCreatedBy() != null) {
                if (request.getCreatedBy().isBlank()) {
                    card.setCreatedBy(null);
                } else {
                    User createdBy = userRepository.findById(UUID.fromString(request.getCreatedBy()))
                        .orElseThrow(() -> new NotFoundException("User not found: " + request.getCreatedBy()));
                    card.setCreatedBy(createdBy);
                }
            }
        }

        return toDto(reteachCardRepository.save(card));
    }

    public List<ReteachCardDto> list(UUID subjectId, UUID topicId, String priority, String status) {
        List<ReteachCard> cards = reteachCardRepository.findByDeletedAtIsNull();
        return cards.stream()
            .filter(card -> subjectId == null || (card.getSubject() != null && subjectId.equals(card.getSubject().getId())))
            .filter(card -> topicId == null || (card.getTopic() != null && topicId.equals(card.getTopic().getId())))
            .filter(card -> priority == null || normalizePriority(priority).equals(card.getPriorityCode()))
            .filter(card -> status == null || normalizeStatus(status).equals(card.getStatusCode()))
            .map(this::toDto)
            .toList();
    }

    public ReteachCardDto get(UUID id) {
        return reteachCardRepository.findByIdAndDeletedAtIsNull(id)
            .map(this::toDto)
            .orElseThrow(() -> new NotFoundException("Re-teach card not found: " + id));
    }

    public ReteachCardDetailDto getDetail(UUID id) {
        ReteachCard card = reteachCardRepository.findByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> new NotFoundException("Re-teach card not found: " + id));
        return toDetailDto(card);
    }

    public void delete(UUID id) {
        ReteachCard card = reteachCardRepository.findByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> new NotFoundException("Re-teach card not found: " + id));
        card.setDeletedAt(Instant.now());
        reteachCardRepository.save(card);
    }

    private ReteachCardDto toDto(ReteachCard card) {
        List<String> affectedIds = toStringList(card.getAffectedStudentIds());
        return ReteachCardDto.builder()
            .id(card.getId().toString())
            .subjectId(card.getSubject() != null ? card.getSubject().getId().toString() : null)
            .subjectName(card.getSubject() != null ? card.getSubject().getName() : null)
            .topicId(card.getTopic() != null ? card.getTopic().getId().toString() : null)
            .topicName(card.getTopic() != null ? card.getTopic().getName() : null)
            .title(card.getTitle())
            .issueSummary(card.getIssueSummary())
            .recommendedActions(card.getRecommendedActions())
            .priority(card.getPriorityCode())
            .status(card.getStatusCode())
            .affectedStudentIds(affectedIds)
            .affectedStudents(affectedIds.size())
            .createdAt(card.getCreatedAt())
            .updatedAt(card.getUpdatedAt())
            .build();
    }

    private ReteachCardDetailDto toDetailDto(ReteachCard card) {
        List<String> affectedIds = toStringList(card.getAffectedStudentIds());
        List<UUID> studentIds = affectedIds.stream()
            .map(this::safeUuid)
            .filter(id -> id != null)
            .toList();

        List<StudentRefDto> affectedStudents = studentIds.isEmpty()
            ? List.of()
            : userRepository.findAllById(studentIds).stream()
            .map(user -> StudentRefDto.builder()
                .id(user.getId().toString())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .build())
            .toList();

        return ReteachCardDetailDto.builder()
            .id(card.getId().toString())
            .subjectId(card.getSubject() != null ? card.getSubject().getId().toString() : null)
            .subjectName(card.getSubject() != null ? card.getSubject().getName() : null)
            .topicId(card.getTopic() != null ? card.getTopic().getId().toString() : null)
            .topicName(card.getTopic() != null ? card.getTopic().getName() : null)
            .title(card.getTitle())
            .issueSummary(card.getIssueSummary())
            .recommendedActions(card.getRecommendedActions())
            .priority(card.getPriorityCode())
            .status(card.getStatusCode())
            .affectedStudentIds(affectedIds)
            .affectedStudents(affectedStudents)
            .affectedStudentsCount(affectedIds.size())
            .createdAt(card.getCreatedAt())
            .updatedAt(card.getUpdatedAt())
            .build();
    }

    private String resolveTitle(String provided, Topic topic) {
        if (provided != null && !provided.isBlank()) {
            return provided;
        }
        if (topic != null && topic.getName() != null) {
            return topic.getName();
        }
        return null;
    }

    private String normalizePriority(String value) {
        if (value == null || value.isBlank()) {
            return "medium";
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "high", "medium", "low" -> normalized;
            default -> throw new BadRequestException("Invalid priority: " + value);
        };
    }

    private String normalizeStatus(String value) {
        if (value == null || value.isBlank()) {
            return "draft";
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "draft", "active", "resolved", "archived" -> normalized;
            default -> throw new BadRequestException("Invalid status: " + value);
        };
    }

    private JsonNode toJson(Object value) {
        if (value == null) {
            return null;
        }
        return objectMapper.valueToTree(value);
    }

    private List<String> toStringList(JsonNode node) {
        if (node == null || node.isNull()) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        if (node.isArray()) {
            node.forEach(item -> {
                if (item != null && !item.isNull()) {
                    values.add(item.asText());
                }
            });
            return values;
        }
        values.add(node.asText());
        return values;
    }

    private UUID safeUuid(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
