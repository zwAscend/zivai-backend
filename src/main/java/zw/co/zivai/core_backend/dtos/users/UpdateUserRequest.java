package zw.co.zivai.core_backend.dtos.users;

import java.util.Set;

import lombok.Data;

@Data
public class UpdateUserRequest {
    private String externalId;
    private String email;
    private String phoneNumber;
    private String firstName;
    private String lastName;
    private String username;
    private String password;
    private Boolean active;
    private Set<String> roleCodes;
}
