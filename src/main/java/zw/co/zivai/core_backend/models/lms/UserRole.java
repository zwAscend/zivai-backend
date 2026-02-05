package zw.co.zivai.core_backend.models.lms;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import zw.co.zivai.core_backend.models.lookups.Role;

@Getter
@Setter
@Entity
@Table(name = "user_roles", schema = "lms")
public class UserRole {
    @EmbeddedId
    private UserRoleId id = new UserRoleId();

    @ManyToOne(optional = false)
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(optional = false)
    @MapsId("roleId")
    @JoinColumn(name = "role_id")
    private Role role;
}
