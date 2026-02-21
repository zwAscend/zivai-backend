package zw.co.zivai.core_backend.services.notification;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import zw.co.zivai.core_backend.dtos.notification.CreateNotificationRequest;
import zw.co.zivai.core_backend.exceptions.NotFoundException;
import zw.co.zivai.core_backend.models.lms.Notification;
import zw.co.zivai.core_backend.models.lms.School;
import zw.co.zivai.core_backend.models.lms.User;
import zw.co.zivai.core_backend.repositories.notification.NotificationRepository;
import zw.co.zivai.core_backend.repositories.school.SchoolRepository;
import zw.co.zivai.core_backend.repositories.user.UserRepository;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final SchoolRepository schoolRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public Notification create(CreateNotificationRequest request) {
        School school = schoolRepository.findByIdAndDeletedAtIsNull(request.getSchoolId())
            .orElseThrow(() -> new NotFoundException("School not found: " + request.getSchoolId()));
        User recipient = userRepository.findByIdAndDeletedAtIsNull(request.getRecipientId())
            .orElseThrow(() -> new NotFoundException("User not found: " + request.getRecipientId()));

        Notification notification = new Notification();
        notification.setSchool(school);
        notification.setRecipient(recipient);
        notification.setNotifType(request.getNotifType());
        notification.setTitle(request.getTitle());
        notification.setMessage(request.getMessage());
        notification.setData(request.getData() == null ? null : objectMapper.valueToTree(request.getData()));
        notification.setRead(request.isRead());
        notification.setReadAt(request.getReadAt());
        notification.setPriority(request.getPriority());
        notification.setExpiresAt(request.getExpiresAt());

        return notificationRepository.save(notification);
    }

    public List<Notification> createBulk(UUID schoolId,
                                         Collection<UUID> recipientIds,
                                         String notifType,
                                         String title,
                                         String message,
                                         Object data,
                                         String priority) {
        if (schoolId == null || recipientIds == null || recipientIds.isEmpty()) {
            return List.of();
        }

        School school = schoolRepository.findByIdAndDeletedAtIsNull(schoolId)
            .orElseThrow(() -> new NotFoundException("School not found: " + schoolId));

        List<UUID> uniqueRecipients = new ArrayList<>(new LinkedHashSet<>(recipientIds));
        List<User> recipients = userRepository.findByIdInAndDeletedAtIsNull(uniqueRecipients);
        if (recipients.isEmpty()) {
            return List.of();
        }

        Map<UUID, User> userById = new HashMap<>();
        for (User recipient : recipients) {
            userById.put(recipient.getId(), recipient);
        }

        List<Notification> notifications = new ArrayList<>();
        for (UUID recipientId : uniqueRecipients) {
            User recipient = userById.get(recipientId);
            if (recipient == null) {
                continue;
            }
            Notification notification = new Notification();
            notification.setSchool(school);
            notification.setRecipient(recipient);
            notification.setNotifType(notifType);
            notification.setTitle(title);
            notification.setMessage(message);
            notification.setData(data == null ? null : objectMapper.valueToTree(data));
            notification.setRead(false);
            notification.setPriority(priority == null || priority.isBlank() ? "medium" : priority);
            notifications.add(notification);
        }

        if (notifications.isEmpty()) {
            return List.of();
        }
        return notificationRepository.saveAll(notifications);
    }

    public List<Notification> list() {
        return list(null);
    }

    public List<Notification> list(UUID recipientId) {
        return list(recipientId, null, null, null, null, null);
    }

    public List<Notification> list(UUID recipientId,
                                   Boolean read,
                                   String type,
                                   String priority,
                                   Integer page,
                                   Integer size) {
        boolean noAdvancedFilters = read == null
            && (type == null || type.isBlank())
            && (priority == null || priority.isBlank())
            && page == null
            && size == null;
        if (noAdvancedFilters) {
            if (recipientId == null) {
                return notificationRepository.findByDeletedAtIsNullOrderByCreatedAtDesc();
            }
            return notificationRepository.findByRecipient_IdAndDeletedAtIsNullOrderByCreatedAtDesc(recipientId);
        }

        int safePage = page == null ? 0 : Math.max(0, page);
        int safeSize = size == null ? 50 : Math.max(1, Math.min(size, 200));
        return notificationRepository.findFiltered(
            recipientId,
            read,
            normalizeNullable(type),
            normalizeNullable(priority),
            PageRequest.of(safePage, safeSize, Sort.by(Sort.Order.desc("createdAt")))
        ).getContent();
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }

    public Notification get(UUID id) {
        Notification notification = notificationRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Notification not found: " + id));
        if (notification.getDeletedAt() != null) {
            throw new NotFoundException("Notification not found: " + id);
        }
        return notification;
    }


    public long getUnreadCount() {
        return getUnreadCount(null);
    }

    public long getUnreadCount(UUID recipientId) {
        if (recipientId == null) {
            return notificationRepository.countByDeletedAtIsNullAndReadFalse();
        }
        return notificationRepository.countByRecipient_IdAndDeletedAtIsNullAndReadFalse(recipientId);
    }

    public Notification markAsRead(UUID id) {
        return markAsRead(id, null);
    }

    public Notification markAsRead(UUID id, UUID recipientId) {
        Notification notification = get(id);
        ensureRecipient(notification, recipientId);
        if (!notification.isRead()) {
            notification.setRead(true);
            notification.setReadAt(Instant.now());
        }
        return notificationRepository.save(notification);
    }

    public void markAllAsRead() {
        markAllAsRead(null);
    }

    public void markAllAsRead(UUID recipientId) {
        List<Notification> notifications = recipientId == null
            ? notificationRepository.findByDeletedAtIsNullAndReadFalse()
            : notificationRepository.findByRecipient_IdAndDeletedAtIsNullAndReadFalse(recipientId);
        for (Notification notification : notifications) {
            notification.setRead(true);
            notification.setReadAt(Instant.now());
        }
        notificationRepository.saveAll(notifications);
    }

    public void delete(UUID id) {
        delete(id, null);
    }

    public void delete(UUID id, UUID recipientId) {
        Notification notification = get(id);
        ensureRecipient(notification, recipientId);
        notification.setDeletedAt(Instant.now());
        notificationRepository.save(notification);
    }

    private void ensureRecipient(Notification notification, UUID recipientId) {
        if (recipientId == null) {
            return;
        }
        UUID actualRecipientId = notification.getRecipient() != null ? notification.getRecipient().getId() : null;
        if (!Objects.equals(actualRecipientId, recipientId)) {
            throw new NotFoundException("Notification not found for recipient: " + recipientId);
        }
    }
}
