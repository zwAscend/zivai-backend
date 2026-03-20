package zw.co.zivai.core_backend.common.services.ai;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.RequiredArgsConstructor;
import zw.co.zivai.core_backend.common.configs.AiServiceProperties;
import zw.co.zivai.core_backend.common.dtos.ai.AiTutorMessageDto;
import zw.co.zivai.core_backend.common.dtos.ai.AiTutorSessionDto;
import zw.co.zivai.core_backend.common.dtos.ai.CreateAiTutorMessageRequest;
import zw.co.zivai.core_backend.common.dtos.ai.CreateAiTutorSessionRequest;
import zw.co.zivai.core_backend.common.exceptions.BadRequestException;
import zw.co.zivai.core_backend.common.exceptions.NotFoundException;
import zw.co.zivai.core_backend.common.models.lms.ai.AiTutorMessage;
import zw.co.zivai.core_backend.common.models.lms.ai.AiTutorSession;
import zw.co.zivai.core_backend.common.models.lms.subjects.Subject;
import zw.co.zivai.core_backend.common.models.lms.users.User;
import zw.co.zivai.core_backend.common.repositories.ai.AiTutorMessageRepository;
import zw.co.zivai.core_backend.common.repositories.ai.AiTutorSessionRepository;
import zw.co.zivai.core_backend.common.repositories.subject.SubjectRepository;
import zw.co.zivai.core_backend.common.repositories.user.UserRepository;

@Service
@RequiredArgsConstructor
public class AiTutorService {
    private final AiTutorSessionRepository sessionRepository;
    private final AiTutorMessageRepository messageRepository;
    private final UserRepository userRepository;
    private final SubjectRepository subjectRepository;
    private final AiServiceProperties aiServiceProperties;
    private final ObjectMapper objectMapper;

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
            messageRepository.save(buildTutorReply(session, request));
        }

        return toMessageDto(saved);
    }

    private AiTutorMessage buildTutorReply(AiTutorSession session, CreateAiTutorMessageRequest request) {
        String replyText;
        try {
            replyText = requestTutorReply(session, request);
        } catch (Exception ex) {
            replyText = buildFallbackReplyText(session, request);
        }

        AiTutorMessage reply = new AiTutorMessage();
        reply.setSession(session);
        reply.setSenderRole("tutor");
        reply.setContentType("text");
        reply.setContent(replyText);
        reply.setTs(Instant.now());
        session.setLastMessageAt(reply.getTs());
        sessionRepository.save(session);
        return reply;
    }

    private String requestTutorReply(AiTutorSession session, CreateAiTutorMessageRequest request)
        throws IOException, InterruptedException {
        String baseUrl = normalizeBaseUrl(aiServiceProperties.getBaseUrl());
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("studentId", session.getStudent() != null && session.getStudent().getId() != null
            ? session.getStudent().getId().toString()
            : null);
        payload.put("subjectId", session.getSubject() != null && session.getSubject().getId() != null
            ? session.getSubject().getId().toString()
            : null);
        payload.put("subjectName", session.getSubject() != null ? session.getSubject().getName() : null);
        payload.put("coachMode", extractPayloadText(request.getContentPayload(), "coachingMode", "socratic"));
        payload.put("latestMessage", resolveMessageText(request));
        payload.put("taskGoal", extractPayloadText(request.getContentPayload(), "taskGoal", null));
        payload.put("reasoningCanvas", extractPayloadText(request.getContentPayload(), "reasoningCanvas", null));
        payload.put("planTitle", extractPayloadText(request.getContentPayload(), "planTitle", null));
        payload.put("planStepTitle", extractPayloadText(request.getContentPayload(), "selectedPlanStep", null));

        ArrayNode messagesNode = payload.putArray("messages");
        List<AiTutorMessage> history = messageRepository.findBySession_IdAndDeletedAtIsNullOrderByTsAsc(session.getId());
        int startIndex = Math.max(0, history.size() - 8);
        for (AiTutorMessage historyMessage : history.subList(startIndex, history.size())) {
            String text = resolveMessageText(historyMessage);
            if (text == null || text.isBlank()) {
                continue;
            }
            ObjectNode messageNode = messagesNode.addObject();
            messageNode.put("role", "student".equalsIgnoreCase(historyMessage.getSenderRole()) ? "student" : "assistant");
            messageNode.put("text", text);
        }

        HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofMillis(Math.max(1000, aiServiceProperties.getConnectTimeoutMs())))
            .build();
        HttpRequest httpRequest = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/api/v1/agents/student/tutor"))
            .timeout(Duration.ofMillis(Math.max(3000, aiServiceProperties.getReadTimeoutMs())))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
            .build();

        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("AI tutor request failed with status " + response.statusCode() + ": " + response.body());
        }

        JsonNode responseBody = objectMapper.readTree(response.body());
        String reply = extractPayloadText(responseBody, "reply", null);
        if (reply == null || reply.isBlank()) {
            throw new IOException("AI tutor response did not include a reply");
        }

        String nextAction = extractPayloadText(responseBody, "suggestedNextAction", null);
        String followUpQuestion = extractPayloadText(responseBody, "followUpQuestion", null);
        StringBuilder combined = new StringBuilder(reply.trim());
        if (nextAction != null && !nextAction.isBlank()) {
            combined.append("\n\nNext step: ").append(nextAction.trim());
        }
        if (followUpQuestion != null && !followUpQuestion.isBlank()) {
            combined.append("\n\nThink about this: ").append(followUpQuestion.trim());
        }
        return combined.toString();
    }

    private String buildFallbackReplyText(AiTutorSession session, CreateAiTutorMessageRequest request) {
        String subjectName = session.getSubject() != null && session.getSubject().getName() != null
            ? session.getSubject().getName()
            : "this subject";
        String stepTitle = extractPayloadText(request.getContentPayload(), "selectedPlanStep", null);
        String focus = (stepTitle != null && !stepTitle.isBlank()) ? stepTitle : subjectName;
        return "Focus on " + focus + " first. Explain the main idea in your own words, test it on one example, "
            + "then tell me which step still feels uncertain.";
    }

    private String resolveMessageText(CreateAiTutorMessageRequest request) {
        String content = request.getContent();
        if (content != null && !content.isBlank()) {
            return content.trim();
        }
        String transcript = request.getTranscript();
        if (transcript != null && !transcript.isBlank()) {
            return transcript.trim();
        }
        return "";
    }

    private String resolveMessageText(AiTutorMessage message) {
        if (message.getContent() != null && !message.getContent().isBlank()) {
            return message.getContent().trim();
        }
        if (message.getTranscript() != null && !message.getTranscript().isBlank()) {
            return message.getTranscript().trim();
        }
        return "";
    }

    private String normalizeBaseUrl(String baseUrl) {
        String resolved = baseUrl == null ? "" : baseUrl.trim();
        if (resolved.isEmpty()) {
            return "http://localhost:8000";
        }
        return resolved.endsWith("/") ? resolved.substring(0, resolved.length() - 1) : resolved;
    }

    private String extractPayloadText(JsonNode node, String fieldName, String fallback) {
        if (node == null || fieldName == null || fieldName.isBlank()) {
            return fallback;
        }
        JsonNode fieldNode = node.get(fieldName);
        if (fieldNode == null || fieldNode.isNull()) {
            return fallback;
        }
        String value = fieldNode.asText(null);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
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
