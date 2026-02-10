package zw.co.zivai.core_backend.models.lms;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import zw.co.zivai.core_backend.models.base.BaseEntity;

@Getter
@Setter
@Entity
@Table(name = "ai_tutor_sessions", schema = "lms")
public class AiTutorSession extends BaseEntity {
    @ManyToOne(optional = false)
    @JoinColumn(name = "student_id")
    private User student;

    @ManyToOne(optional = false)
    @JoinColumn(name = "subject_id")
    private Subject subject;

    @Column(name = "status_code", nullable = false)
    private String statusCode = "active";

    @Column(name = "last_message_at")
    private Instant lastMessageAt;

    @ManyToOne
    @JoinColumn(name = "created_by")
    private User createdBy;
}
