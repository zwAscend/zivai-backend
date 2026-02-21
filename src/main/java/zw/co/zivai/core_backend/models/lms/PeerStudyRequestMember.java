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
@Table(name = "peer_study_request_members", schema = "lms")
public class PeerStudyRequestMember extends BaseEntity {
    @ManyToOne(optional = false)
    @JoinColumn(name = "request_id")
    private PeerStudyRequest request;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "role_code", nullable = false)
    private String roleCode = "member";

    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt = Instant.now();
}
