package zw.co.zivai.core_backend.services.chat;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import zw.co.zivai.core_backend.dtos.chat.ChatMessageDto;
import zw.co.zivai.core_backend.dtos.chat.UnreadChatCountDto;
import zw.co.zivai.core_backend.exceptions.NotFoundException;
import zw.co.zivai.core_backend.models.lms.Chat;
import zw.co.zivai.core_backend.models.lms.ChatMember;
import zw.co.zivai.core_backend.models.lms.ClassEntity;
import zw.co.zivai.core_backend.models.lms.ClassSubject;
import zw.co.zivai.core_backend.models.lms.Enrolment;
import zw.co.zivai.core_backend.models.lms.Message;
import zw.co.zivai.core_backend.models.lms.School;
import zw.co.zivai.core_backend.models.lms.User;
import zw.co.zivai.core_backend.repositories.chat.ChatMemberRepository;
import zw.co.zivai.core_backend.repositories.chat.ChatRepository;
import zw.co.zivai.core_backend.repositories.classroom.ClassSubjectRepository;
import zw.co.zivai.core_backend.repositories.classroom.EnrolmentRepository;
import zw.co.zivai.core_backend.repositories.chat.MessageRepository;
import zw.co.zivai.core_backend.repositories.school.SchoolRepository;
import zw.co.zivai.core_backend.repositories.user.UserRepository;
import zw.co.zivai.core_backend.websockets.ChatSocketRegistry;

@Service
@RequiredArgsConstructor
public class ChatMessageService {
    private final ChatRepository chatRepository;
    private final ChatMemberRepository chatMemberRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final SchoolRepository schoolRepository;
    private final EnrolmentRepository enrolmentRepository;
    private final ClassSubjectRepository classSubjectRepository;
    private final ChatSocketRegistry chatSocketRegistry;

    public List<ChatMessageDto> getMessagesForStudent(UUID studentId) {
        User student = userRepository.findByIdAndDeletedAtIsNull(studentId)
            .orElseThrow(() -> new NotFoundException("Student not found: " + studentId));

        Chat chat = getOrCreateChatForStudent(student);
        return messageRepository.findByChatIdOrderByTsAsc(chat.getId()).stream()
            .map(this::toDto)
            .toList();
    }

    public ChatMessageDto sendMessage(UUID studentId, UUID senderId, String content) {
        User student = userRepository.findByIdAndDeletedAtIsNull(studentId)
            .orElseThrow(() -> new NotFoundException("Student not found: " + studentId));

        Chat chat = getOrCreateChatForStudent(student);
        School school = chat.getSchool();
        User sender = student;
        if (senderId != null) {
            sender = userRepository.findByIdAndDeletedAtIsNull(senderId)
                .orElseThrow(() -> new NotFoundException("Sender not found: " + senderId));
        }

        Message message = new Message();
        message.setChat(chat);
        message.setSchool(school);
        message.setSender(sender);
        message.setContent(content == null ? "" : content.trim());
        message.setTs(Instant.now());
        message.setRead(false);

        ChatMessageDto dto = toDto(messageRepository.save(message));
        chatSocketRegistry.broadcast(studentId.toString(), dto);
        return dto;
    }

    public void markChatRead(UUID studentId) {
        User student = userRepository.findByIdAndDeletedAtIsNull(studentId)
            .orElseThrow(() -> new NotFoundException("Student not found: " + studentId));
        Chat chat = getOrCreateChatForStudent(student);
        List<Message> unread = messageRepository.findByChatIdAndReadFalse(chat.getId());
        if (unread.isEmpty()) {
            return;
        }
        for (Message message : unread) {
            message.setRead(true);
        }
        messageRepository.saveAll(unread);
    }

    public List<UnreadChatCountDto> getUnreadCounts() {
        List<User> students = userRepository.findByRoles_CodeAndDeletedAtIsNull("student");
        return students.stream()
            .map(student -> {
                Chat chat = getExistingChatForStudent(student).orElse(null);
                long unread = 0;
                String lastMessage = null;
                Instant lastMessageTime = null;
                if (chat != null) {
                    unread = messageRepository.findByChatIdAndReadFalse(chat.getId()).size();
                    Optional<Message> latest = messageRepository.findFirstByChatIdOrderByTsDesc(chat.getId());
                    if (latest.isPresent()) {
                        lastMessage = latest.get().getContent();
                        lastMessageTime = latest.get().getTs();
                    }
                }
                return UnreadChatCountDto.builder()
                    .studentId(student.getId().toString())
                    .studentName(student.getFirstName() + " " + student.getLastName())
                    .unreadCount(unread)
                    .lastMessage(lastMessage)
                    .lastMessageTime(lastMessageTime)
                    .build();
            })
            .toList();
    }

    private Chat getOrCreateChatForStudent(User student) {
        return getExistingChatForStudent(student).orElseGet(() -> createChatForStudent(student));
    }

    private Optional<Chat> getExistingChatForStudent(User student) {
        List<ChatMember> memberships = chatMemberRepository.findByUser_Id(student.getId());
        if (memberships.isEmpty()) {
            return Optional.empty();
        }
        return memberships.stream()
            .map(ChatMember::getChat)
            .filter(chat -> chat != null && "direct".equalsIgnoreCase(chat.getChatType()))
            .findFirst();
    }

    private Chat createChatForStudent(User student) {
        School school = resolveSchool(student);
        User teacher = resolveTeacher(student);

        Chat chat = new Chat();
        chat.setSchool(school);
        chat.setChatType("direct");
        chat.setTitle("Student Chat");
        Chat savedChat = chatRepository.save(chat);

        ChatMember studentMember = new ChatMember();
        studentMember.setChat(savedChat);
        studentMember.setUser(student);
        studentMember.setRole("member");
        chatMemberRepository.save(studentMember);

        if (teacher != null) {
            ChatMember teacherMember = new ChatMember();
            teacherMember.setChat(savedChat);
            teacherMember.setUser(teacher);
            teacherMember.setRole("admin");
            chatMemberRepository.save(teacherMember);
        }

        return savedChat;
    }

    private School resolveSchool(User student) {
        List<Enrolment> enrolments = enrolmentRepository.findByStudent_Id(student.getId());
        for (Enrolment enrolment : enrolments) {
            ClassEntity classEntity = enrolment.getClassEntity();
            if (classEntity != null && classEntity.getSchool() != null) {
                return classEntity.getSchool();
            }
        }
        return schoolRepository.findAll().stream()
            .findFirst()
            .orElseThrow(() -> new NotFoundException("No school found"));
    }

    private User resolveTeacher(User student) {
        List<Enrolment> enrolments = enrolmentRepository.findByStudent_Id(student.getId());
        for (Enrolment enrolment : enrolments) {
            ClassEntity classEntity = enrolment.getClassEntity();
            if (classEntity == null) {
                continue;
            }
            if (classEntity.getHomeroomTeacher() != null) {
                return classEntity.getHomeroomTeacher();
            }
            List<ClassSubject> classSubjects = classSubjectRepository
                .findByClassEntity_IdAndDeletedAtIsNull(classEntity.getId());
            Optional<User> subjectTeacher = classSubjects.stream()
                .map(ClassSubject::getTeacher)
                .filter(user -> user != null)
                .findFirst();
            if (subjectTeacher.isPresent()) {
                return subjectTeacher.get();
            }
        }

        List<User> teachers = userRepository.findByRoles_CodeAndDeletedAtIsNull("teacher");
        if (!teachers.isEmpty()) {
            return teachers.get(0);
        }
        return null;
    }

    private ChatMessageDto toDto(Message message) {
        User sender = message.getSender();
        boolean isTeacher = sender != null && sender.getRoles().stream()
            .anyMatch(role -> "teacher".equalsIgnoreCase(role.getCode()));

        return ChatMessageDto.builder()
            .id(message.getId().toString())
            .sender(ChatMessageDto.SenderDto.builder()
                .id(sender != null ? sender.getId().toString() : null)
                .firstName(sender != null ? sender.getFirstName() : null)
                .lastName(sender != null ? sender.getLastName() : null)
                .avatar(null)
                .build())
            .content(message.getContent())
            .timestamp(message.getTs())
            .read(message.isRead())
            .chatId(message.getChat() != null ? message.getChat().getId().toString() : null)
            .isTeacher(isTeacher)
            .createdAt(message.getCreatedAt())
            .updatedAt(message.getUpdatedAt())
            .build();
    }
}
