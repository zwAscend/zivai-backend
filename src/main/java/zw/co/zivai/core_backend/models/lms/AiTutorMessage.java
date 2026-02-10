package zw.co.zivai.core_backend.models.lms;

import java.time.Instant;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import zw.co.zivai.core_backend.models.base.BaseEntity;

@Getter
@Setter
@Entity
@Table(name = "ai_tutor_messages", schema = "lms")
public class AiTutorMessage extends BaseEntity {
    @ManyToOne(optional = false)
    @JoinColumn(name = "session_id")
    private AiTutorSession session;

    @ManyToOne
    @JoinColumn(name = "sender_id")
    private User sender;

    @Column(name = "sender_role", nullable = false)
    private String senderRole = "student";

    @Column(name = "content_type", nullable = false)
    private String contentType = "text";

    @Column(name = "content")
    private String content;

    @Column(name = "transcript")
    private String transcript;

    @Column(name = "audio_url")
    private String audioUrl;

    @Column(name = "content_payload")
    @JdbcTypeCode(SqlTypes.JSON)
    private JsonNode contentPayload;

    @Column(name = "ts", nullable = false)
    private Instant ts = Instant.now();
}
