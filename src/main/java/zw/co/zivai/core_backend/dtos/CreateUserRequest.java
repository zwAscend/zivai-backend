package zw.co.zivai.core_backend.dtos;

import java.util.Set;

import lombok.Data;

@Data
public class CreateUserRequest {
    private String externalId;
    private String email;
    private String phoneNumber;
    private String firstName;
    private String lastName;
    private String username;
    private boolean active = true;
    private Set<String> roleCodes;
}
