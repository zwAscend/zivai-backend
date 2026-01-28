package zw.co.zivai.core_backend.services;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import zw.co.zivai.core_backend.dtos.CreateNotificationRequest;
import zw.co.zivai.core_backend.exceptions.NotFoundException;
import zw.co.zivai.core_backend.models.lms.Notification;
import zw.co.zivai.core_backend.models.lms.School;
import zw.co.zivai.core_backend.models.lms.User;
import zw.co.zivai.core_backend.repositories.NotificationRepository;
import zw.co.zivai.core_backend.repositories.SchoolRepository;
import zw.co.zivai.core_backend.repositories.UserRepository;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final SchoolRepository schoolRepository;
    private final UserRepository userRepository;

    public Notification create(CreateNotificationRequest request) {
        School school = schoolRepository.findById(request.getSchoolId())
            .orElseThrow(() -> new NotFoundException("School not found: " + request.getSchoolId()));
        User recipient = userRepository.findById(request.getRecipientId())
            .orElseThrow(() -> new NotFoundException("User not found: " + request.getRecipientId()));

        Notification notification = new Notification();
        notification.setSchool(school);
        notification.setRecipient(recipient);
        notification.setNotifType(request.getNotifType());
        notification.setTitle(request.getTitle());
        notification.setMessage(request.getMessage());
        notification.setData(request.getData());
        notification.setRead(request.isRead());
        notification.setReadAt(request.getReadAt());
        notification.setPriority(request.getPriority());
        notification.setExpiresAt(request.getExpiresAt());

        return notificationRepository.save(notification);
    }

    public List<Notification> list() {
        return notificationRepository.findAll();
    }

    public Notification get(UUID id) {
        return notificationRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Notification not found: " + id));
    }


    public long getUnreadCount() {
        return notificationRepository.countByReadFalse();
    }

    public Notification markAsRead(UUID id) {
        Notification notification = get(id);
        if (!notification.isRead()) {
            notification.setRead(true);
            notification.setReadAt(Instant.now());
        }
        return notificationRepository.save(notification);
    }

    public void markAllAsRead() {
        List<Notification> notifications = notificationRepository.findAll();
        for (Notification notification : notifications) {
            if (!notification.isRead()) {
                notification.setRead(true);
                notification.setReadAt(Instant.now());
            }
        }
        notificationRepository.saveAll(notifications);
    }

    public void delete(UUID id) {
        Notification notification = get(id);
        notificationRepository.delete(notification);
    }
}
