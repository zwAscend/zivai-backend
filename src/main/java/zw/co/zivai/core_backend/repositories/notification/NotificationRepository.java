package zw.co.zivai.core_backend.repositories.notification;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
    long countByRecipient_IdAndDeletedAtIsNullAndReadFalseAndPriorityIgnoreCase(UUID recipientId, String priority);

    @EntityGraph(attributePaths = {"recipient"})
    @Query("""
        select n
        from Notification n
        where n.deletedAt is null
          and (:recipientId is null or n.recipient.id = :recipientId)
          and (:read is null or n.read = :read)
          and (:type is null or lower(n.notifType) = lower(:type))
          and (:priority is null or lower(n.priority) = lower(:priority))
    """)
    Page<Notification> findFiltered(@Param("recipientId") UUID recipientId,
                                    @Param("read") Boolean read,
                                    @Param("type") String type,
                                    @Param("priority") String priority,
                                    Pageable pageable);
}
