package zw.co.zivai.core_backend.common.repositories.notification;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import zw.co.zivai.core_backend.common.models.lms.chat.Notification;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    @EntityGraph(attributePaths = {"recipient", "school"})
    List<Notification> findByDeletedAtIsNullOrderByCreatedAtDesc();
    @EntityGraph(attributePaths = {"recipient", "school"})
    List<Notification> findByRecipient_IdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID recipientId);
    List<Notification> findByDeletedAtIsNullAndReadFalse();
    List<Notification> findByRecipient_IdAndDeletedAtIsNullAndReadFalse(UUID recipientId);
    @EntityGraph(attributePaths = {"recipient", "school"})
    java.util.Optional<Notification> findByIdAndDeletedAtIsNull(UUID id);
    long countByDeletedAtIsNullAndReadFalse();
    long countByRecipient_IdAndDeletedAtIsNullAndReadFalse(UUID recipientId);
    long countByRecipient_IdAndDeletedAtIsNullAndReadFalseAndPriorityIgnoreCase(UUID recipientId, String priority);

    @EntityGraph(attributePaths = {"recipient", "school"})
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

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update Notification n
        set n.read = true,
            n.readAt = :readAt
        where n.deletedAt is null
          and n.read = false
          and (:recipientId is null or n.recipient.id = :recipientId)
    """)
    int markAllAsRead(@Param("recipientId") UUID recipientId, @Param("readAt") java.time.Instant readAt);
}
