package zw.co.zivai.core_backend.models.lms;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "chat_members", schema = "lms")
public class ChatMember {
    @EmbeddedId
    private ChatMemberId id = new ChatMemberId();

    @ManyToOne(optional = false)
    @MapsId("chatId")
    @JoinColumn(name = "chat_id")
    private Chat chat;

    @ManyToOne(optional = false)
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private String role = "member";

    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt = Instant.now();

    @Getter
    @Setter
    @Embeddable
    @EqualsAndHashCode
    public static class ChatMemberId implements Serializable {
        @Column(name = "chat_id")
        private UUID chatId;

        @Column(name = "user_id")
        private UUID userId;
    }
}
