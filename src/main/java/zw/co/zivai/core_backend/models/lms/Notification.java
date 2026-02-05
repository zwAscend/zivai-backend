package zw.co.zivai.core_backend.models.lms;

import java.time.Instant;

import com.fasterxml.jackson.databind.JsonNode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.Getter;
import lombok.Setter;
import zw.co.zivai.core_backend.models.base.BaseEntity;

@Getter
@Setter
@Entity
@Table(name = "notifications", schema = "lms")
public class Notification extends BaseEntity {
    @ManyToOne(optional = false)
    @JoinColumn(name = "school_id")
    private School school;

    @ManyToOne(optional = false)
    @JoinColumn(name = "recipient_id")
    private User recipient;

    @Column(name = "notif_type", nullable = false)
    private String notifType;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String message;

    @Column(name = "data")
    @JdbcTypeCode(SqlTypes.JSON)
    private JsonNode data;

    @Column(name = "is_read", nullable = false)
    private boolean read = false;

    @Column(name = "read_at")
    private Instant readAt;

    @Column(name = "priority", nullable = false)
    private String priority = "medium";

    @Column(name = "expires_at")
    private Instant expiresAt;
}
