package zw.co.zivai.core_backend.controllers;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import zw.co.zivai.core_backend.dtos.CreateNotificationRequest;
import zw.co.zivai.core_backend.models.lms.Notification;
import zw.co.zivai.core_backend.services.NotificationService;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService notificationService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Notification create(@RequestBody CreateNotificationRequest request) {
        return notificationService.create(request);
    }

    @GetMapping
    public List<Notification> list() {
        return notificationService.list();
    }

    @GetMapping("/unread-count")
    public long unreadCount() {
        return notificationService.getUnreadCount();
    }

    @GetMapping("/{id}")
    public Notification get(@PathVariable UUID id) {
        return notificationService.get(id);
    }

    @PutMapping("/{id}/read")
    public Notification markAsRead(@PathVariable UUID id) {
        return notificationService.markAsRead(id);
    }

    @PutMapping("/read-all")
    public Map<String, String> markAllAsRead() {
        notificationService.markAllAsRead();
        return Map.of("message", "All notifications marked as read");
    }

    @DeleteMapping("/{id}")
    public Map<String, String> delete(@PathVariable UUID id) {
        notificationService.delete(id);
        return Map.of("message", "Notification deleted");
    }

}
