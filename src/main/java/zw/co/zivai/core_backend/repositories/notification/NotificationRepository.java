package zw.co.zivai.core_backend.repositories.notification;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import zw.co.zivai.core_backend.models.lms.Notification;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    long countByReadFalse();
}

