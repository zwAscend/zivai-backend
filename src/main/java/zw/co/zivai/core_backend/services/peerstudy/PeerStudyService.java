package zw.co.zivai.core_backend.services.peerstudy;

import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import zw.co.zivai.core_backend.dtos.peerstudy.CreatePeerStudyRequest;
import zw.co.zivai.core_backend.dtos.peerstudy.JoinPeerStudyRequest;
import zw.co.zivai.core_backend.dtos.peerstudy.PeerStudyMemberDto;
import zw.co.zivai.core_backend.dtos.peerstudy.PeerStudyRequestDetailDto;
import zw.co.zivai.core_backend.dtos.peerstudy.PeerStudyRequestDto;
import zw.co.zivai.core_backend.dtos.peerstudy.UpdatePeerStudyRequest;
import zw.co.zivai.core_backend.exceptions.BadRequestException;
import zw.co.zivai.core_backend.exceptions.NotFoundException;
import zw.co.zivai.core_backend.models.lms.PeerStudyRequest;
import zw.co.zivai.core_backend.models.lms.PeerStudyRequestMember;
import zw.co.zivai.core_backend.models.lms.Subject;
import zw.co.zivai.core_backend.models.lms.Topic;
import zw.co.zivai.core_backend.models.lms.User;
import zw.co.zivai.core_backend.repositories.peerstudy.PeerStudyRequestMemberRepository;
import zw.co.zivai.core_backend.repositories.peerstudy.PeerStudyRequestRepository;
import zw.co.zivai.core_backend.repositories.subject.SubjectRepository;
import zw.co.zivai.core_backend.repositories.subject.TopicRepository;
import zw.co.zivai.core_backend.repositories.user.UserRepository;

@Service
@RequiredArgsConstructor
public class PeerStudyService {
    private static final int DEFAULT_MAX_PARTICIPANTS = 6;

    private final PeerStudyRequestRepository peerStudyRequestRepository;
    private final PeerStudyRequestMemberRepository peerStudyRequestMemberRepository;
    private final SubjectRepository subjectRepository;
    private final TopicRepository topicRepository;
    private final UserRepository userRepository;

    @Transactional
    public PeerStudyRequestDto create(CreatePeerStudyRequest request) {
        if (request == null) {
            throw new BadRequestException("Request body is required");
        }
        if (request.getSubjectId() == null || request.getSubjectId().isBlank()) {
            throw new BadRequestException("subjectId is required");
        }
        if (request.getCreatedBy() == null || request.getCreatedBy().isBlank()) {
            throw new BadRequestException("createdBy is required");
        }
        if (request.getNote() == null || request.getNote().isBlank()) {
            throw new BadRequestException("note is required");
        }

        Subject subject = subjectRepository.findByIdAndDeletedAtIsNull(parseUuid(request.getSubjectId(), "subjectId"))
            .orElseThrow(() -> new NotFoundException("Subject not found: " + request.getSubjectId()));
        User createdBy = userRepository.findByIdAndDeletedAtIsNull(parseUuid(request.getCreatedBy(), "createdBy"))
            .orElseThrow(() -> new NotFoundException("User not found: " + request.getCreatedBy()));

        Topic topic = resolveTopic(request.getTopicId());
        if (topic != null && !topic.getSubject().getId().equals(subject.getId())) {
            throw new BadRequestException("topicId does not belong to subjectId");
        }

        PeerStudyRequest entity = new PeerStudyRequest();
        entity.setSubject(subject);
        entity.setTopic(topic);
        entity.setTopicTitle(resolveTopicTitle(request.getTopic(), topic));
        entity.setRequestType(normalizeType(request.getType()));
        entity.setNote(request.getNote().trim());
        entity.setPreferredTime(request.getPreferredTime());
        entity.setStatusCode("open");
        entity.setMaxParticipants(resolveMaxParticipants(request.getMaxParticipants()));
        entity.setCreatedBy(createdBy);
        entity = peerStudyRequestRepository.save(entity);

        PeerStudyRequestMember host = new PeerStudyRequestMember();
        host.setRequest(entity);
        host.setUser(createdBy);
        host.setRoleCode("host");
        host.setJoinedAt(Instant.now());
        peerStudyRequestMemberRepository.save(host);

        return toSummaryDto(entity, 1, true);
    }

    @Transactional(readOnly = true)
    public List<PeerStudyRequestDto> list(UUID subjectId,
                                          UUID topicId,
                                          String type,
                                          String status,
                                          UUID createdBy,
                                          UUID joinedBy) {
        String normalizedType = type != null && !type.isBlank() ? normalizeType(type) : null;
        String normalizedStatus = status != null && !status.isBlank() ? normalizeStatus(status) : null;

        List<PeerStudyRequest> requests = peerStudyRequestRepository.findFilteredForList(
            subjectId, topicId, normalizedType, normalizedStatus, createdBy, joinedBy
        );
        if (requests.isEmpty()) {
            return List.of();
        }

        List<UUID> requestIds = requests.stream().map(PeerStudyRequest::getId).toList();
        Map<UUID, Integer> participantsByRequest = loadParticipantsByRequestId(requestIds);
        Set<UUID> joinedRequestIds = joinedBy == null
            ? Set.of()
            : new HashSet<>(peerStudyRequestMemberRepository.findJoinedRequestIds(joinedBy, requestIds));

        return requests.stream()
            .map(item -> toSummaryDto(
                item,
                participantsByRequest.getOrDefault(item.getId(), 0),
                joinedRequestIds.contains(item.getId())
            ))
            .toList();
    }

    @Transactional(readOnly = true)
    public PeerStudyRequestDetailDto getDetail(UUID id, UUID viewerId) {
        PeerStudyRequest request = peerStudyRequestRepository.findByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> new NotFoundException("Peer study request not found: " + id));
        List<PeerStudyRequestMember> members =
            peerStudyRequestMemberRepository.findByRequest_IdAndDeletedAtIsNullOrderByJoinedAtAsc(id);
        boolean joined = viewerId != null && members.stream()
            .anyMatch(member -> member.getUser() != null && viewerId.equals(member.getUser().getId()));
        return toDetailDto(request, members, joined);
    }

    @Transactional
    public PeerStudyRequestDto update(UUID id, UpdatePeerStudyRequest request) {
        PeerStudyRequest entity = peerStudyRequestRepository.findByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> new NotFoundException("Peer study request not found: " + id));

        if (request != null) {
            if (request.getSubjectId() != null && !request.getSubjectId().isBlank()) {
                Subject subject = subjectRepository.findByIdAndDeletedAtIsNull(parseUuid(request.getSubjectId(), "subjectId"))
                    .orElseThrow(() -> new NotFoundException("Subject not found: " + request.getSubjectId()));
                entity.setSubject(subject);
            }

            if (request.getTopicId() != null) {
                if (request.getTopicId().isBlank()) {
                    entity.setTopic(null);
                } else {
                    Topic topic = resolveTopic(request.getTopicId());
                    if (topic == null) {
                        throw new NotFoundException("Topic not found: " + request.getTopicId());
                    }
                    if (!topic.getSubject().getId().equals(entity.getSubject().getId())) {
                        throw new BadRequestException("topicId does not belong to current subject");
                    }
                    entity.setTopic(topic);
                }
            }

            if (request.getTopic() != null) {
                entity.setTopicTitle(resolveTopicTitle(request.getTopic(), entity.getTopic()));
            }
            if (request.getType() != null) {
                entity.setRequestType(normalizeType(request.getType()));
            }
            if (request.getNote() != null) {
                if (request.getNote().isBlank()) {
                    throw new BadRequestException("note cannot be empty");
                }
                entity.setNote(request.getNote().trim());
            }
            if (request.getPreferredTime() != null) {
                entity.setPreferredTime(request.getPreferredTime());
            }
            if (request.getMaxParticipants() != null) {
                int maxParticipants = resolveMaxParticipants(request.getMaxParticipants());
                int currentParticipants = (int) peerStudyRequestMemberRepository
                    .countByRequest_IdAndDeletedAtIsNull(entity.getId());
                if (maxParticipants < currentParticipants) {
                    throw new BadRequestException("maxParticipants cannot be less than current participants");
                }
                entity.setMaxParticipants(maxParticipants);
            }
            if (request.getStatus() != null) {
                entity.setStatusCode(normalizeStatus(request.getStatus()));
            }
        }

        entity = peerStudyRequestRepository.save(entity);
        int participants = (int) peerStudyRequestMemberRepository.countByRequest_IdAndDeletedAtIsNull(entity.getId());
        return toSummaryDto(entity, participants, false);
    }

    @Transactional
    public PeerStudyRequestDetailDto join(UUID requestId, JoinPeerStudyRequest request) {
        if (request == null || request.getStudentId() == null || request.getStudentId().isBlank()) {
            throw new BadRequestException("studentId is required");
        }
        UUID studentId = parseUuid(request.getStudentId(), "studentId");

        PeerStudyRequest studyRequest = peerStudyRequestRepository.findByIdAndDeletedAtIsNull(requestId)
            .orElseThrow(() -> new NotFoundException("Peer study request not found: " + requestId));
        User student = userRepository.findByIdAndDeletedAtIsNull(studentId)
            .orElseThrow(() -> new NotFoundException("Student not found: " + request.getStudentId()));

        PeerStudyRequestMember existingMember = peerStudyRequestMemberRepository
            .findByRequest_IdAndUser_IdAndDeletedAtIsNull(requestId, studentId)
            .orElse(null);
        if (existingMember != null) {
            return getDetail(requestId, studentId);
        }

        int participants = (int) peerStudyRequestMemberRepository.countByRequest_IdAndDeletedAtIsNull(requestId);
        if ("closed".equals(studyRequest.getStatusCode()) || "cancelled".equals(studyRequest.getStatusCode())) {
            throw new BadRequestException("Request is not open for joining");
        }
        if (participants >= studyRequest.getMaxParticipants()) {
            if (!"filled".equals(studyRequest.getStatusCode())) {
                studyRequest.setStatusCode("filled");
                peerStudyRequestRepository.save(studyRequest);
            }
            throw new BadRequestException("Request is full");
        }

        PeerStudyRequestMember member = new PeerStudyRequestMember();
        member.setRequest(studyRequest);
        member.setUser(student);
        member.setRoleCode("member");
        member.setJoinedAt(Instant.now());
        peerStudyRequestMemberRepository.save(member);

        int updatedParticipants = participants + 1;
        if (updatedParticipants >= studyRequest.getMaxParticipants() && !"filled".equals(studyRequest.getStatusCode())) {
            studyRequest.setStatusCode("filled");
            peerStudyRequestRepository.save(studyRequest);
        }

        return getDetail(requestId, studentId);
    }

    private Map<UUID, Integer> loadParticipantsByRequestId(Collection<UUID> requestIds) {
        if (requestIds == null || requestIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, Integer> counts = new HashMap<>();
        for (Object[] row : peerStudyRequestMemberRepository.countByRequestIds(requestIds)) {
            if (row == null || row.length < 2 || row[0] == null || row[1] == null) {
                continue;
            }
            UUID requestId = (UUID) row[0];
            long count = ((Number) row[1]).longValue();
            counts.put(requestId, Math.toIntExact(count));
        }
        return counts;
    }

    private PeerStudyRequestDto toSummaryDto(PeerStudyRequest request, int participants, boolean joined) {
        return PeerStudyRequestDto.builder()
            .id(request.getId().toString())
            .subjectId(request.getSubject() != null ? request.getSubject().getId().toString() : null)
            .subjectName(request.getSubject() != null ? request.getSubject().getName() : null)
            .topicId(request.getTopic() != null ? request.getTopic().getId().toString() : null)
            .topic(request.getTopicTitle())
            .type(request.getRequestType())
            .note(request.getNote())
            .preferredTime(request.getPreferredTime())
            .status(request.getStatusCode())
            .maxParticipants(request.getMaxParticipants())
            .participants(participants)
            .createdById(request.getCreatedBy() != null ? request.getCreatedBy().getId().toString() : null)
            .createdByName(fullName(request.getCreatedBy()))
            .joined(joined)
            .createdAt(request.getCreatedAt())
            .updatedAt(request.getUpdatedAt())
            .build();
    }

    private PeerStudyRequestDetailDto toDetailDto(PeerStudyRequest request,
                                                  List<PeerStudyRequestMember> members,
                                                  boolean joined) {
        List<PeerStudyMemberDto> memberDtos = members.stream()
            .map(member -> PeerStudyMemberDto.builder()
                .userId(member.getUser() != null ? member.getUser().getId().toString() : null)
                .firstName(member.getUser() != null ? member.getUser().getFirstName() : null)
                .lastName(member.getUser() != null ? member.getUser().getLastName() : null)
                .role(member.getRoleCode())
                .joinedAt(member.getJoinedAt())
                .build())
            .toList();

        return PeerStudyRequestDetailDto.builder()
            .id(request.getId().toString())
            .subjectId(request.getSubject() != null ? request.getSubject().getId().toString() : null)
            .subjectName(request.getSubject() != null ? request.getSubject().getName() : null)
            .topicId(request.getTopic() != null ? request.getTopic().getId().toString() : null)
            .topic(request.getTopicTitle())
            .type(request.getRequestType())
            .note(request.getNote())
            .preferredTime(request.getPreferredTime())
            .status(request.getStatusCode())
            .maxParticipants(request.getMaxParticipants())
            .participants(memberDtos.size())
            .createdById(request.getCreatedBy() != null ? request.getCreatedBy().getId().toString() : null)
            .createdByName(fullName(request.getCreatedBy()))
            .joined(joined)
            .members(memberDtos)
            .createdAt(request.getCreatedAt())
            .updatedAt(request.getUpdatedAt())
            .build();
    }

    private String normalizeType(String value) {
        if (value == null || value.isBlank()) {
            return "need-help";
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT).replace('_', '-');
        return switch (normalized) {
            case "need-help", "offer-help", "study-group" -> normalized;
            default -> throw new BadRequestException("Invalid peer study type: " + value);
        };
    }

    private String normalizeStatus(String value) {
        if (value == null || value.isBlank()) {
            return "open";
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT).replace('_', '-');
        return switch (normalized) {
            case "open", "filled", "closed", "cancelled" -> normalized;
            default -> throw new BadRequestException("Invalid peer study status: " + value);
        };
    }

    private int resolveMaxParticipants(Integer value) {
        if (value == null) {
            return DEFAULT_MAX_PARTICIPANTS;
        }
        if (value <= 0) {
            throw new BadRequestException("maxParticipants must be greater than 0");
        }
        return value;
    }

    private Topic resolveTopic(String topicId) {
        if (topicId == null || topicId.isBlank()) {
            return null;
        }
        UUID id = parseUuid(topicId, "topicId");
        return topicRepository.findById(id)
            .filter(topic -> topic.getDeletedAt() == null)
            .orElseThrow(() -> new NotFoundException("Topic not found: " + topicId));
    }

    private UUID parseUuid(String value, String fieldName) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid UUID for " + fieldName + ": " + value);
        }
    }

    private String resolveTopicTitle(String providedTopic, Topic topic) {
        if (providedTopic != null && !providedTopic.isBlank()) {
            return providedTopic.trim();
        }
        if (topic != null && topic.getName() != null && !topic.getName().isBlank()) {
            return topic.getName().trim();
        }
        throw new BadRequestException("topic is required when topicId is not provided");
    }

    private String fullName(User user) {
        if (user == null) {
            return null;
        }
        String firstName = user.getFirstName() == null ? "" : user.getFirstName().trim();
        String lastName = user.getLastName() == null ? "" : user.getLastName().trim();
        String value = (firstName + " " + lastName).trim();
        return value.isEmpty() ? null : value;
    }
}
