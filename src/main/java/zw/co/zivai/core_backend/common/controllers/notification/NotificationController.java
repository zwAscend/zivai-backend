package zw.co.zivai.core_backend.common.controllers.notification;

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
import zw.co.zivai.core_backend.common.dtos.notification.CreateNotificationRequest;
import zw.co.zivai.core_backend.common.dtos.notification.NotificationResponse;
import zw.co.zivai.core_backend.common.services.notification.NotificationService;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService notificationService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public NotificationResponse create(@RequestBody CreateNotificationRequest request) {
        return notificationService.toResponse(notificationService.create(request));
    }

    @GetMapping
    public List<NotificationResponse> list(@RequestParam(required = false) UUID recipientId,
                                           @RequestParam(required = false) Boolean read,
                                           @RequestParam(required = false, name = "type") String type,
                                           @RequestParam(required = false) String priority,
                                           @RequestParam(required = false) Integer page,
                                           @RequestParam(required = false) Integer size) {
        return notificationService.listResponses(recipientId, read, type, priority, page, size);
    }

    @GetMapping("/unread-count")
    public long unreadCount(@RequestParam(required = false) UUID recipientId) {
        return notificationService.getUnreadCount(recipientId);
    }

    @GetMapping("/{id}")
    public NotificationResponse get(@PathVariable UUID id) {
        return notificationService.getResponse(id);
    }

    @PutMapping("/{id}/read")
    public NotificationResponse markAsRead(@PathVariable UUID id,
                                           @RequestParam(required = false) UUID recipientId) {
        return notificationService.markAsReadResponse(id, recipientId);
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
