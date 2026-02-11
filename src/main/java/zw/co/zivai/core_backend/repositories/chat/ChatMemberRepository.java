package zw.co.zivai.core_backend.repositories.chat;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import zw.co.zivai.core_backend.models.lms.ChatMember;
import zw.co.zivai.core_backend.models.lms.ChatMember.ChatMemberId;

public interface ChatMemberRepository extends JpaRepository<ChatMember, ChatMemberId> {
    List<ChatMember> findByUser_Id(UUID userId);
    List<ChatMember> findByChat_Id(UUID chatId);
}
