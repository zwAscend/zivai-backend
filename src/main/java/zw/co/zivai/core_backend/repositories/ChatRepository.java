package zw.co.zivai.core_backend.repositories;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import zw.co.zivai.core_backend.models.lms.Chat;

public interface ChatRepository extends JpaRepository<Chat, UUID> {
}
