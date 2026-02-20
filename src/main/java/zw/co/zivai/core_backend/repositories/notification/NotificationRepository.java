package zw.co.zivai.core_backend.repositories.notification;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import zw.co.zivai.core_backend.models.lms.Notification;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    @EntityGraph(attributePaths = {"recipient"})
    List<Notification> findByDeletedAtIsNullOrderByCreatedAtDesc();
    @EntityGraph(attributePaths = {"recipient"})
    List<Notification> findByRecipient_IdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID recipientId);
    List<Notification> findByDeletedAtIsNullAndReadFalse();
    List<Notification> findByRecipient_IdAndDeletedAtIsNullAndReadFalse(UUID recipientId);
    long countByDeletedAtIsNullAndReadFalse();
    long countByRecipient_IdAndDeletedAtIsNullAndReadFalse(UUID recipientId);
}
