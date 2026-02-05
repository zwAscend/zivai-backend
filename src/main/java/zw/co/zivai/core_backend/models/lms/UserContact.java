package zw.co.zivai.core_backend.models.lms;

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
@Table(name = "user_contacts", schema = "lms")
public class UserContact extends BaseEntity {
    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "contact_channel_code", nullable = false)
    private String contactChannelCode;

    @Column(nullable = false)
    private String value;

    @Column(name = "is_primary", nullable = false)
    private boolean primary = false;

    private String notes;
}
