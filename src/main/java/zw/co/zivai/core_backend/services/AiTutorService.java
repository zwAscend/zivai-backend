package zw.co.zivai.core_backend.services;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import zw.co.zivai.core_backend.dtos.AiTutorMessageDto;
import zw.co.zivai.core_backend.dtos.AiTutorSessionDto;
import zw.co.zivai.core_backend.dtos.CreateAiTutorMessageRequest;
import zw.co.zivai.core_backend.dtos.CreateAiTutorSessionRequest;
import zw.co.zivai.core_backend.exceptions.BadRequestException;
import zw.co.zivai.core_backend.exceptions.NotFoundException;
import zw.co.zivai.core_backend.models.lms.AiTutorMessage;
import zw.co.zivai.core_backend.models.lms.AiTutorSession;
import zw.co.zivai.core_backend.models.lms.Subject;
import zw.co.zivai.core_backend.models.lms.User;
import zw.co.zivai.core_backend.repositories.AiTutorMessageRepository;
import zw.co.zivai.core_backend.repositories.AiTutorSessionRepository;
import zw.co.zivai.core_backend.repositories.SubjectRepository;
import zw.co.zivai.core_backend.repositories.UserRepository;

@Service
@RequiredArgsConstructor
public class AiTutorService {
    private final AiTutorSessionRepository sessionRepository;
    private final AiTutorMessageRepository messageRepository;
    private final UserRepository userRepository;
    private final SubjectRepository subjectRepository;

    public AiTutorSessionDto getOrCreateSession(CreateAiTutorSessionRequest request) {
        if (request == null || request.getStudentId() == null || request.getStudentId().isBlank()) {
            throw new BadRequestException("studentId is required");
        }
        if (request.getSubjectId() == null || request.getSubjectId().isBlank()) {
            throw new BadRequestException("subjectId is required");
        }
        UUID studentId = UUID.fromString(request.getStudentId());
        UUID subjectId = UUID.fromString(request.getSubjectId());

        User student = userRepository.findByIdAndDeletedAtIsNull(studentId)
            .orElseThrow(() -> new NotFoundException("Student not found: " + request.getStudentId()));
        Subject subject = subjectRepository.findByIdAndDeletedAtIsNull(subjectId)
            .orElseThrow(() -> new NotFoundException("Subject not found: " + request.getSubjectId()));

        AiTutorSession session = sessionRepository
            .findByStudent_IdAndSubject_IdAndDeletedAtIsNull(studentId, subjectId)
            .orElseGet(() -> {
                AiTutorSession created = new AiTutorSession();
                created.setStudent(student);
                created.setSubject(subject);
                created.setStatusCode("active");
                if (request.getCreatedBy() != null && !request.getCreatedBy().isBlank()) {
                    User creator = userRepository.findById(UUID.fromString(request.getCreatedBy()))
                        .orElse(null);
                    created.setCreatedBy(creator);
                } else {
                    created.setCreatedBy(student);
                }
                return sessionRepository.save(created);
            });

        return toSessionDto(session);
    }

    public List<AiTutorSessionDto> listSessions(UUID studentId) {
        if (studentId == null) {
            throw new BadRequestException("studentId is required");
        }
        return sessionRepository.findByStudent_IdAndDeletedAtIsNull(studentId).stream()
            .map(this::toSessionDto)
            .toList();
    }

    public List<AiTutorMessageDto> listMessages(UUID sessionId) {
        if (sessionId == null) {
            throw new BadRequestException("sessionId is required");
        }
        AiTutorSession session = sessionRepository.findByIdAndDeletedAtIsNull(sessionId)
            .orElseThrow(() -> new NotFoundException("AI tutor session not found: " + sessionId));
        return messageRepository.findBySession_IdAndDeletedAtIsNullOrderByTsAsc(session.getId()).stream()
            .map(this::toMessageDto)
            .toList();
    }

    public AiTutorMessageDto createMessage(CreateAiTutorMessageRequest request) {
        if (request == null || request.getSessionId() == null || request.getSessionId().isBlank()) {
            throw new BadRequestException("sessionId is required");
        }
        UUID sessionId = UUID.fromString(request.getSessionId());
        AiTutorSession session = sessionRepository.findByIdAndDeletedAtIsNull(sessionId)
            .orElseThrow(() -> new NotFoundException("AI tutor session not found: " + request.getSessionId()));

        String content = request.getContent();
        String transcript = request.getTranscript();
        if ((content == null || content.isBlank()) && (transcript == null || transcript.isBlank())
            && request.getContentPayload() == null) {
            throw new BadRequestException("content, transcript, or contentPayload is required");
        }

        AiTutorMessage message = new AiTutorMessage();
        message.setSession(session);
        message.setSender(resolveSender(request.getSenderId()));
        message.setSenderRole(normalizeSenderRole(request.getSenderRole()));
        message.setContentType(normalizeContentType(request.getContentType()));
        message.setContent(content);
        message.setTranscript(transcript);
        message.setAudioUrl(request.getAudioUrl());
        message.setContentPayload(request.getContentPayload());
        message.setTs(Instant.now());

        AiTutorMessage saved = messageRepository.save(message);
        session.setLastMessageAt(saved.getTs());
        sessionRepository.save(session);

        if (Boolean.TRUE.equals(request.getAutoReply())
            && "student".equalsIgnoreCase(saved.getSenderRole())) {
            messageRepository.save(buildPlaceholderReply(session));
        }

        return toMessageDto(saved);
    }

    private AiTutorMessage buildPlaceholderReply(AiTutorSession session) {
        AiTutorMessage reply = new AiTutorMessage();
        reply.setSession(session);
        reply.setSenderRole("tutor");
        reply.setContentType("text");
        reply.setContent("Your request has been saved. The AI tutor response will appear once it is enabled.");
        reply.setTs(Instant.now());
        session.setLastMessageAt(reply.getTs());
        sessionRepository.save(session);
        return reply;
    }

    private User resolveSender(String senderId) {
        if (senderId == null || senderId.isBlank()) {
            return null;
        }
        return userRepository.findById(UUID.fromString(senderId)).orElse(null);
    }

    private String normalizeSenderRole(String role) {
        if (role == null || role.isBlank()) {
            return "student";
        }
        String normalized = role.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "student", "tutor", "system" -> normalized;
            default -> "student";
        };
    }

    private String normalizeContentType(String type) {
        if (type == null || type.isBlank()) {
            return "text";
        }
        String normalized = type.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "text", "voice", "content" -> normalized;
            default -> "text";
        };
    }

    private AiTutorSessionDto toSessionDto(AiTutorSession session) {
        User student = session.getStudent();
        Subject subject = session.getSubject();
        return AiTutorSessionDto.builder()
            .id(session.getId().toString())
            .studentId(student != null ? student.getId().toString() : null)
            .studentName(student != null ? student.getFirstName() + " " + student.getLastName() : null)
            .subjectId(subject != null ? subject.getId().toString() : null)
            .subjectName(subject != null ? subject.getName() : null)
            .status(session.getStatusCode())
            .lastMessageAt(session.getLastMessageAt())
            .createdAt(session.getCreatedAt())
            .build();
    }

    private AiTutorMessageDto toMessageDto(AiTutorMessage message) {
        return AiTutorMessageDto.builder()
            .id(message.getId().toString())
            .sessionId(message.getSession() != null ? message.getSession().getId().toString() : null)
            .senderId(message.getSender() != null ? message.getSender().getId().toString() : null)
            .senderRole(message.getSenderRole())
            .contentType(message.getContentType())
            .content(message.getContent())
            .transcript(message.getTranscript())
            .audioUrl(message.getAudioUrl())
            .contentPayload(message.getContentPayload())
            .ts(message.getTs())
            .build();
    }
}
