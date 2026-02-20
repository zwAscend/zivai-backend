package zw.co.zivai.core_backend.controllers.notification;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import zw.co.zivai.core_backend.dtos.notification.CreateNotificationRequest;
import zw.co.zivai.core_backend.models.lms.Notification;
import zw.co.zivai.core_backend.services.notification.NotificationService;

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
    public List<Notification> list(@RequestParam(required = false) UUID recipientId) {
        return notificationService.list(recipientId);
    }

    @GetMapping("/unread-count")
    public long unreadCount(@RequestParam(required = false) UUID recipientId) {
        return notificationService.getUnreadCount(recipientId);
    }

    @GetMapping("/{id}")
    public Notification get(@PathVariable UUID id) {
        return notificationService.get(id);
    }

    @PutMapping("/{id}/read")
    public Notification markAsRead(@PathVariable UUID id,
                                   @RequestParam(required = false) UUID recipientId) {
        return notificationService.markAsRead(id, recipientId);
    }

    @PutMapping("/read-all")
    public Map<String, String> markAllAsRead(@RequestParam(required = false) UUID recipientId) {
        notificationService.markAllAsRead(recipientId);
        return Map.of("message", "All notifications marked as read");
    }

    @DeleteMapping("/{id}")
    public Map<String, String> delete(@PathVariable UUID id,
                                      @RequestParam(required = false) UUID recipientId) {
        notificationService.delete(id, recipientId);
        return Map.of("message", "Notification deleted");
    }

}
