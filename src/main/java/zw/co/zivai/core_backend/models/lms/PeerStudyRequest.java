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
@Table(name = "peer_study_requests", schema = "lms")
public class PeerStudyRequest extends BaseEntity {
    @ManyToOne(optional = false)
    @JoinColumn(name = "subject_id")
    private Subject subject;

    @ManyToOne
    @JoinColumn(name = "topic_id")
    private Topic topic;

    @Column(name = "topic_title", nullable = false)
    private String topicTitle;

    @Column(name = "request_type", nullable = false)
    private String requestType = "need-help";

    @Column(nullable = false)
    private String note;

    @Column(name = "preferred_time")
    private Instant preferredTime;

    @Column(name = "status_code", nullable = false)
    private String statusCode = "open";

    @Column(name = "max_participants", nullable = false)
    private Integer maxParticipants = 6;

    @ManyToOne(optional = false)
    @JoinColumn(name = "created_by")
    private User createdBy;
}
